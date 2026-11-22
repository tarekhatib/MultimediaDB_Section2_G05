# SKILL.md — MDIP Group 05 Team Working Context

## How to use this file

This file is shared with the AI assistants used by Tarek, Ayoub and Shokor.

**First action:** identify the human owner of this AI session:
- Tarek
- Ayoub
- Shokor

If the owner is not explicit in the conversation, ask: "Are you Tarek, Ayoub or Shokor?" Do not guess.

Then read the ownership matrix below and work ONLY on that person's assigned tasks. You may explain shared architecture and interfaces when necessary, but do not take over another teammate's implementation area. If asked to do work outside the owner's scope, explain who owns it and offer to help define the interface or review their contribution.

Do not invent completed work. Inspect the current repository/code before proposing edits. Preserve existing conventions and avoid overwriting teammates' work.

---

## Project identity

- Repository: `MultimediaDB_Section2_G05`
- Course: Multimedia Database & Image Processing, 2026/2027
- Team: Tarek, Ayoub, Shokor
- Project title: **Adversarial Image Poisoning, Defensive Restoration & Multimedia Indexing**
- Core application: a Java desktop application that ingests images, detects objects, indexes visual descriptors, diagnoses possible adversarial images, offers attack or restoration workflows, and stores searchable image history in SQLite.
- Java must be a substantive core language. Python is used for machine-learning functionality. The team is using Java 21, Maven Wrapper, Python 3.11, SQLite and JavaFX.
- Target OSes: Tarek uses macOS; Ayoub and Shokor use Windows. All shared code must be cross-platform.

## Simple product explanation

A user imports an image. The system runs object detection and computes visual descriptors. It compares these signals to decide whether the image may be adversarially poisoned. If it appears clean, the user can choose a target label, select a reference image and generate a controlled targeted attack. If it appears poisoned, the user can run a restoration pipeline and check whether the original prediction returns. The application saves every version and all relevant metadata, then supports browsing, lineage inspection and content-based image retrieval.

## Required project outcomes (from instructor specification)

1. **Attack:** baseline detector; at least one targeted attack (ideally compare two); bounded perturbation, suggested L-infinity epsilon around 2–8/255; target prediction above a defined confidence threshold; report PSNR and SSIM; save clean/poisoned/difference images and comparison results.
2. **Restoration:** Gaussian and median filtering, optional third filter; Sobel and Laplacian edge detection; morphological opening/closing; re-run detector; measure recovery rate and SSIM trade-off; save ordered filter logs.
3. **Database, CBIR and diagnosis:** SQLite plus filesystem; preserve original/poisoned/restored lineage; store attack metadata, detections/bounding boxes, filter logs and descriptors; query by image and text label; use a documented similarity metric; diagnosis must combine at least two signals and explain the rule.
4. **Tool workflow:** ingest → diagnose → if poisoned offer restoration; if clean allow target label, search reference images and attack; browse records, status, lineage and logs.
5. **Deliverables:** source repository and README/dependencies/run instructions; report; vertical 45-second video; 3-minute PowerPoint; 2-minute live demo.
6. **Ethics:** use own images or public datasets only; do not attack production systems or third-party deployed models.

## Architecture principles

- Java owns the main application, user workflow, persistence and integration.
- Python owns YOLO inference and gradient-based attack execution.
- Java image-processing/CBIR modules own classical filtering, descriptors and retrieval.
- Start with `ProcessBuilder` to launch Python scripts. Do not add microservices, Docker or HTTP APIs unless there is a concrete reason.
- Use JSON as the Java/Python contract. Python stdout must contain machine-readable JSON only; diagnostics go to stderr. Non-zero exit code means failure.
- Store image files on disk and metadata in SQLite. Use relative paths, never developer-specific absolute paths.
- Keep original images immutable. Each generated image is a new record linked to its parent.
- Use fixed seeds and log model versions, parameters and timestamps.
- Use feature branches and pull requests. Do not commit directly to `main` for feature work.
- Never commit `.venv`, model weights, large datasets, generated bulk images, secrets or machine-specific IDE state.

## Database proposal

### `images`
One row per image version:
- `id` primary key
- `parent_id` nullable self-reference to `images.id`
- `file_path` relative path, unique
- `image_type`: original / poisoned / restored
- `status`: clean / poisoned / restored / flagged (team may refine)
- `created_at`, `source_name`, optional checksum

### `detections`
One or more detector runs per image:
- `id` primary key
- `image_id` foreign key
- `model_name`, `detected_label`, `confidence`
- `bounding_boxes_json`, `created_at`

### `attacks`
One row per attack event:
- `id` primary key
- `source_image_id`, `output_image_id`, optional `reference_image_id` foreign keys to images
- `attack_type`, `target_label`, `epsilon`, `seed`, `parameters_json`, `created_at`

### `filter_runs`
One row per restoration attempt:
- `id` primary key
- `source_image_id`, optional `output_image_id`
- `recovered_label`, `recovery_success`, `metrics_json`, `created_at`

### `filter_operations`
Ordered operations within a restoration:
- `id` primary key
- `filter_run_id` foreign key
- `sequence_number`, `operation_name`, `parameters_json`

### `descriptors`
CBIR features:
- `id` primary key
- `image_id` foreign key
- `descriptor_type`, bins/configuration, `feature_vector`, `created_at`

