package model;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.junit.jupiter.api.*;

import java.io.IOException;

class LexemeTest {
    LuceneMorphology russianMorph;
    LuceneMorphology englishMorph;

    @BeforeEach
    void setUp() throws IOException {
        russianMorph = new RussianLuceneMorphology();
        englishMorph = new EnglishLuceneMorphology();
    }

    @Test
    @DisplayName("russian morphology test for hyphen words")
    void test1() {
        Lexeme lexeme = new Lexeme("кёмто-зеленого", russianMorph);
        String actual = lexeme.getWord();
        String expected = "кёмто-зеленый";
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("english morphology test for hyphen words")
    void test2() {
        Lexeme lexeme = new Lexeme("tool-assisted", englishMorph);
        String actual = lexeme.getWord();
        String expected = "tool-assisted";
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("english morphology test for apostrophe words")
    void test3() {
        Lexeme lexeme = new Lexeme("it's", englishMorph);
        String actual = lexeme.getWord();
        String expected = "it's";
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("получение падежей")
    void test4() {
        String text = "над поляной я увидел зверя";
        String word = "поляна";
        if (text.contains(word.substring(0, word.length() - 2)))
        {
            int i = text.indexOf(word.substring(0, word.length() - 2));
            int j = text.substring(i).indexOf(" ");
            String other = text.substring(i, i + j);
            Lexeme lexeme = new Lexeme(other, russianMorph);
            Assertions.assertEquals(word, lexeme.getWord());
        }
    }

    @AfterEach
    void tearDown() {
    }
}