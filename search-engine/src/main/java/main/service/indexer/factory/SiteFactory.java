package main.service.indexer.factory;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import main.dao.SiteRepository;
import main.model.Site;
import main.model.Site.Status;
import main.utilities.ApplicationProperties;
import main.utilities.BeanUtil;
import main.utilities.LogUtil;

public class SiteFactory {
  private static final ApplicationProperties applicationProperties = BeanUtil.getBean(ApplicationProperties.class);
  private static final SiteRepository siteRepository = BeanUtil.getBean(SiteRepository.class);

  public static void update(Site site, Status status) {
    site.setStatus(status);
    site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
    site.setLastError(null);
    siteRepository.save(site);
  }

  public static void update(Site site, String errorMessage) {
    site.setStatus(Status.FAILED);
    site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
    site.setLastError(errorMessage);
    siteRepository.save(site);
  }

  public static void update(Site site, Exception e) {
    site.setStatus(Site.Status.FAILED);
    site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
    Throwable rootCause = LogUtil.getRootCause(e);
    site.setLastError(rootCause == null ? e.getMessage() : rootCause.getMessage());
    siteRepository.save(site);
  }

  public static Site create(String s) throws IllegalArgumentException {
    Optional<Site> optionalSite = applicationProperties.getSites().stream()
        .filter(x -> x.getUrl().equals(s))
        .findAny();
    if (optionalSite.isEmpty()) {
      throw new IllegalArgumentException("Такого сайта нет в индексе.");
    }
    return optionalSite.get();
  }
}
