package com.fraudshield.analysis;

/** Transparent weights (percent) of the six rule-based risk factors. They add up to 100. */
public final class RiskWeights {
    public static final int AMOUNT = 25;
    public static final int FREQUENCY = 20;
    public static final int TIME = 15;
    public static final int DEVICE = 15;
    public static final int LOCATION = 15;
    public static final int BEHAVIOR = 10;

    private RiskWeights() {
    }
}
