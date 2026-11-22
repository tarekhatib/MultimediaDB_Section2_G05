
package com.mdib.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    private final DatabaseManager databaseManager;

    public DatabaseInitializer(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void initialize() throws SQLException {

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("""
                CREATE TABLE IF NOT EXISTS images (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    parent_id INTEGER,
                    file_path TEXT NOT NULL UNIQUE,
                    image_type TEXT NOT NULL
                        CHECK (image_type IN ('original', 'poisoned', 'restored')),
                    source_name TEXT,
                    checksum TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                    FOREIGN KEY (parent_id)
                        REFERENCES images(id)
                        ON DELETE RESTRICT
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS detections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    image_id INTEGER NOT NULL,
                    model_name TEXT NOT NULL,
                    detected_label TEXT NOT NULL,
                    confidence REAL NOT NULL,
                    bounding_boxes_json TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                    FOREIGN KEY (image_id)
                        REFERENCES images(id)
                        ON DELETE CASCADE
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS attacks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_image_id INTEGER NOT NULL,
                    output_image_id INTEGER NOT NULL,
                    reference_image_id INTEGER,
                    attack_type TEXT NOT NULL,
                    target_label TEXT NOT NULL,
                    epsilon REAL NOT NULL,
                    seed INTEGER,
                    parameters_json TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                    FOREIGN KEY (source_image_id)
                        REFERENCES images(id)
                        ON DELETE RESTRICT,

                    FOREIGN KEY (output_image_id)
                        REFERENCES images(id)
                        ON DELETE RESTRICT,

                    FOREIGN KEY (reference_image_id)
                        REFERENCES images(id)
                        ON DELETE SET NULL
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS filter_runs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_image_id INTEGER NOT NULL,
                    output_image_id INTEGER,
                    recovered_label TEXT,
                    recovery_success INTEGER NOT NULL DEFAULT 0
                        CHECK (recovery_success IN (0, 1)),
                    metrics_json TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                    FOREIGN KEY (source_image_id)
                        REFERENCES images(id)
                        ON DELETE RESTRICT,

                    FOREIGN KEY (output_image_id)
                        REFERENCES images(id)
                        ON DELETE RESTRICT
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS filter_operations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    filter_run_id INTEGER NOT NULL,
                    sequence_number INTEGER NOT NULL,
                    operation_name TEXT NOT NULL,
                    parameters_json TEXT,

                    UNIQUE (filter_run_id, sequence_number),

                    FOREIGN KEY (filter_run_id)
                        REFERENCES filter_runs(id)
                        ON DELETE CASCADE
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS descriptors (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    image_id INTEGER NOT NULL,
                    descriptor_type TEXT NOT NULL,
                    configuration_json TEXT,
                    feature_vector TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                    FOREIGN KEY (image_id)
                        REFERENCES images(id)
                        ON DELETE CASCADE
                )
            """);

            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_images_parent
                ON images(parent_id)
            """);

            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_detections_image
                ON detections(image_id)
            """);

            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_attacks_source
                ON attacks(source_image_id)
            """);

            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_filter_runs_source
                ON filter_runs(source_image_id)
            """);

            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_descriptors_image
                ON descriptors(image_id)
            """);
        }
    }
}
