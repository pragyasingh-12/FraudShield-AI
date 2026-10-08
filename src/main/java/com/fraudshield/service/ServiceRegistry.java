package com.fraudshield.service;

import com.fraudshield.analysis.HybridRiskAnalyzer;
import com.fraudshield.analysis.MLModelService;
import com.fraudshield.analysis.MLTransactionAnalyzer;
import com.fraudshield.analysis.RuleBasedAnalyzer;

/**
 * Creates and holds the application-wide singletons (a tiny hand-made dependency container).
 * Servlets call ServiceRegistry.get() instead of creating their own services, so every request
 * shares the same thread pools, ML model and caches.
 */
public final class ServiceRegistry {
    private static volatile ServiceRegistry instance;

    private final AppExecutors executors = new AppExecutors();
    private final MLModelService mlService = new MLModelService();
    private final BehaviorProfileService profileService = new BehaviorProfileService();
    private final DashboardService dashboardService = new DashboardService();
    private final FraudDetectionService detectionService;
    private final TransactionService transactionService;
    private final AuthenticationService authService = new AuthenticationService();
    private final UserService userService = new UserService(profileService);
    private final AlertService alertService;
    private final RiskAnalysisService riskAnalysisService = new RiskAnalysisService(mlService);

    private ServiceRegistry() {
        HybridRiskAnalyzer analyzer = new HybridRiskAnalyzer(new RuleBasedAnalyzer(), new MLTransactionAnalyzer(mlService));
        this.detectionService = new FraudDetectionService(profileService, analyzer, executors, dashboardService);
        this.transactionService = new TransactionService(detectionService, executors);
        this.alertService = new AlertService(detectionService);
    }

    public static synchronized ServiceRegistry init() {
        if (instance == null) instance = new ServiceRegistry();
        return instance;
    }

    public static ServiceRegistry get() {
        ServiceRegistry r = instance;
        if (r == null) throw new IllegalStateException("ServiceRegistry not initialised (context listener did not run)");
        return r;
    }

    public AppExecutors executors() { return executors; }
    public MLModelService ml() { return mlService; }
    public DashboardService dashboard() { return dashboardService; }
    public FraudDetectionService detection() { return detectionService; }
    public TransactionService transactions() { return transactionService; }
    public AuthenticationService auth() { return authService; }
    public UserService users() { return userService; }
    public AlertService alerts() { return alertService; }
    public RiskAnalysisService risk() { return riskAnalysisService; }

    public void shutdown() {
        executors.shutdown();
        instance = null;
    }
}
