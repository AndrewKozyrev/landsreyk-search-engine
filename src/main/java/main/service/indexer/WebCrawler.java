package main.service.indexer;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;
import main.dao.PageRepository;
import main.dao.WordRepository;
import main.model.Page;
import main.model.Site;
import main.service.indexer.factory.PageFactory;
import main.utilities.BeanUtil;
import main.utilities.LogUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebCrawler extends RecursiveAction {

    private Pipeline pipeline;
    private LinkManager linkManager;
    private Site site;
    private final String url;
    public static volatile boolean isCancelled;

    private WebCrawler(String url) {
        this.url = url;
    }

    public WebCrawler(Site site, Pipeline pipeline) {
        PageRepository pageRepository = BeanUtil.getBean(PageRepository.class);
        pageRepository.deleteBySite(site);
        WordRepository wordRepository = BeanUtil.getBean(WordRepository.class);
        wordRepository.deleteBySite(site);
        this.site = site;
        linkManager = new LinkManager(site);
        this.url = site.getUrl();
        this.pipeline = pipeline;
    }

    @Override
    protected void compute() {
        if (isCancelled) {
            return;
        }
        Page page;
        try {
            page = PageFactory.create(url, site);
        } catch (Exception e) {
            Throwable rootCause = LogUtil.getRootCause(e);
            LogUtil.logger.fatal("%s -> %s".formatted(url, rootCause == null ? e.getMessage() : rootCause.getMessage()));
            return;
        }
        if (page == null) {
            return;
        }
        pipeline.run(page);
        Collection<WebCrawler> subtasks = createSubtasks(page);
        ForkJoinTask.invokeAll(subtasks);
    }

    /**
     * Создаёт список задач из внутренних ссылок документа
     *
     * @param page текущая страница
     * @return список main.application.WebCrawler
     */
    private Collection<WebCrawler> createSubtasks(Page page) {
        Collection<String> urls = extractUrls(page);
        return urls.stream().map(url -> {
            WebCrawler webCrawler = new WebCrawler(url);
            webCrawler.site = this.site;
            webCrawler.linkManager = this.linkManager;
            webCrawler.pipeline = this.pipeline;
            return webCrawler;
        }).collect(Collectors.toList());
    }

    /**
     * Метод получения ссылок из документа, которые раньше не встречались
     *
     * @param page текущая страница
     * @return список ссылок
     */
    private Collection<String> extractUrls(Page page) {
        Elements elements = Jsoup.parse(page.getContent(), site.getUrl()).select("a[href]");
        HashSet<String> result = new HashSet<>();
        for (Element e : elements) {
            String href = e.attr("abs:href");
            result.add(href);
        }
        linkManager.lock();
        linkManager.filter(result);
        linkManager.add(result);
        linkManager.unlock();

        return result;
    }
}
