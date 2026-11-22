package com.mdib.cbir;

import com.mdib.processing.OpenCvSupport;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Computes a quantised, L1-normalised HSV colour histogram from a BGR image.
 *
 * <p>Default quantisation is 8 x 4 x 4 = 128 bins: hue is the most discriminative
 * channel for object colour, so it gets the most bins, while saturation and value
 * are coarser to stay tolerant to lighting changes and small perturbations.
 * OpenCV's 8-bit HSV uses H in [0,180) and S,V in [0,256).
 */
public final class HsvHistogramExtractor {

    public static final int DEFAULT_H_BINS = 8;
    public static final int DEFAULT_S_BINS = 4;
    public static final int DEFAULT_V_BINS = 4;

    private final int hBins;
    private final int sBins;
    private final int vBins;

    public HsvHistogramExtractor() {
        this(DEFAULT_H_BINS, DEFAULT_S_BINS, DEFAULT_V_BINS);
    }

    public HsvHistogramExtractor(int hBins, int sBins, int vBins) {
        if (hBins <= 0 || sBins <= 0 || vBins <= 0) {
            throw new IllegalArgumentException("Bin counts must be positive");
        }
        this.hBins = hBins;
        this.sBins = sBins;
        this.vBins = vBins;
    }

    public String descriptorType() {
        return HsvHistogram.typeName(hBins, sBins, vBins);
    }

    public HsvHistogram extract(Path imagePath) throws IOException {
        Mat bgr = OpenCvSupport.readBgr(imagePath);
        try {
            return extract(bgr);
        } finally {
            bgr.release();
        }
    }

    /** @param bgr an 8-bit 3-channel BGR image (the input is not modified). */
    public HsvHistogram extract(Mat bgr) {
        OpenCvSupport.ensureLoaded();
        if (bgr.empty() || bgr.type() != CvType.CV_8UC3) {
            throw new IllegalArgumentException("Expected a non-empty 8-bit 3-channel BGR image");
        }
        // Every temporary below wraps native memory, so all are released in finally
        // to avoid leaks when processing large image collections.
        Mat hsv = new Mat();
        Mat hist = new Mat();
        Mat noMask = new Mat();
        MatOfInt channels = new MatOfInt(0, 1, 2);
        MatOfInt histSize = new MatOfInt(hBins, sBins, vBins);
        MatOfFloat ranges = new MatOfFloat(0f, 180f, 0f, 256f, 0f, 256f);
        try {
            Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV);
            Imgproc.calcHist(List.of(hsv), channels, noMask, hist, histSize, ranges);

            // calcHist returns a 3-D CV_32F matrix laid out h-major, then s, then v.
            float[] values = new float[hBins * sBins * vBins];
            hist.get(new int[] {0, 0, 0}, values);

            double total = Core.sumElems(hist).val[0];
            if (total > 0) {
                for (int i = 0; i < values.length; i++) {
                    values[i] = (float) (values[i] / total);
                }
            }
            return new HsvHistogram(hBins, sBins, vBins, values);
        } finally {
            hsv.release();
            hist.release();
            noMask.release();
            channels.release();
            histSize.release();
            ranges.release();
        }
    }
}
