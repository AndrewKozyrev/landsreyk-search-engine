package main.service.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.dao.IndexRepository;
import main.dao.PageRepository;
import main.dao.SiteRepository;
import main.dao.WordRepository;
import main.model.Index;
import main.model.Page;
import main.model.Site;
import main.model.Site.Status;
import main.model.Word;
import main.service.indexer.factory.IndexFactory;
import main.service.indexer.factory.LemmaFactory;
import main.service.indexer.factory.PageFactory;
import main.service.indexer.factory.SiteFactory;
import main.utilities.ApplicationProperties;
import main.utilities.LogUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;

@Service
public class IndexBuilder {

    private final ApplicationProperties properties;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final WordRepository wordRepository;
    private final IndexRepository indexRepository;
    private final ThreadPoolExecutor threadPool;
    private LaunchState launchState = LaunchState.IDLE;

    public IndexBuilder(ApplicationProperties properties,
                        SiteRepository siteRepository,
                        PageRepository pageRepository,
                        WordRepository wordRepository,
                        IndexRepository indexRepository) {
        this.properties = properties;
        this.siteRepository = siteRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.wordRepository = wordRepository;
        threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    public ResponseEntity<?> stop() {
        LogUtil.logger.info("IndexBuilder::stop");
        if (launchState == LaunchState.IDLE) {
            return ResponseEntity.status(409)
                    .body(Map.of("result", false, "error", "Индексация не запущена"));
        }
        if (launchState == LaunchState.STOPPING) {
            return ResponseEntity.status(409).body(Map.of("result", false, "error", "Индексация останавливается."));
        }
        launchState = LaunchState.STOPPING;
        Runnable runnable = () -> {
            WebCrawler.isCancelled = true;
            Pipeline.isCancelled = true;
            while (launchState != LaunchState.IDLE) {
                Thread.onSpinWait();
            }
            Pipeline.isCancelled = false;
            WebCrawler.isCancelled = false;
        };
        threadPool.execute(runnable);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("result", true));
    }

    public ResponseEntity<?> start() {
        LogUtil.logger.info("IndexBuilder::start");
        if (launchState == LaunchState.BUILDING_INDEX) {
            return ResponseEntity.status(409).body(Map.of("result", false, "error", "Индексация уже запущена"));
        }
        if (launchState == LaunchState.STOPPING) {
            return ResponseEntity.status(409).body(Map.of("result", false, "error", "Индексация всё ещё останавливается."));
        }
        Runnable runnable = () -> {
            launchState = LaunchState.BUILDING_INDEX;
            List<Site> sites = properties.getSites();
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (Site site : sites) {
                tasks.add(() -> indexSite(site));
            }
            try {
                threadPool.invokeAll(tasks);
            } catch (Exception e) {
                LogUtil.logger.fatal(e);
                System.exit(3);
            }
            launchState = LaunchState.IDLE;
        };
        threadPool.execute(runnable);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("result", true));
    }

    private boolean indexSite(Site site) {
        try {
            SiteFactory.update(site, Status.INDEXING);
            Pipeline pipeline = new Pipeline();
            WebCrawler webCrawler = new WebCrawler(site, pipeline);
            ForkJoinPool pool = ForkJoinPool.commonPool();
            pool.invoke(webCrawler);
            pipeline.close();
            if (launchState == LaunchState.STOPPING) {
                SiteFactory.update(site, "Индексация остановлена");
            } else {
                SiteFactory.update(site, Status.INDEXED);
            }
        } catch (Exception e) {
            LogUtil.logger.fatal(site.getUrl(), e);
            SiteFactory.update(site, e);
            return false;
        }
        return true;
    }

    public ResponseEntity<?> indexPage(String url) throws Exception {
        Optional<Site> found = properties.getSites().stream().filter(x -> new LinkManager(x).check(url))
                .findAny();
        if (found.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("result", false, "error",
                    "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"));
        }
        Site site = found.get();
        Optional<Page> optionalPage = pageRepository.findByUrlAndSite(new URL(url).getPath(), site);
        Page page;
        if (optionalPage.isPresent()) {
            page = PageFactory.update(optionalPage.get());
        } else {
            page = PageFactory.create(url, site);
        }
        if (page == null) {
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("result", true));
        }
        pageRepository.save(page);
        if (page.getCode() == 404 || page.getCode() == 500) {
            LogUtil.logger.info(
                    "IndexBuilder::indexPage -> page [%s] is declined due to status code.".formatted(page));
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("result", true));
        }
        List<Index> oldIndices = indexRepository.findByPage(page);
        List<Word> oldWords = oldIndices.stream().map(Index::getWord).toList();
        Collection<Word> toPersist = LemmaFactory.update(oldWords, page);
        List<Word> toDelete = new ArrayList<>();
        oldWords.stream().filter(Predicate.not(toPersist::contains)).forEach(toDelete::add);
        wordRepository.deleteAll(toDelete);
        wordRepository.saveAll(toPersist);
        indexRepository.deleteAll(oldIndices);
        Collection<Index> indices = IndexFactory.create(page, toPersist);
        indexRepository.saveAll(indices);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("result", true));
    }

    public ResponseEntity<?> statistics() {
        LogUtil.logger.info("IndexBuilder::statistics");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("result", true);
        ObjectNode statistics = root.putObject("statistics");
        ObjectNode total = statistics.putObject("total");
        total.put("sites", siteRepository.count());
        total.put("pages", pageRepository.count());
        total.put("lemmas", wordRepository.count());
        total.put("isIndexing", launchState != LaunchState.IDLE);
        ArrayNode arrayNode = statistics.putArray("detailed");
        for (Site site : siteRepository.findAll()) {
            ObjectNode node = arrayNode.addObject();
            node.put("url", site.getUrl());
            node.put("name", site.getName());
            node.put("status", site.getStatus().toString());
            node.put("statusTime", site.getStatusTime().toString());
            node.put("error", site.getLastError());
            node.put("pages", pageRepository.countBySiteId(site));
            node.put("lemmas", wordRepository.countBySiteId(site));
        }
        return ResponseEntity.status(HttpStatus.OK).body(root);
    }

    private enum LaunchState {
        BUILDING_INDEX,
        STOPPING,
        IDLE
    }
}
