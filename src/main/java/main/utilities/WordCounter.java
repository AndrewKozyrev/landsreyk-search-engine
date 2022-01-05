package main.utilities;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class WordCounter {
    public static LuceneMorphology russianMorph;
    public static LuceneMorphology englishMorph;

    static {
        try {
            russianMorph = new RussianLuceneMorphology();
            englishMorph = new EnglishLuceneMorphology();
        } catch (IOException e) {
            LogUtil.logger.fatal(
                    "WordCounter can't be instantiated. Exiting main.application, fix your bug.", e);
            System.exit(2);
        }
    }

    /**
     * Выделяет слова из текста с помощью регулярных выражений
     * @param text текст для разделения
     * @return список слов, пустой список - если подходящих слов нет
     */
    public static Collection<String> selectWords(String text) {
        ArrayList<String> result = new ArrayList<>();
        Pattern p = Pattern.compile("[a-zA-Zа-яА-Я]+-?[a-zA-Zа-яА-Я]+"); // word pattern
        Matcher matcher = p.matcher(text);    // find words matching mattern
        if (!matcher.find()) {
            return Collections.emptyList();
        }
        matcher.reset();
        while (matcher.find()) {
            String word = matcher.group();  // extract next word
            if (word.contains("-")) {
                if (!russianMorph.checkString(word) && !englishMorph.checkString(word)) {
                    List<String> strings = Arrays.stream(word.split("-")).map(String::toLowerCase).toList();
                    result.addAll(strings);
                }
                else
                {
                    result.add(word.toLowerCase());
                }
            } else {
                result.add(word.toLowerCase());
            }
        }
        return result;
    }

    /**
     * Создает отображение word -> count
     *
     * @param text исходный текст
     * @return словарь (key, value) = (словоформа, частота_слова)
     */
    public static Map<String, Integer> getStats(String text) {
        Collection<String> words = selectWords(text);
        ConcurrentHashMap<Lexeme, Integer> map = new ConcurrentHashMap<>();
        words.parallelStream().forEach(word ->
        {
            Lexeme lexeme;
            if (russianMorph.checkString(word)) {
                lexeme = new Lexeme(word, russianMorph);    // if it's a russian word
            } else if (englishMorph.checkString(word)) {
                lexeme = new Lexeme(word, englishMorph);    // for english
            } else {
                return;
            }
            if (lexeme.getWord() == null) {
                return;  // служебная часть речи, пропускаем
            }
            if (map.containsKey(lexeme)) {  // если такая словоформа есть
                Lexeme key = map.keySet().stream()  // берем ключ
                        .filter(lexeme::equals)
                        .findFirst()
                        .get();
                int count = map.get(lexeme);   // получаем частоту этой лексемы
                if (lexeme.getWord().length() > key.getWord().length()) {  // если лексема обширнее существующей
                    key = lexeme;   // заменяем то, что было на более подробную версию этой лексемы
                }
                map.put(key, count + 1);
            } else {
                map.put(lexeme, 1);
            }
        });

        return map.entrySet().parallelStream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(x -> x.getKey().getWord(),
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

}
