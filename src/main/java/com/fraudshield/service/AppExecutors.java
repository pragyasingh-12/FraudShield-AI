package com.fraudshield.service;

import com.fraudshield.util.AppConfig;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Owns the application's thread pools so they can be shut down cleanly when Tomcat stops the app. */
public class AppExecutors {
    private final ExecutorService analysisPool;
    private final ScheduledExecutorService scheduler;

    public AppExecutors() {
        int threads = Math.max(1, AppConfig.getInt("analysis.threads", 4));
        this.analysisPool = Executors.newFixedThreadPool(threads, named("analysis-worker"));
        this.scheduler = Executors.newSingleThreadScheduledExecutor(named("analytics-refresher"));
    }

    private static ThreadFactory named(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }

    public ExecutorService analysisPool() { return analysisPool; }
    public ScheduledExecutorService scheduler() { return scheduler; }

    public void shutdown() {
        analysisPool.shutdown();
        scheduler.shutdownNow();
        try {
            analysisPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
