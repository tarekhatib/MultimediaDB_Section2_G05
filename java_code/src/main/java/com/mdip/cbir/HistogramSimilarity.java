package com.mdip.cbir;

/**
 * Similarity / distance measures between two L1-normalised histograms.
 *
 * <p>The primary metric is <b>histogram intersection</b> (Swain &amp; Ballard, 1991):
 * for normalised histograms it lies in [0, 1] (1 = identical colour distribution),
 * needs no tuning, and is robust to background clutter and partial occlusion because
 * it only counts the colour mass the two images share. Chi-square distance is provided
 * as a secondary metric for comparison in the report.
 */
public final class HistogramSimilarity {

    private HistogramSimilarity() {
    }

    /** @return sum_i min(a_i, b_i), in [0, 1] for normalised histograms; higher = more similar. */
    public static double intersection(HsvHistogram a, HsvHistogram b) {
        requireSameLayout(a, b);
        double sum = 0;
        for (int i = 0; i < a.size(); i++) {
            sum += Math.min(a.valueAt(i), b.valueAt(i));
        }
        return sum;
    }

    /**
     * Symmetric chi-square distance: 0.5 * sum_i (a_i - b_i)^2 / (a_i + b_i).
     *
     * @return a value in [0, 1] for normalised histograms; lower = more similar.
     */
    public static double chiSquare(HsvHistogram a, HsvHistogram b) {
        requireSameLayout(a, b);
        double sum = 0;
        for (int i = 0; i < a.size(); i++) {
            double x = a.valueAt(i);
            double y = b.valueAt(i);
            double denom = x + y;
            if (denom > 0) {
                double diff = x - y;
                sum += diff * diff / denom;
            }
        }
        return 0.5 * sum;
    }

    private static void requireSameLayout(HsvHistogram a, HsvHistogram b) {
        if (!a.sameLayoutAs(b)) {
            throw new IllegalArgumentException(
                    "Histogram layouts differ: " + a.type() + " vs " + b.type());
        }
    }
}
