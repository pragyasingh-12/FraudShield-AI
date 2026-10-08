package com.fraudshield.servlet;

import com.fraudshield.db.DBConnection;
import com.fraudshield.service.ServiceRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Application start/stop hook. On start it creates the services and launches ONE background task that
 * (1) trains the Weka model and (2) analyses any transactions still PENDING (demo data / crash recovery).
 * On stop it shuts the thread pools down.
 */
@WebListener
public class AppContextListener implements ServletContextListener {
    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServiceRegistry registry = ServiceRegistry.init();
        registry.dashboard().startBackgroundRefresh(registry.executors());
        registry.executors().analysisPool().submit(() -> {
            // Step 1: ML training. A failure here must not prevent step 2 (rules still work without ML).
            try {
                registry.ml().train();
                LOG.log(Level.INFO, "Weka model status: {0}", registry.ml().getReport().getStatus());
                if (!registry.ml().isReady()) {
                    LOG.log(Level.WARNING, "Weka model not available, using rules only: {0}", registry.ml().getReport().getError());
                }
            } catch (Throwable ex) {
                LOG.log(Level.SEVERE, "ML training crashed; continuing with rule-based analysis only", ex);
            }
            // Step 2: analyse transactions that are still PENDING (sample data / crash recovery).
            try {
                if (DBConnection.isAvailable()) {
                    registry.detection().loadSuspiciousDevices();
                    int n = registry.detection().analyzePending();
                    LOG.log(Level.INFO, "Start-up analysis finished: {0} pending transaction(s) analysed", n);
                    registry.dashboard().refresh();
                } else {
                    LOG.warning("Database not reachable - check application.properties");
                }
            } catch (Throwable ex) {
                LOG.log(Level.SEVERE, "Start-up analysis failed", ex);
            }
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            ServiceRegistry.get().shutdown();
        } catch (IllegalStateException ignored) {
            // already shut down
        }
    }
}
