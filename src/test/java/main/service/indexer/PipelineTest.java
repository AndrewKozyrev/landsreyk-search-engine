package main.service.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import main.model.Page;
import main.model.Site;
import main.model.Site.Status;
import main.service.indexer.factory.PageFactory;
import main.service.indexer.factory.SiteFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class PipelineTest {

  @Test
  public void test1() throws Exception {
    long start = System.currentTimeMillis();
    Pipeline pipeline = new Pipeline();
    Site site = SiteFactory.create("https://www.playback.ru");
    SiteFactory.update(site, Status.INDEXING);
    File file = new File("src/test/resources/pages.txt");
    FileInputStream fos = new FileInputStream(file);
    ObjectInputStream stream = new ObjectInputStream(fos);
    List<?> urls = (List<?>) stream.readObject();
    urls.parallelStream().forEach(url -> {
      Page page = null;
      try {
        page = PageFactory.create((String) url, site);
      } catch (Exception e) {
        e.printStackTrace();
      }
      pipeline.run(page);
    });
    pipeline.close();
    long duration = System.currentTimeMillis() - start;
    System.out.printf("Duration = %d ms", duration);
  }
}