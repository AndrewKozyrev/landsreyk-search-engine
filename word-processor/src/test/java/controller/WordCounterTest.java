package controller;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

class WordCounterTest {

    WordCounter wordCounter;
    @BeforeEach
    void setUp() throws IOException {
        wordCounter = new WordCounter();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @DisplayName("test for no exception")
    void test1() throws IOException {
        File file = new File("src/test/resources/info.log");
        String text = Files.readString(file.toPath());
        Assertions.assertDoesNotThrow(() -> wordCounter.getStats(text));
    }

    @Test
    @DisplayName("test for word separation")
    void test2() {
        String text = "some people have long curly hair мои слова";
        Collection<String> actual = WordCounter.selectWords(text);
        List<String> expected = Arrays.asList("some", "people", "have", "long", "curly", "hair", "мои", "слова");
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("get stats check")
    void test3() {
        String text = "hello-world it's night time телефон-Samsung желто-зеленого";
        Map<String, Integer> actual = wordCounter.getStats(text);
        Map<String, Integer> expected = new HashMap<>() {{
            put("телефон", 1);
            put("it", 1);
            put("night", 1);
            put("hello-world", 1);
            put("желто-зеленый", 1);
            put("samsing", 1);
            put("time", 1);
        }};
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("unknown words")
    void test4() {
        String word = "шxвxт";
        Map<String, Integer> actual = wordCounter.getStats(word);
        Map<Object, Object> expected = Collections.emptyMap();
        Assertions.assertEquals(expected, actual);
    }
}