# CBIR descriptor: HSV colour histogram (prototype)

Owner: Shokor · Status: prototype, not yet wired into the UI or database.

## What it is

A global colour histogram computed in HSV space, used for query-by-image retrieval
and as one signal for the "visual cluster vs. detector label" poisoning diagnosis.

| Item | Value |
|---|---|
| Colour space | HSV (OpenCV 8-bit: H ∈ [0,180), S,V ∈ [0,256)) |
| Quantisation | **8 × 4 × 4 = 128 bins** (H × S × V), uniform |
| Normalisation | L1: bins sum to 1 |
| Type tag | `HSV_HIST_8x4x4` (format: `HSV_HIST_<h>x<s>x<v>`) |
| Bin index | `h * (sBins*vBins) + s * vBins + v` (hue-major) |
| Storage encoding | JSON array of 128 floats, e.g. `[0.125,0.0,…]` |

**Why HSV and these bins:** hue separates object colours better than raw RGB and is
less affected by brightness. Hue gets the most bins (8); saturation and value are coarse
(4 each) so lighting changes and tiny adversarial perturbations (ε ≤ 8/255) barely move
pixels across bins — the descriptor should stay stable when the detector is fooled.

## Similarity metric

- **Primary: histogram intersection** `Σ min(aᵢ, bᵢ)` ∈ [0, 1], 1 = identical.
  Chosen because it is bounded, parameter-free, and robust to background clutter and
  partial occlusion (only shared colour mass counts) — Swain & Ballard, 1991.
- Secondary (for comparison in the report): **chi-square** `½ Σ (aᵢ−bᵢ)²/(aᵢ+bᵢ)` ∈ [0, 1], 0 = identical.

## Java API (`com.mdib.cbir`)

```java
HsvHistogram h = new HsvHistogramExtractor().extract(Path.of("car.jpg")); // or extract(Mat bgr)
double sim     = HistogramSimilarity.intersection(h, other);
List<HistogramRetriever.Match> top5 = HistogramRetriever.rank(h, Map.of(id, desc, ...), 5);
String json    = h.toJsonArray();                                        // for the descriptors table
HsvHistogram back = HsvHistogram.fromJsonArray(8, 4, 4, json);
```

`HistogramRetriever` works on caller-supplied IDs (e.g. `images.id`), so it has no
dependency on the database layer. Mapping to the `descriptors` table in
`docs/database-schema.md`:

| Column | Value |
|---|---|
| `descriptor_type` | `HSV_HIST_8x4x4` (`h.type()`) |
| `configuration_json` | `{"colorSpace":"HSV","hBins":8,"sBins":4,"vBins":4,"normalization":"L1","binOrder":"h-major"}` |
| `feature_vector` | `h.toJsonArray()` — JSON array of 128 floats |

To load: `HsvHistogram.fromJsonArray(hBins, sBins, vBins, feature_vector)` with the bin
counts from `configuration_json`.

## OpenCV setup

Dependency `org.openpnp:opencv:4.9.0-0` bundles the native libraries for Windows,
macOS (Intel + Apple Silicon) and Linux, so no manual install is needed. Call
`OpenCvSupport.ensureLoaded()` once (the extractor does it for you). Images are read with
`OpenCvSupport.readBgr`, which works with non-ASCII paths on Windows (plain `imread` does not).

## Verify

```
java_code/mvnw -f java_code/pom.xml test
java_code/mvnw -f java_code/pom.xml compile exec:java -Dexec.mainClass=com.mdib.cbir.HistogramDemo -Dexec.args="query.jpg a.jpg b.jpg"
```

The demo prints the OpenCV version, then ranks the other images by intersection with the query.

## Limitations / next steps

- Global histograms ignore spatial layout: a red car and a red wall can match. Adding an
  MPEG-7 Color Layout or Edge Histogram descriptor would address this.
- Not yet integrated with the SQLite `descriptors` table or the JavaFX UI (pending team API agreement).
- Restoration filters (Gaussian / median / Sobel / Laplacian / morphology) are the next module.
