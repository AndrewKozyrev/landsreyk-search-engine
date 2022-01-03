package main.controller;

import main.service.searcher.SearchClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {
    private final SearchClient searchClient;

    public SearchController(SearchClient searchClient) {
        this.searchClient = searchClient;
    }

    @GetMapping("/api/search")
    public ResponseEntity<?> search(@RequestParam String query, @RequestParam(required = false) String site,
                                 @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int limit) {
        return searchClient.search(query, site, offset, limit);
    }
}
