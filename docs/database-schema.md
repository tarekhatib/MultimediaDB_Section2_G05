# Database Schema Proposal

SQLite stores metadata and relationships. Image binaries are stored on disk; `images.file_path` stores a relative path.

## Tables

### images
- id INTEGER PRIMARY KEY
- parent_id INTEGER NULL REFERENCES images(id)
- file_path TEXT NOT NULL UNIQUE
- image_type TEXT NOT NULL CHECK(image_type IN ('original','poisoned','restored'))
- status TEXT NOT NULL
- source_name TEXT
- checksum TEXT
- created_at TEXT NOT NULL

### detections
- id INTEGER PRIMARY KEY
- image_id INTEGER NOT NULL REFERENCES images(id)
- model_name TEXT NOT NULL
- detected_label TEXT NOT NULL
- confidence REAL NOT NULL
- bounding_boxes_json TEXT
- created_at TEXT NOT NULL

### attacks
- id INTEGER PRIMARY KEY
- source_image_id INTEGER NOT NULL REFERENCES images(id)
- output_image_id INTEGER NOT NULL REFERENCES images(id)
- reference_image_id INTEGER NULL REFERENCES images(id)
- attack_type TEXT NOT NULL
- target_label TEXT NOT NULL
- epsilon REAL
- seed INTEGER
- parameters_json TEXT
- created_at TEXT NOT NULL

### filter_runs
- id INTEGER PRIMARY KEY
- source_image_id INTEGER NOT NULL REFERENCES images(id)
- output_image_id INTEGER NULL REFERENCES images(id)
- recovered_label TEXT
- recovery_success INTEGER
- metrics_json TEXT
- created_at TEXT NOT NULL

### filter_operations
- id INTEGER PRIMARY KEY
- filter_run_id INTEGER NOT NULL REFERENCES filter_runs(id)
- sequence_number INTEGER NOT NULL
- operation_name TEXT NOT NULL
- parameters_json TEXT

### descriptors
- id INTEGER PRIMARY KEY
- image_id INTEGER NOT NULL REFERENCES images(id)
- descriptor_type TEXT NOT NULL
- configuration_json TEXT
- feature_vector TEXT NOT NULL
- created_at TEXT NOT NULL

## Decisions
- `images.parent_id` is a self-reference that preserves original → poisoned → restored lineage.
- Detections are separate because one image may be evaluated multiple times or by different model versions.
- Attacks and filter runs are event records; their parameters do not belong in the image row.
- Filter operations are separate to preserve order and allow multiple restoration attempts.
- Descriptors are separate to support multiple descriptor types/configurations.
- Use transactions for workflows that create an image and multiple related records.
- Enable `PRAGMA foreign_keys = ON` for each SQLite connection.
- Index `images(status)`, `images(parent_id)`, `detections(detected_label)`, and relevant foreign keys.
- Use PreparedStatement. Store bounding boxes/configurations as JSON initially; document descriptor vector encoding.
