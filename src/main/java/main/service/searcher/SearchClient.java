package main.service.searcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.dao.PageRepository;
import main.dao.SiteRepository;
import main.dao.WordRepository;
import main.model.MatchedPage;
import main.model.Page;
import main.model.Site;
import main.model.Site.Status;
import main.model.Word;
import main.utilities.Lexeme;
import main.utilities.LogUtil;
import main.utilities.WordCounter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SearchClient {

    private static final int MAX_OCCURRENCE_PERCENT = 90;
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Zа-яА-Я]+-?[a-zA-Zа-яА-Я]+");
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final WordRepository wordRepository;
    private final JdbcTemplate jdbcTemplate;

    public SearchClient(SiteRepository siteRepository,
                        PageRepository pageRepository,
                        WordRepository wordRepository,
                        JdbcTemplate jdbcTemplate) throws IOException {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.wordRepository = wordRepository;
        this.jdbcTemplate = jdbcTemplate;;
    }

    /**
     * Выделяет подходящее слово из текста
     *
     * @param text содеражимое страницы
     * @param word слово, для поиска
     * @return индекс начала и конца слова в тексте
     */
    private int[] getSnippet(String text, String word) {
        String token = word;
        if (text.contains(token)) {
            int i = text.indexOf(word);
            int j = i + word.length();
            return new int[]{i, j};
        }
        String content = text;
        if (token.contains("|")) {
            token = token.split("\\|")[0];
        }
        token = token.substring(0, token.length() / 2 + 1);
        while (true) {
            int i = content.indexOf(token);
            if (i == -1) {
                LogUtil.logger.error("Something went wrong while parsing snippet.");
                System.exit(2);
            }
            int j = content.substring(i).indexOf(" ") + i;
            String other = content.substring(i, j);
            Matcher matcher = WORD_PATTERN.matcher(other);
            matcher.find();
            other = matcher.group();
            Lexeme lexeme = new Lexeme(other, WordCounter.russianMorph);
            if (word.equals(lexeme.getWord())) {
                return new int[]{i, j};
            }
            content = content.substring(j);
        }

    }

    /**
     * Отображает наборы слов и страниц на результат поиска.
     *
     * @param words набор слов запроса
     * @param pages набор подходящих страниц
     * @return набор результатов поиска
     */
    private List<MatchedPage> mapToMatchedPages(List<Word> words, Set<Page> pages) {
        List<MatchedPage> resultList = pages.parallelStream()
                .map(x -> createMatchedPage(words, x))
                .collect(Collectors.toList());
        setRelativeRelevance(resultList);
        resultList.sort(Comparator.comparingDouble(MatchedPage::getRelevance).reversed());
        return resultList;
    }

    /**
     * Устанавливает относительную релевантность каждого результата поиска. Для этого требуется
     * определить наиболее релевантный результат
     *
     * @param resultList все результаты поиска
     */
    private void setRelativeRelevance(List<MatchedPage> resultList) {
        float maxRelevance = resultList.parallelStream()
                .map(MatchedPage::getRelevance)
                .max(Float::compareTo).orElseThrow();
        resultList.parallelStream()
                .forEach(x -> x.setRelevance(x.getRelevance() / maxRelevance));
    }

    /**
     * Создает результат поиска с абсолютной релевантностью
     *
     * @param words список слов запроса
     * @param page  совпадающая страница
     * @return результат поиска
     */
    private MatchedPage createMatchedPage(List<Word> words, Page page) {
        MatchedPage matchedPage = new MatchedPage();
        matchedPage.setSite(page.getSite().getUrl());
        matchedPage.setSiteName(page.getSite().getName());
        matchedPage.setUrl(page.getUrl());
        Element element = Jsoup.parse(page.getContent()).selectFirst("title");
        String title = null;
        if (Objects.nonNull(element)) {
            title = element.text();
        }
        matchedPage.setTitle(title);
        String content = Jsoup.parse(page.getContent()).text().toLowerCase();
        StringJoiner snippet = new StringJoiner("...", " ... ", "...");
        words.stream()
                .map(word -> {
                    String sql = "SELECT `rank` FROM _index WHERE page_id = %s AND lemma_id = %s"
                            .formatted(page.getId(), word.getId());
                    float rank = jdbcTemplate.queryForObject(sql, Float.class);
                    matchedPage.setRelevance(matchedPage.getRelevance() + rank);
                    return word.getName();
                })
                .forEach(x -> {
                    // выделение фрагментов
                    int[] wordIndices = getSnippet(content, x);
                    if (snippet.toString().contains(content.substring(wordIndices[0], wordIndices[1]))) {
                        return;
                    }
                    int snippetStart = Math.max(wordIndices[0] - 20, 0);
                    int snippetEnd = Math.min(wordIndices[1] + 20, content.length());
                    snippet.add(content.substring(snippetStart, snippetEnd));
                });
        matchedPage.setSnippet(snippet.toString());
        return matchedPage;
    }

    /**
     * Собирает страницы, подходящие по набору лемм
     *
     * @param words сущности Word
     * @return страницы, содержащие все леммы
     */
    private Set<Page> mapToPages(List<Word> words, Site site) {
        HashSet<Page> matchedSet = new HashSet<>(pageRepository.findBySite(site));
        for (Word word : words) {
            String sql = """
                    SELECT _page.id FROM _index
                    JOIN _page ON _page.id = _index.page_id
                    JOIN _lemma ON _lemma.id = _index.lemma_id
                    WHERE _lemma.lemma = "%s"
                    """.formatted(word.getName());
            List<Integer> list = jdbcTemplate.queryForList(sql, Integer.class);
            matchedSet.removeIf(y -> !list.contains(y.getId()));
        }
        return matchedSet;
    }

    /**
     * Получает совпадающие леммы из базы данных
     *
     * @param lemmas набор слов
     * @return список сущностей Word из таблицы _lemma
     */
    private List<Word> mapToWords(Set<String> lemmas, Site site) {
        List<Word> words = new ArrayList<>();
        long pagesCount = pageRepository.countBySiteId(site);
        for (String lemma : lemmas) {
            Optional<Word> optional = wordRepository.findByNameAndSite(lemma, site);
            if (optional.isEmpty()) {
                return Collections.emptyList();
            }
            Word word = optional.get();
            float percent = word.getFrequency() / (float) pagesCount * 100;
            if (pagesCount < 3 || percent < MAX_OCCURRENCE_PERCENT) {
                words.add(word);
            }
        }
        words.sort(Comparator.comparingInt(Word::getFrequency));
        return words;
    }

    public List<MatchedPage> search(String searchQuery, Site site) {
        Set<String> lemmas = WordCounter.getStats(searchQuery).keySet();
        List<Word> words = mapToWords(lemmas, site);
        if (words.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Page> pages = mapToPages(words, site);
        return mapToMatchedPages(words, pages);
    }

    public ResponseEntity<?> search(String searchQuery, String site, int offset, int limit) {
        List<Site> sites = siteRepository.findAll();
        if (site != null) {
            if (sites.stream().anyMatch(x -> x.getUrl().equals(site))) {
                sites.removeIf(x -> !x.getUrl().equals(site));
            } else {
                return ResponseEntity.status(400).body(Map.of("result", false,
                        "error", "Такой сайт не содержится в индексе."));
            }
        }
        if (!sites.stream().allMatch(x -> x.getStatus().equals(Status.INDEXED))) {
            return ResponseEntity.status(409).body(Map.of("result", false,
                    "error", "Не все сайты проиндексированы."));
        }
        List<MatchedPage> searchResults = new ArrayList<>();
        for (Site s : sites) {
            List<MatchedPage> siteMatches = search(searchQuery, s);
            searchResults.addAll(siteMatches);
        }
        int count = searchResults.size();
        if (count > offset) {
            searchResults = searchResults.subList(offset, count);
        }
        searchResults = searchResults.stream()
                .limit(limit)
                .toList();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("result", true);
        root.put("count", count);
        ArrayNode arrayNode = mapper.valueToTree(searchResults);
        root.putArray("data").addAll(arrayNode);
        return ResponseEntity.status(200).body(root);
    }
}
