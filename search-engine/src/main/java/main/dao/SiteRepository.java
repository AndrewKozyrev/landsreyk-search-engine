package main.dao;

import main.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByUrl(String url);
}
