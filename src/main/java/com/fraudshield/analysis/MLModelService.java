package com.fraudshield.analysis;

import com.fraudshield.exception.FraudAnalysisException;
import com.fraudshield.util.AppConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

/**
 * Trains and serves the Weka model.
 * <ul>
 *   <li>Dataset: data/transactions.csv (synthetic, produced by util.DatasetGenerator)</li>
 *   <li>Algorithm: J48 decision tree (C4.5) with Laplace smoothing - easy to explain, outputs probabilities</li>
 *   <li>Evaluation: 70/30 hold-out test + 10-fold cross-validation</li>
 * </ul>
 * Thread safety: training runs once on a background thread and publishes the finished model through a
 * volatile field; predict() is synchronized because Weka classifiers are not documented as thread-safe.
 */
public class MLModelService {
    /** Feature order - MUST match the CSV columns and MLTransactionAnalyzer.features(). */
    public static final String[] FEATURES = {"amount", "amount_ratio", "transaction_frequency", "hour",
            "device_new", "location_changed", "behavior_deviation"};

    private volatile J48 model;
    private volatile Instances header;
    private volatile ModelReport report = ModelReport.notTrained();

    public boolean isReady() {
        return model != null;
    }

    public ModelReport getReport() {
        return report;
    }

    /** Loads the CSV, evaluates and trains the final model. Safe to call from a background thread. */
    public synchronized void train() {
        try {
            Instances data = loadDataset();
            int n = data.numInstances();
            int fraud = 0;
            for (int i = 0; i < n; i++) if ((int) data.instance(i).classValue() == 1) fraud++;

            // 1) hold-out: shuffle, 70% train / 30% test
            Instances shuffled = new Instances(data);
            shuffled.randomize(new Random(42));
            int trainSize = (int) Math.round(n * 0.7);
            Instances train = new Instances(shuffled, 0, trainSize);
            Instances test = new Instances(shuffled, trainSize, n - trainSize);
            J48 holdoutModel = newClassifier();
            holdoutModel.buildClassifier(train);
            Evaluation holdout = new Evaluation(train);
            holdout.evaluateModel(holdoutModel, test);

            // 2) 10-fold cross-validation on the full dataset
            Evaluation cv = new Evaluation(data);
            cv.crossValidateModel(newClassifier(), data, 10, new Random(42));

            // 3) final model on all data
            J48 finalModel = newClassifier();
            finalModel.buildClassifier(data);

            ModelReport r = new ModelReport();
            r.status = "READY";
            r.trainedAt = LocalDateTime.now();
            r.rows = n;
            r.fraudRows = fraud;
            r.trainRows = train.numInstances();
            r.testRows = test.numInstances();
            r.holdoutAccuracy = holdout.pctCorrect();
            r.holdoutPrecision = holdout.precision(1) * 100;
            r.holdoutRecall = holdout.recall(1) * 100;
            r.holdoutF1 = holdout.fMeasure(1) * 100;
            r.cvAccuracy = cv.pctCorrect();
            r.cvPrecision = cv.precision(1) * 100;
            r.cvRecall = cv.recall(1) * 100;
            r.cvF1 = cv.fMeasure(1) * 100;
            r.cvAuc = cv.areaUnderROC(1);
            double[][] cm = holdout.confusionMatrix();
            r.confusion = String.format(Locale.ROOT, "TN=%d  FP=%d  FN=%d  TP=%d",
                    (int) cm[0][0], (int) cm[0][1], (int) cm[1][0], (int) cm[1][1]);
            r.treeText = finalModel.toString();

            this.header = new Instances(data, 0);
            this.model = finalModel;
            this.report = r;
        } catch (Exception | LinkageError ex) {
            // LinkageError (e.g. NoClassDefFoundError from a missing Weka dependency) is an Error, not an
            // Exception, but must not stop the application: the engine falls back to rule-only scoring.
            ModelReport r = ModelReport.notTrained();
            r.status = "FAILED";
            r.error = String.valueOf(ex);
            this.report = r;
            this.model = null;
        }
    }

