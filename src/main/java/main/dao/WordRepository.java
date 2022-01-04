package main.dao;

import java.util.Optional;
import main.model.Site;
import main.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface WordRepository extends JpaRepository<Word, Integer> {
    Optional<Word> findByNameAndSite(String name, Site site);

    @Query("SELECT COUNT(*) FROM Word WHERE site_id =:#{#site.id}")
    long countBySiteId(@Param("site") Site site);

    @Transactional
    void deleteBySite(Site site);
}
