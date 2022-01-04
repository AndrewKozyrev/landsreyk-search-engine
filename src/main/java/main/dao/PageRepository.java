package main.dao;

import java.util.List;
import java.util.Optional;
import main.model.Page;
import main.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

  Optional<Page> findByUrlAndSite(String url, Site site);

  @Query("SELECT COUNT(*) FROM Page WHERE site_id =:#{#site.id}")
  long countBySiteId(@Param("site") Site site);

  @Transactional
  void deleteBySite(Site site);

  List<Page> findBySite(Site site);
}
