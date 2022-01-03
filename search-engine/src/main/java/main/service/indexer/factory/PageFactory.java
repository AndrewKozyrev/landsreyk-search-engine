package main.service.indexer.factory;

import java.net.URL;
import java.util.Random;
import main.model.Page;
import main.model.Site;
import main.service.indexer.LinkManager;
import main.utilities.LogUtil;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public final class PageFactory {

  /**
   * Создаёт страницу
   *
   * @param url url адрес страницы
   * @return созданная страница
   */
  public static Page create(String url, Site site) throws Exception {
    LogUtil.logger.info("PageFactory::create -> start url: [%s]".formatted(url));
    String path = new URL(url).getPath();
    Response response = getResponse(url);
    if (!response.contentType().contains("text/html")) {
      LogUtil.logger.info("PageFactory::create -> url [%s] is declined, because it does not contain text/html".formatted(url));
      return null;
    }
    Page page = new Page();
    page.setUrl(path);
    page.setSite(site);
    Document document = response.parse();
    page.setCode(response.statusCode());
    page.setContent(document.outerHtml());
    LogUtil.logger.info("PageFactory::create -> created url: [%s]".formatted(url));
    return page;
  }

  /**
   * Метод получения http ответа на GET запрос
   *
   * @param url адрес страницы, к которой совершается запрос
   * @return возвращает ответ сервера
   */
  private static Response getResponse(String url) throws Exception {
    Connection connection = Jsoup.connect(url)
        .userAgent(LinkManager.USER_AGENT)
        .referrer(LinkManager.REFERRER);
    Thread.sleep(new Random().nextLong(500, 5000));
    return connection
        .ignoreContentType(true)
        .ignoreHttpErrors(true)
        .execute();
  }

  public static Page update(Page page) throws Exception {
    LogUtil.logger.info("PageFactory::update -> start page [%s]".formatted(page));
    Response response = getResponse(page.getSite().getUrl() + page.getUrl());
    if (!response.contentType().contains("text/html")) {
      LogUtil.logger.info("PageFactory::update -> url [%s] is declined, because it does not contain text/html".formatted(page.getUrl()));
      return null;
    }
    Document document = response.parse();
    page.setCode(response.statusCode());
    page.setContent(document.outerHtml());
    LogUtil.logger.info("PageFactory::update -> updated page [%s]".formatted(page));
    return page;
  }
}
