package com.fraudshield.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Random;

/**
 * Generates the SYNTHETIC training dataset (data/transactions.csv). No real payment data is used.
 *
 * Features are drawn from realistic distributions (mostly normal behaviour, some abnormal bursts).
 * The fraud label is then drawn from a noisy logistic model of those features, so the classes
 * overlap and the learned model gets a realistic (not 100%) accuracy.
 *
 * Run:  java -cp target/classes com.fraudshield.util.DatasetGenerator data/transactions.csv 2000
 */
public final class DatasetGenerator {
    /** How sharply the fraud probability rises with risk evidence (higher = cleaner separation). */
    private static final double STEEPNESS = 3.0;
    /** Evidence level at which fraud becomes 50% likely. */
    private static final double THRESHOLD = -1.0;

    private DatasetGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : "data/transactions.csv");
        int rows = args.length > 1 ? Integer.parseInt(args[1]) : 2000;
        Random rnd = new Random(2026);
        double[] userAverages = {800, 1500, 2500, 4000, 6000, 9000};
        int fraudCount = 0;

        if (out.getParent() != null) Files.createDirectories(out.getParent());
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            w.println("amount,amount_ratio,transaction_frequency,hour,device_new,location_changed,behavior_deviation,fraud");
            for (int i = 0; i < rows; i++) {
                double avg = userAverages[rnd.nextInt(userAverages.length)];
                double ratio = rnd.nextDouble() < 0.12 ? Math.exp(1.4 + 0.6 * rnd.nextGaussian())
                        : Math.exp(0.45 * rnd.nextGaussian());
                ratio = Math.round(ratio * 100.0) / 100.0;
                double amount = Math.round(ratio * avg * 100.0) / 100.0;

                int freq = rnd.nextDouble() < 0.10 ? 2 + poisson(rnd, 1.5) : poisson(rnd, 0.4);
                freq = Math.min(freq, 8);

                int hour;
                double h = rnd.nextDouble();
                if (h < 0.80) hour = 8 + rnd.nextInt(15);          // 08-22 normal
                else if (h < 0.88) hour = 5 + rnd.nextInt(3);      // 05-07 early
                else if (h < 0.93) hour = 23;
                else hour = rnd.nextInt(5);                        // 00-04 night

                int deviceNew = rnd.nextDouble() < 0.07 ? 1 : 0;
                int locChanged = rnd.nextDouble() < 0.08 ? 1 : 0;

                int behavior = 0;
                if (rnd.nextDouble() < 0.25) behavior += 50;       // new receiver
                if (ratio > 2.5) behavior += 30;                   // far above previous maximum
                if (rnd.nextDouble() < 0.05) behavior += 20;       // rarely used payment type

                double z = -4.2
                        + 1.2 * Math.max(0, Math.log(Math.max(ratio, 0.1)))
                        + 0.55 * Math.min(freq, 5)
                        + (hour < 5 ? 1.8 : (hour < 7 || hour == 23) ? 0.6 : 0)
                        + 1.6 * deviceNew
                        + 1.4 * locChanged
                        + 0.012 * behavior
                        + 0.5 * rnd.nextGaussian();
                double p = 1.0 / (1.0 + Math.exp(-STEEPNESS * (z - THRESHOLD)));
                int fraud = rnd.nextDouble() < p ? 1 : 0;
                fraudCount += fraud;

                w.println(String.format(Locale.ROOT, "%.2f,%.2f,%d,%d,%d,%d,%d,%d",
                        amount, ratio, freq, hour, deviceNew, locChanged, behavior, fraud));
            }
        }
        System.out.printf(Locale.ROOT, "Wrote %d rows to %s (fraud rate %.1f%%)%n", rows, out, 100.0 * fraudCount / rows);
    }

    /** Knuth's algorithm for Poisson-distributed integers. */
    private static int poisson(Random rnd, double lambda) {
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1;
        do {
            k++;
            p *= rnd.nextDouble();
        } while (p > l);
        return k - 1;
    }
}
