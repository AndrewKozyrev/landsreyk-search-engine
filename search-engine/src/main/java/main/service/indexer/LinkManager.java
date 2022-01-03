package main.service.indexer;

import lombok.Getter;
import main.model.Site;
import main.utilities.LogUtil;
import org.apache.commons.validator.routines.UrlValidator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class LinkManager {
    private static int id;
    private final String name;
    public static String USER_AGENT = "LandsreykSearchBot/1.0 (+http://www.google.com/bot.html)";
    public static String REFERRER = "http://www.google.com";
    @Getter
    private final String baseHost;
    private final HashSet<String> visitedUrls = new HashSet<>();
    private final ReentrantLock mutex = new ReentrantLock();
    UrlValidator urlValidator;

    public LinkManager(Site site) {
        name = "LinkManager#" + id++;
        LogUtil.logger.info(name + " is created.");
        String h;
        urlValidator = new UrlValidator(new String[]{"http", "https"});
        URL url1 = null;
        try {
            url1 = new URL(site.getUrl());
        } catch (MalformedURLException e) {
            LogUtil.logger.fatal(e);
        }
        h = Objects.requireNonNull(url1).getHost();
        h = h.startsWith("www.") ? h.substring(4) : h;
        baseHost = h;
        add(site.getUrl());
    }

    /**
     * Проверяет ссылку
     *
     * @param toCheck ссылка
     * @return true - если ссылка - внутренняя ссылка главной страницы и уникальна
     */
    public boolean check(String toCheck) {
        if (!urlValidator.isValid(toCheck))
        {
            return false;
        }
        try {
            URL url2 = new URL(toCheck);
            String host = url2.getHost();
            host = host.startsWith("www.") ? host.substring(4) : host;
            boolean cond1 = baseHost.equals(host);
            boolean cond2 = !visitedUrls.contains(url2.getPath());
            boolean cond3 = !toCheck.contains("#");
            return cond1 && cond2 && cond3;
        } catch (MalformedURLException e) {
            LogUtil.logger.fatal(e);
            return false;
        }
    }

    /**
     * Добавляет ссылку в коллекцию
     *
     * @param url ссылка
     */
    private void add(String url) {
        try {
            // получаем путь от url
            String path = new URL(url).getPath();
            visitedUrls.add(path);
            LogUtil.logger.trace(name + "::add -> " + path + " is added.");
        } catch (MalformedURLException e) {
            LogUtil.logger.fatal(url, e);
        }
    }

    public void add(Collection<String> collection) {
        collection.forEach(this::add);
    }

    /**
     * Блокирует менеджер для синхронизации
     */
    public void lock() {
        mutex.lock();
    }

    /**
     * Снимает блокировку с менеджера
     */
    public void unlock() {
        mutex.unlock();
    }

    public void filter(Collection<String> collection) {
        collection.removeIf(url -> !check(url));
    }

}
