package main.utilities;

import lombok.Getter;
import lombok.NonNull;
import org.apache.lucene.morphology.LuceneMorphology;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Lexeme {
    @Getter
    String word;
    LuceneMorphology luceneMorph;
    // паттерн служебных частей речи
    private static final Pattern nonWords = Pattern.compile("ПРЕДЛ|СОЮЗ|МЕЖД|ПРЕДК|CONJ|PREP|PART");

    /**
     * Лексема
     * @param originalWord исходное слово
     * @param luceneMorph морфологический словарь
     */
    public Lexeme(@NonNull final String originalWord, @NonNull final LuceneMorphology luceneMorph) {
        this.luceneMorph = luceneMorph;
        word = extractWord(originalWord);
    }

    /**
     * Выделение начальной формы слова из исходной формы слова
     * @param originalWord исходное слово
     * @return начальная форма, полученная от исходного слова, null если служебная часть речи
     */
    String extractWord(String originalWord)
    {
        int wordType = getType(originalWord);
        String word;
        if (wordType == 0)  // у слова одна начальная форма
        {
            word = luceneMorph.getNormalForms(originalWord).get(0);
        }
        else if (wordType == 1) // несколько начальных форм подходят для слова
        {
            List<String> collect = luceneMorph.getNormalForms(originalWord);
            word = String.join("|", collect);
        }
        else // служебная часть речи
        {
            return null;
        }
        return word;
    }

    /**
     * Возвращает тип слова
     *
     * @param word исходное слово в любом виде
     * @return 0 - у слова единственная начальная форма; 1 - слово омоним; 2 - служебная часть речи
     */
    int getType(String word) {
        List<String> infoList = luceneMorph.getMorphInfo(word);
        List<String> collect = infoList.stream()
                .filter(info -> !nonWords.matcher(info).find())
                .collect(Collectors.toList());
        if (collect.size() == 1) {
            return 0;
        } else if (collect.size() > 1) {
            return 1;
        } else {
            return 2;
        }

    }

    /**
     * Очень ответственный метод. Он должен определять совпадают ли слова в своих начальных формах.
     * @param o объект для сравнения
     * @return true - если слова совпадают.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Lexeme other = (Lexeme) o;
        TreeSet<String> set1 = new TreeSet<>(Arrays.asList(word.split("\\|")));
        TreeSet<String> set2 = new TreeSet<>(Arrays.asList(other.word.split("\\|")));
        return set1.containsAll(set2);
    }

    @Override
    public int hashCode() {
        return 1;
    }

}
