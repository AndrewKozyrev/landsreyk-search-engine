package main.service.indexer;

import main.model.Site;
import main.model.Site.Status;
import main.service.indexer.factory.SiteFactory;
import main.utilities.ApplicationProperties;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class WebCrawlerTest {
  @Autowired
  private ApplicationProperties applicationProperties;

  @Test
  public void test1() {
    Site site = applicationProperties.getSites().get(0);
    SiteFactory.update(site, Status.INDEXING);
    Pipeline pipeline = new Pipeline();
    WebCrawler webCrawler = new WebCrawler(site, pipeline);
    webCrawler.compute();
    pipeline.close();
  }
}