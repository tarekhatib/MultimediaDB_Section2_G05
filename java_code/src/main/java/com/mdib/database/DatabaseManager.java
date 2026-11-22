
package com.mdib.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private final Path databasePath;

    public DatabaseManager() {
        this(Path.of("data", "mdib.db"));
    }

    public DatabaseManager(Path databasePath) {
        this.databasePath = databasePath;
    }

    public Connection getConnection() throws SQLException {
        try {
            Path absolutePath = databasePath.toAbsolutePath();
            Files.createDirectories(absolutePath.getParent());

            Connection connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + absolutePath
            );

            try (var statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            }

            return connection;

        } catch (java.io.IOException e) {
            throw new SQLException(
                    "Could not create database directory.", e
            );
        }
    }

    public Path getDatabasePath() {
        return databasePath;
    }
}
