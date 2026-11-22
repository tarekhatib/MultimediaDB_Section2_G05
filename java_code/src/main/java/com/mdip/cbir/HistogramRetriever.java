package com.mdip.cbir;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Query-by-image retrieval over an in-memory collection of descriptors.
 *
 * <p>Keys are caller-chosen IDs (e.g. {@code images.id} from the database), so this
 * class has no dependency on the storage layer.
 */
public final class HistogramRetriever {

    /** One ranked hit: the image ID and its intersection similarity to the query. */
    public record Match(long imageId, double similarity) {
    }

    private HistogramRetriever() {
    }

    /**
     * Ranks every descriptor in {@code collection} by histogram intersection with the query.
     *
     * @param limit maximum number of results (use {@code Integer.MAX_VALUE} for all)
     * @return matches sorted by descending similarity, ties broken by ascending ID
     */
    public static List<Match> rank(HsvHistogram query, Map<Long, HsvHistogram> collection, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be >= 0");
        }
        List<Match> matches = new ArrayList<>(collection.size());
        for (Map.Entry<Long, HsvHistogram> entry : collection.entrySet()) {
            matches.add(new Match(entry.getKey(),
                    HistogramSimilarity.intersection(query, entry.getValue())));
        }
        matches.sort(Comparator.comparingDouble(Match::similarity).reversed()
                .thenComparingLong(Match::imageId));
        return matches.size() > limit ? List.copyOf(matches.subList(0, limit)) : List.copyOf(matches);
    }
}
