package com.mdib.cbir;

import com.mdib.processing.OpenCvSupport;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Standalone prototype: proves OpenCV loads, extracts HSV histograms and ranks images.
 *
 * <pre>
 * mvnw exec:java -Dexec.mainClass=com.mdib.cbir.HistogramDemo -Dexec.args="query.jpg a.jpg b.jpg"
 * </pre>
 *
 * The first argument is the query; the rest form the collection.
 */
public final class HistogramDemo {

    private HistogramDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("OpenCV " + OpenCvSupport.version() + " loaded");
        if (args.length < 2) {
            System.out.println("Usage: HistogramDemo <query-image> <image> [<image> ...]");
            return;
        }

        HsvHistogramExtractor extractor = new HsvHistogramExtractor();
        HsvHistogram query = extractor.extract(Path.of(args[0]));
        System.out.printf("Query: %s  (%s, %d bins)%n", args[0], query.type(), query.size());

        Map<Long, HsvHistogram> collection = new LinkedHashMap<>();
        for (int i = 1; i < args.length; i++) {
            collection.put((long) i, extractor.extract(Path.of(args[i])));
        }

        List<HistogramRetriever.Match> ranked =
                HistogramRetriever.rank(query, collection, Integer.MAX_VALUE);
        System.out.println("Rank  Intersection  Chi-square  Image");
        int rank = 1;
        for (HistogramRetriever.Match m : ranked) {
            double chi = HistogramSimilarity.chiSquare(query, collection.get(m.imageId()));
            System.out.printf(Locale.ROOT, "%4d  %12.4f  %10.4f  %s%n",
                    rank++, m.similarity(), chi, args[(int) m.imageId()]);
        }
    }
}
