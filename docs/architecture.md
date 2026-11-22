# System Architecture

## Product
A Java desktop application for image ingestion, object detection, adversarial diagnosis, targeted poisoning, defensive restoration and content-based retrieval.

## High-level flow
1. Ingest image and copy it into managed filesystem storage.
2. Run detector and compute CBIR descriptors.
3. Persist image, detection and descriptor records.
4. Diagnose using at least two signals (for example CBIR-vs-detector disagreement plus high-frequency energy or prediction stability under mild blur).
5. If clean: search reference images by target label, select reference and run targeted attack.
6. If suspicious/poisoned: run restoration pipeline and re-evaluate detector.
7. Save every output as a new image linked to its parent; persist parameters, metrics and logs.
8. Browse status, lineage, detections, operations and retrieval results.

## Component ownership
- Java/Tarek: application orchestration, JavaFX, SQLite/JDBC, ProcessBuilder integration.
- Python/Ayoub: YOLO worker, targeted attacks, attack metrics.
- Java/Shokor: OpenCV processing, CBIR descriptors/similarity, restoration.

## Architectural principles
- Java is the substantive core application language.
- Python is a worker process for ML tasks.
- SQLite stores metadata; image binaries live in filesystem storage.
- Use relative paths and JSON contracts.
- Original files are immutable; derived images are separate records.
- Start with ProcessBuilder; avoid premature microservices.
