package main.service.indexer;

import main.model.Index;
import main.model.Page;
import main.model.Word;
import main.service.indexer.factory.IndexFactory;
import main.service.indexer.factory.LemmaFactory;
import main.utilities.BeanUtil;
import main.utilities.LogUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Pipeline {

    public static volatile boolean isCancelled;
    private static int id;
    private final SessionFactory sessionFactory;
    private final String name;
    private final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    private final ExecutorService threadExecutor;
    private volatile boolean threadIsActive;

    public Pipeline() {
        name = "Pipeline#" + id++;
        LogUtil.logger.info(name + " is created.");
        sessionFactory = BeanUtil.getBean(SessionFactory.class);
        threadExecutor = Executors.newSingleThreadExecutor();
    }

    public void run(Page page) {
        LogUtil.logger.info(name + "::run -> started");
        if (isCancelled) {
            LogUtil.logger.info(name + "::run -> canceled");
            return;
        }
        persist(page);
        if (page.getCode() == 404 || page.getCode() == 500) {
            LogUtil.logger.info(name + "::run -> page [%s] is declined due to status code.".formatted(page));
            return;
        }
        Collection<Word> words = LemmaFactory.create(page);
        persist(words);
        Collection<Index> indices = IndexFactory.create(page, words);
        persist(indices);
        LogUtil.logger.info(name + "::run -> ended");
    }

    private <T> void persist(T entity) {
        queue.add(entity);
        if (!threadIsActive) {
            LogUtil.logger.trace(name + "::persist -> invoking worker.");
            threadExecutor.execute(this::flush);
        }
    }

    private <T> void persist(Collection<T> entities) {
        entities.forEach(this::persist);
    }

    private void flush() {
        threadIsActive = true;
        try {
            LogUtil.logger.info(name + "::flush -> started.");
            int currentSize = queue.size();
            while (currentSize > 0) {
                if (isCancelled) {
                    LogUtil.logger.info(name + "::flush -> canceled");
                    break;
                }
                LogUtil.logger.info(name + "::flush -> queue size [%d].".formatted(currentSize));
                LogUtil.logger.info(name + "::flush -> opening session.");
                Session session = sessionFactory.openSession();
                LogUtil.logger.info(name + "::flush -> begin transaction.");
                session.beginTransaction();
                for (int i = 0; i < currentSize; i++) {
                    if (isCancelled) {
                        LogUtil.logger.info(name + "::flush -> canceled");
                        break;
                    }
                    Object entity = queue.poll();
                    LogUtil.logger.trace(name + "::flush -> persisting entity " + entity);
                    session.saveOrUpdate(entity);
                }
                LogUtil.logger.info(name + "::flush -> committing transaction.");
                session.getTransaction().commit();
                LogUtil.logger.info(name + "::flush -> closing session.");
                session.close();
                currentSize = queue.size();
            }
            LogUtil.logger.info(name + "::flush -> ended.");
            threadIsActive = false;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void close() {
        LogUtil.logger.info(name + "::close -> started.");
        while (threadIsActive) {
            Thread.onSpinWait();
        }
        LogUtil.logger.info(name + "::close -> ended.");
    }


}
