package com.mdib.cbir;

import java.util.Arrays;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Immutable, L1-normalised global HSV colour histogram (a CBIR feature vector).
 *
 * <p>Bin layout: index = h * (sBins * vBins) + s * vBins + v, so the vector is
 * hue-major. Values are non-negative and sum to 1 (for a non-empty image).
 * See {@code docs/cbir-descriptor.md} for the full format.
 */
public final class HsvHistogram {

    /** Descriptor type tag stored with each vector, e.g. {@code HSV_HIST_8x4x4}. */
    public static String typeName(int hBins, int sBins, int vBins) {
        return "HSV_HIST_" + hBins + "x" + sBins + "x" + vBins;
    }

    private final int hBins;
    private final int sBins;
    private final int vBins;
    private final float[] values;

    public HsvHistogram(int hBins, int sBins, int vBins, float[] values) {
        if (hBins <= 0 || sBins <= 0 || vBins <= 0) {
            throw new IllegalArgumentException("Bin counts must be positive");
        }
        if (values.length != hBins * sBins * vBins) {
            throw new IllegalArgumentException("Expected " + hBins * sBins * vBins
                    + " values, got " + values.length);
        }
        this.hBins = hBins;
        this.sBins = sBins;
        this.vBins = vBins;
        this.values = values.clone();
    }

    public int hBins() {
        return hBins;
    }

    public int sBins() {
        return sBins;
    }

    public int vBins() {
        return vBins;
    }

    public int size() {
        return values.length;
    }

    public String type() {
        return typeName(hBins, sBins, vBins);
    }

    /** @return a copy of the normalised bin values. */
    public float[] values() {
        return values.clone();
    }

    float valueAt(int i) {
        return values[i];
    }

    boolean sameLayoutAs(HsvHistogram other) {
        return hBins == other.hBins && sBins == other.sBins && vBins == other.vBins;
    }

    /**
     * Encodes the vector as a JSON array of floats, e.g. {@code [0.125000000,0.00000000,...]}.
     * Uses 9 significant digits, enough for every float to survive a store/reload round trip
     * unchanged.
     */
    public String toJsonArray() {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float v : values) {
            joiner.add(String.format(Locale.ROOT, "%.9g", v));
        }
        return joiner.toString();
    }

    /** Decodes a vector produced by {@link #toJsonArray()} for the given bin layout. */
    public static HsvHistogram fromJsonArray(int hBins, int sBins, int vBins, String json) {
        String body = json.trim();
        if (!body.startsWith("[") || !body.endsWith("]")) {
            throw new IllegalArgumentException("Not a JSON array: " + json);
        }
        body = body.substring(1, body.length() - 1).trim();
        String[] parts = body.isEmpty() ? new String[0] : body.split(",");
        float[] values = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Float.parseFloat(parts[i].trim());
        }
        return new HsvHistogram(hBins, sBins, vBins, values);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HsvHistogram other && sameLayoutAs(other)
                && Arrays.equals(values, other.values);
    }

    @Override
    public int hashCode() {
        return 31 * type().hashCode() + Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return type() + Arrays.toString(values);
    }
}
