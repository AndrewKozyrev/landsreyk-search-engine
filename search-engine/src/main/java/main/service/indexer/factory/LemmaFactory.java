package main.service.indexer.factory;

import main.model.Page;
import main.model.Word;
import main.service.indexer.FieldManager;
import main.utilities.LogUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LemmaFactory {

    private static final Map<String, HashSet<Word>> cachedWords = new ConcurrentHashMap<>();
    private static final FieldManager fieldManager = new FieldManager();

    public static Collection<Word> create(Page page) {
        LogUtil.logger.info("LemmaFactory::create -> start page [%s]".formatted(page));
        Map<String, Float> stats = fieldManager.parse(page);
        Collection<Word> words = create(stats, page);
        LogUtil.logger.info("LemmaFactory::create -> end page [%s]".formatted(page));
        return words;
    }

    private synchronized static Collection<Word> create(Map<String, Float> stats, Page page) {
        return stats.keySet().stream().map(lemma -> {
            Word word = getFromCache(lemma, page);
            if (word == null) {
                word = new Word();
                word.setName(lemma);
                word.setSite(page.getSite());
                cachedWords.get(lemma).add(word);
            }
            word.setFrequency(word.getFrequency() + 1);
            word.setRank(stats.get(lemma));
            return word;
        }).collect(Collectors.toList());
    }

    public static Collection<Word> update(Collection<Word> toUpdate, Page page) {
        Collection<Word> newWords = create(page);
        Collection<Word> result = new ArrayList<>();
        Map<String, Word> oldWords = toUpdate.stream().collect(Collectors.toMap(Word::getName, x -> x));
        for (Word newWord : newWords) {
            Word word = oldWords.getOrDefault(newWord.getName(), newWord);
            word.setRank(newWord.getRank());
            word.setFrequency(newWord.getFrequency());
            result.add(word);
        }
        return result;
    }

    private static Word getFromCache(String lemma, Page page) {
        HashSet<Word> cached = LemmaFactory.cachedWords.get(lemma);
        if (cached == null) {
            cachedWords.put(lemma, new HashSet<>());
            return null;
        }
        List<Word> matches = cached.stream().filter(x -> x.getSite().getId() == page.getSite().getId()).toList();
        if (matches.size() == 0) {
            return null;
        } else if (matches.size() == 1) {
            return matches.get(0);
        }
        LogUtil.logger.error("LemmaManager::getFromCache -> [%s] is cached multiple times for site_id [%s]".formatted(lemma, page.getSite().getId()));
        System.exit(2);
        return null;
    }

}
