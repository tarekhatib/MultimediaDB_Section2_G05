
package com.mdip.app;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        Label title = new Label("MDIP");
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

        stage.setTitle("MDIP - Multimedia Database");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
