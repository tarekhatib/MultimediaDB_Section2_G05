
package com.mdib.app;

import com.mdib.database.DatabaseInitializer;
import com.mdib.database.DatabaseManager;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        // Initialize the database before starting the application
        try {
            DatabaseManager databaseManager = new DatabaseManager();

            DatabaseInitializer databaseInitializer =
                    new DatabaseInitializer(databaseManager);

            databaseInitializer.initialize();

            System.out.println("Database initialized successfully!");

        } catch (SQLException e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Could not initialize the database");
            alert.setContentText(e.getMessage());
            alert.showAndWait();

            return;
        }

        // Main application window
        Label title = new Label("MDIB");
        title.setStyle(
                "-fx-font-size: 32px;" +
                        "-fx-font-weight: bold;"
        );

        Label subtitle = new Label(
                "Adversarial Image Poisoning, " +
                        "Defensive Restoration & Multimedia Indexing"
        );

        VBox root = new VBox(15, title, subtitle);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 800, 500);

        stage.setTitle("MDIB - Multimedia Database");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
