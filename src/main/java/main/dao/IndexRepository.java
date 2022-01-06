package main.dao;

import java.util.List;
import main.model.Index;
import main.model.Page;
import main.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
  List<Index> findByPage(Page page);

  List<Index> findByWord(Word word);

  Index findByPageAndWord(Page page, Word word);
}
