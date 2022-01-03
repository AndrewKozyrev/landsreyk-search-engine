package main.service.indexer.factory;

import java.util.Collection;
import main.model.Index;
import main.model.Page;
import main.model.Word;
import main.utilities.LogUtil;

public class IndexFactory {

    public synchronized static Collection<Index> create(Page page, Collection<Word> words) {
        LogUtil.logger.info("IndexFactory::create -> start page [%s]".formatted(page));
        Collection<Index> indices = words.parallelStream().map(word -> {
            Index index = new Index();
            index.setPage(page);
            index.setWord(word);
            index.setRank(word.getRank());
            return index;
        }).toList();
        LogUtil.logger.info("IndexFactory::create -> end page [%s]".formatted(page));
        return indices;
    }
}
