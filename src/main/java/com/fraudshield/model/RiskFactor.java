package com.fraudshield.model;

/**
 * One explainable component of the risk score (e.g. AMOUNT, DEVICE).
 * score is 0-100 (how abnormal), weight is its share of the final score in percent,
 * contribution = score * weight / 100 points added to the final score.
 */
public class RiskFactor {
    public static final int TRIGGER_THRESHOLD = 40;

    private final String code;
    private final String name;
    private final int score;
    private final int weight;
    private final String description;

    public RiskFactor(String code, String name, int score, int weight, String description) {
        this.code = code;
        this.name = name;
        this.score = Math.max(0, Math.min(100, score));
        this.weight = weight;
        this.description = description == null ? "" : description;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public int getScore() { return score; }
    public int getWeight() { return weight; }
    public String getDescription() { return description; }

    public double getContribution() {
        return score * weight / 100.0;
    }

    public boolean isTriggered() {
        return score >= TRIGGER_THRESHOLD;
    }

    /** Single-line storage format: CODE|NAME|score|weight|description (pipes/newlines removed). */
    public String serialize() {
        return code + "|" + name.replace('|', '/') + "|" + score + "|" + weight + "|"
                + description.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }

    public static RiskFactor deserialize(String line) {
        String[] p = line.split("\\|", 5);
        if (p.length < 5) throw new IllegalArgumentException("Bad factor line: " + line);
        return new RiskFactor(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), p[4]);
    }
}
