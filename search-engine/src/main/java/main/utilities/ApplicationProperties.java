package main.utilities;

import java.util.List;
import java.util.Optional;
import lombok.Setter;
import main.dao.SiteRepository;
import main.model.Site;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class ApplicationProperties {
    private final SiteRepository siteRepository;

    @Setter
    private List<Site> sites;

    public ApplicationProperties(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public List<Site> getSites() {
        return sites.parallelStream().map(x -> {
            Optional<Site> optionalSite = siteRepository.findByUrl(x.getUrl());
            return optionalSite.orElse(x);
        }).toList();
    }
}