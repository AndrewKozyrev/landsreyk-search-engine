package main.controller;

import main.service.indexer.IndexBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexBuilderController {

  private final IndexBuilder indexBuilder;

  public IndexBuilderController(IndexBuilder indexBuilder) {
    this.indexBuilder = indexBuilder;
  }

  @GetMapping("/api/startIndexing")
  public ResponseEntity<?> startIndexing() {
    return indexBuilder.start();
  }

  @GetMapping("/api/stopIndexing")
  public ResponseEntity<?> stopIndexing() {
    return indexBuilder.stop();
  }

  @PostMapping("/api/indexPage")
  public ResponseEntity<?> indexPage(@RequestParam String url) throws Exception {
    return indexBuilder.indexPage(url);
  }

  /**
   * Метод возвращает статистику и другую служебную информацию о состоянии поисковых индексов и
   * самого движка.
   *
   * @return json response
   */
  @GetMapping("/api/statistics")
  public ResponseEntity<?> statistics() {
    return indexBuilder.statistics();
  }
}
