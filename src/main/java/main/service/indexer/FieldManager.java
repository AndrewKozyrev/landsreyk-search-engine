package main.service.indexer;

import lombok.Getter;
import main.dao.FieldRepository;
import main.model.Field;
import main.model.Page;
import main.utilities.BeanUtil;
import main.utilities.WordCounter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class FieldManager {
  private final List<Field> list;

  public FieldManager() {
    list = BeanUtil.getBean(FieldRepository.class).findAll();
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
    return WordCounter.getStats(sb.toString());
  }
}