Use foreign keys, indexes for common filters/searches, transactions for multi-record workflows, and PreparedStatement. Enable SQLite foreign-key enforcement. Decide whether bounding boxes and feature vectors are JSON or binary and document the encoding.

## Team ownership — enforce this boundary

### Tarek — Java core / architecture / database / JavaFX / integration
Work assigned to Tarek's AI:
- Java application architecture and package structure.
- SQLite schema, JDBC repositories/DAO layer, migrations or schema initialization.
- Java application orchestration and workflow coordination.
- JavaFX shell, screens, navigation and controllers.
- Java `ProcessBuilder` integration adapter and JSON parsing/validation.
- Cross-module integration tests, build/run instructions and merge coordination.
- Review interfaces with Ayoub and Shokor; do not implement their ML or CBIR algorithms as a substitute for their work.

Not assigned to Tarek's AI:
- Implementing YOLO/FGSM/PGD internals (Ayoub owns these).
- Implementing the main OpenCV/CBIR/restoration algorithms (Shokor owns these).

### Ayoub — Python ML / YOLO / adversarial attacks
Work assigned to Ayoub's AI:
- Python 3.11 environment and `requirements.txt`.
- Python worker CLI and stable JSON input/output contract.
- YOLO baseline detection, label/confidence/bounding-box output.
- Targeted attack implementation (FGSM/PGD or justified method), target/reference handling as agreed.
- Perturbation bound enforcement, fixed seeds and attack metadata.
- PSNR/SSIM and attack success evaluation.
- Python tests and usage documentation.

Not assigned to Ayoub's AI:
- JavaFX screens or Java database implementation.
- Java OpenCV/CBIR/restoration implementation (Shokor owns these).

### Shokor — Java image processing / CBIR / restoration
Work assigned to Shokor's AI:
- Java OpenCV integration for classical image processing.
- Color histogram descriptor (RGB or HSV; team agrees bin counts).
- Descriptor normalization and similarity metric (e.g. histogram intersection or chi-square).
- Query-by-image retrieval and support for label-based retrieval through database queries.
- Gaussian/median filtering and optional third filter.
- Sobel/Laplacian edge detection and morphological opening/closing.
- Restoration parameter sweeps, recovery evaluation hooks and ordered filter operation output.
- Tests and module documentation.

Not assigned to Shokor's AI:
- Python YOLO/attack internals (Ayoub owns these).
- Central JavaFX/application orchestration/database ownership (Tarek owns these).

## Shared interfaces to agree before implementation

1. **Python worker request/response:** command, input image path, target label, reference image path, epsilon, seed, model configuration; JSON result with success, output paths, detections, metrics and error.
2. **Java processing API:** input path + processing configuration → output path + ordered operation log + metrics.
3. **CBIR API:** image path → descriptor; query descriptor + collection → ranked image IDs/similarity scores.
4. **Database API:** create image, attach detection, record attack, record filter run/operations, save descriptor, retrieve lineage/status.
5. Do not independently rename shared fields or change table structure without a PR and team agreement.

## Suggested repository layout

```text
MultimediaDB_Section2_G05/
├── docs/
│   ├── architecture.md
│   ├── database-schema.md
│   ├── python-java-integration.md
│   └── development-guidelines.md
├── java_code/
│   ├── pom.xml
│   └── src/main/java/com/mdip/
│       ├── app/
│       ├── database/
│       ├── detection/
│       ├── cbir/
│       ├── processing/
│       ├── restoration/
│       ├── integration/
│       └── ui/
├── python_code/
│   ├── requirements.txt
│   ├── worker.py
│   ├── detector.py
│   └── attack.py
├── images/
├── report_assets/
├── report.pdf
├── 45sec_video.mp4
├── README.md
└── SKILL.md
```

## How this AI should behave

1. Identify whether the owner is Tarek, Ayoub or Shokor.
2. Work only on that owner's assigned tasks.
3. Before editing, inspect relevant files, current branch and git status. Do not overwrite uncommitted work.
4. Make small, reviewable changes. Explain which files changed and why.
5. Add tests or a reproducible verification command for implementation changes.
6. Preserve cross-platform compatibility (macOS + Windows).
7. Do not add dependencies without explaining why and checking compatibility with Java 21/Python 3.11.
8. Do not invent successful test results. If unable to run tests, say so.
9. If a request crosses ownership boundaries, define the interface and ask the responsible teammate to implement the other side.
10. If the user asks for conceptual explanation, explain simply, but keep implementation tasks within the owner's scope.

## First task by owner

- **Tarek:** finalize and implement the initial SQLite schema/repository layer and document the Java/Python integration contract. Coordinate table/API agreement before merging.
- **Ayoub:** build a minimal Python worker that loads one image, runs baseline YOLO inference and returns valid JSON. Do not begin attack implementation until baseline output and interface are agreed.
- **Shokor:** prove Java OpenCV works on Windows and implement a small standalone image-processing/HSV-histogram prototype with a documented descriptor format. Do not integrate into the main UI until API is agreed.

## Definition of done for any task

A task is done only when:
- The code is committed on a feature branch and opened as a PR.
- The module has a clear public interface and short documentation.
- Relevant tests or a manual verification command are provided.
- No machine-specific paths, local environments, model weights or large datasets are committed.
- The owner can explain what was implemented and its limitations.
