package main.service.indexer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import main.model.Field;
import main.dao.FieldRepository;
import main.model.Page;
import main.utilities.BeanUtil;
import main.utilities.LogUtil;
import main.utilities.WordCounter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Getter
public class FieldManager {
  private static WordCounter analyzer;
  private final List<Field> list;

  public FieldManager() {
    list = BeanUtil.getBean(FieldRepository.class).findAll();
    try {
      analyzer = new WordCounter();
    } catch (IOException e) {
      LogUtil.logger.fatal(
          "WordCounter can't be instantiated. Exiting main.application, fix your bug.", e);
      System.exit(2);
    }
  }

  public Map<String, Float> parse(Page page) {
    ConcurrentHashMap<String, Float> stats = new ConcurrentHashMap<>();
    for (Field field : list) {
      Map<String, Integer> wordFrequency = analyze(page, field);
      wordFrequency.keySet().parallelStream().forEach(lemma -> stats.compute(lemma, (k, v) -> v == null ? wordFrequency.get(lemma) * field.getWeight()
          : v + wordFrequency.get(lemma) * field.getWeight()));
    }
    return stats;
  }

  private static Map<String, Integer> analyze(Page page, Field field) {
    Elements select = Jsoup.parse(page.getContent()).select(field.getSelector());
    StringBuilder sb = new StringBuilder();
    for (Element element : select) {
      String text = element.text();
      sb.append(text);
    }
    return analyzer.getStats(sb.toString());
  }
}
