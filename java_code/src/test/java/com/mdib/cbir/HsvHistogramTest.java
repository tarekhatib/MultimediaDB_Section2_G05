package com.mdib.cbir;

import com.mdib.processing.OpenCvSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HsvHistogramTest {

    private static final double EPS = 1e-6;
    private final HsvHistogramExtractor extractor = new HsvHistogramExtractor();

    /** Native matrices created by a test; all released after each test. */
    private final List<Mat> nativeMats = new ArrayList<>();

    @BeforeAll
    static void loadOpenCv() {
        OpenCvSupport.ensureLoaded();
    }

    @AfterEach
    void releaseNativeMats() {
        nativeMats.forEach(Mat::release);
        nativeMats.clear();
    }

    private <T extends Mat> T track(T mat) {
        nativeMats.add(mat);
        return mat;
    }

    /** Solid BGR image. */
    private Mat solid(double b, double g, double r) {
        return track(new Mat(32, 32, CvType.CV_8UC3, new Scalar(b, g, r)));
    }

    /** Left half one colour, right half another. */
    private Mat split(Scalar left, Scalar right) {
        Mat m = track(new Mat(32, 32, CvType.CV_8UC3, left));
        Mat rightHalf = track(m.colRange(16, 32));
        rightHalf.setTo(right);
        return m;
    }

    private static double sum(float[] v) {
        double s = 0;
        for (float x : v) {
            s += x;
        }
        return s;
    }

    @Test
    void openCvLoads() {
        assertTrue(OpenCvSupport.version().startsWith("4."));
    }

    @Test
    void defaultLayoutIs128BinsAndNormalised() {
        HsvHistogram h = extractor.extract(solid(30, 200, 90));
        assertEquals("HSV_HIST_8x4x4", h.type());
        assertEquals(128, h.size());
        assertEquals(1.0, sum(h.values()), EPS);
    }

    @Test
    void solidColourFallsInExactlyOneBin() {
        // Pure red: H=0, S=255, V=255 -> bin (0, 3, 3) = 0*16 + 3*4 + 3 = 15
        float[] v = extractor.extract(solid(0, 0, 255)).values();
        assertEquals(1.0f, v[15], 1e-6f);
        assertEquals(1.0, sum(v), EPS);
    }

    @Test
    void identicalImagesHaveIntersectionOneAndChiSquareZero() {
        HsvHistogram a = extractor.extract(solid(10, 120, 240));
        HsvHistogram b = extractor.extract(solid(10, 120, 240));
        assertEquals(1.0, HistogramSimilarity.intersection(a, b), EPS);
        assertEquals(0.0, HistogramSimilarity.chiSquare(a, b), EPS);
    }

    @Test
    void disjointColoursHaveIntersectionZeroAndChiSquareOne() {
        HsvHistogram red = extractor.extract(solid(0, 0, 255));
        HsvHistogram blue = extractor.extract(solid(255, 0, 0));
        assertEquals(0.0, HistogramSimilarity.intersection(red, blue), EPS);
        assertEquals(1.0, HistogramSimilarity.chiSquare(red, blue), EPS);
    }

    @Test
    void halfOverlapGivesHalfIntersection() {
        Scalar red = new Scalar(0, 0, 255);
        HsvHistogram redOnly = extractor.extract(solid(0, 0, 255));
        HsvHistogram redBlue = extractor.extract(split(red, new Scalar(255, 0, 0)));
        assertEquals(0.5, HistogramSimilarity.intersection(redOnly, redBlue), EPS);
    }

    @Test
    void retrieverRanksMostSimilarFirst() {
        Scalar red = new Scalar(0, 0, 255);
        HsvHistogram query = extractor.extract(solid(0, 0, 255));
        Map<Long, HsvHistogram> collection = Map.of(
                1L, extractor.extract(solid(255, 0, 0)),                  // blue: 0.0
                2L, extractor.extract(split(red, new Scalar(255, 0, 0))), // half red: 0.5
                3L, extractor.extract(solid(0, 0, 255)));                 // red: 1.0

        List<HistogramRetriever.Match> ranked = HistogramRetriever.rank(query, collection, 10);
        assertEquals(List.of(3L, 2L, 1L), ranked.stream().map(HistogramRetriever.Match::imageId).toList());
        assertEquals(2, HistogramRetriever.rank(query, collection, 2).size());
    }

    @Test
    void jsonRoundTripPreservesVectorExactly() {
        // A three-colour image gives non-trivial fractions (e.g. 1/3) that need full float precision.
        Mat img = track(new Mat(30, 30, CvType.CV_8UC3, new Scalar(0, 0, 255)));
        track(img.colRange(10, 20)).setTo(new Scalar(40, 180, 60));
        track(img.colRange(20, 30)).setTo(new Scalar(200, 90, 10));

        HsvHistogram h = extractor.extract(img);
        HsvHistogram back = HsvHistogram.fromJsonArray(8, 4, 4, h.toJsonArray());
        assertArrayEquals(h.values(), back.values(), 0f);
        assertEquals(h, back);
    }

    @Test
    void readsImageFromDiskIncludingNonAsciiPath(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("صورة_é.png");
        // Write via imencode + Files so the non-ASCII path is handled by Java, not OpenCV.
        MatOfByte buf = track(new MatOfByte());
        Imgcodecs.imencode(".png", solid(0, 0, 255), buf);
        Files.write(file, buf.toArray());

        float[] v = extractor.extract(file).values();
        assertEquals(1.0f, v[15], 1e-6f);
    }

    @Test
    void rejectsMismatchedLayoutsAndBadInput() {
        HsvHistogram a = new HsvHistogramExtractor(8, 4, 4).extract(solid(0, 0, 255));
        HsvHistogram b = new HsvHistogramExtractor(4, 4, 4).extract(solid(0, 0, 255));
        assertThrows(IllegalArgumentException.class, () -> HistogramSimilarity.intersection(a, b));
        Mat grey = track(new Mat(8, 8, CvType.CV_8UC1));
        assertThrows(IllegalArgumentException.class, () -> extractor.extract(grey));
    }
}