    private J48 newClassifier() {
        J48 tree = new J48();
        tree.setMinNumObj(5);        // each leaf needs >= 5 rows (less over-fitting)
        tree.setUseLaplace(true);    // smoother probabilities
        return tree;
    }

    /** Probability (0.0-1.0) that a transaction with these features is fraudulent. */
    public synchronized double predictFraudProbability(double[] features) throws FraudAnalysisException {
        J48 m = model;
        if (m == null) throw new FraudAnalysisException("ML model is not trained yet");
        if (features.length != FEATURES.length) throw new FraudAnalysisException("Wrong number of ML features");
        try {
            DenseInstance inst = new DenseInstance(header.numAttributes());
            inst.setDataset(header);
            for (int i = 0; i < features.length; i++) inst.setValue(i, features[i]);
            double[] dist = m.distributionForInstance(inst);
            return dist[1];
        } catch (Exception ex) {
            throw new FraudAnalysisException("ML prediction failed: " + ex.getMessage(), ex);
        }
    }

    // ------------------------------------------------------------ dataset

    private Instances loadDataset() throws IOException {
        ArrayList<Attribute> attrs = new ArrayList<>();
        for (String f : FEATURES) attrs.add(new Attribute(f));
        attrs.add(new Attribute("fraud", Arrays.asList("0", "1")));
        Instances data = new Instances("transactions", attrs, 2000);
        data.setClassIndex(attrs.size() - 1);

        try (BufferedReader br = openDataset()) {
            String header = br.readLine();
            if (header == null) throw new IOException("Dataset is empty");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] p = line.trim().split(",");
                if (p.length != FEATURES.length + 1) continue;          // skip malformed rows
                double[] vals = new double[p.length];
                for (int i = 0; i < FEATURES.length; i++) vals[i] = Double.parseDouble(p[i]);
                vals[FEATURES.length] = (int) Double.parseDouble(p[FEATURES.length]);   // class index 0 or 1
                data.add(new DenseInstance(1.0, vals));
            }
        }
        if (data.numInstances() < 50) throw new IOException("Dataset too small: " + data.numInstances() + " rows");
        return data;
    }

    private BufferedReader openDataset() throws IOException {
        String override = AppConfig.get("ml.dataset.path", "");
        if (!override.isBlank()) {
            Path p = Paths.get(override);
            if (Files.exists(p)) return Files.newBufferedReader(p, StandardCharsets.UTF_8);
        }
        InputStream in = MLModelService.class.getResourceAsStream("/data/transactions.csv");
        if (in == null) throw new IOException("data/transactions.csv not found on the classpath");
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /** Immutable-style report shown on the Analytics / About pages. */
    public static class ModelReport {
        private String status = "NOT_TRAINED";
        private String error = "";
        private LocalDateTime trainedAt;
        private int rows, fraudRows, trainRows, testRows;
        private double holdoutAccuracy, holdoutPrecision, holdoutRecall, holdoutF1;
        private double cvAccuracy, cvPrecision, cvRecall, cvF1, cvAuc;
        private String confusion = "";
        private String treeText = "";

        static ModelReport notTrained() { return new ModelReport(); }

        public String getStatus() { return status; }
        public String getError() { return error; }
        public boolean isReady() { return "READY".equals(status); }
        public String getTrainedAtFormatted() { return com.fraudshield.util.DateUtil.format(trainedAt); }
        public int getRows() { return rows; }
        public int getFraudRows() { return fraudRows; }
        public int getTrainRows() { return trainRows; }
        public int getTestRows() { return testRows; }
        public double getHoldoutAccuracy() { return holdoutAccuracy; }
        public double getHoldoutPrecision() { return holdoutPrecision; }
        public double getHoldoutRecall() { return holdoutRecall; }
        public double getHoldoutF1() { return holdoutF1; }
        public double getCvAccuracy() { return cvAccuracy; }
        public double getCvPrecision() { return cvPrecision; }
        public double getCvRecall() { return cvRecall; }
        public double getCvF1() { return cvF1; }
        public double getCvAuc() { return cvAuc; }
        public String getConfusion() { return confusion; }
        public String getTreeText() { return treeText; }
    }
}
