
package com.financecopilot;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("Finance Copilot");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #60a5fa;");

        Label subtitle = new Label("AI-Powered Financial Analysis & Decision Support");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");

        Label status = new Label("Your financial workspace is ready to begin.");
        status.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        VBox root = new VBox(18, title, subtitle, status);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #0f172a;");

        Scene scene = new Scene(root, 900, 600);

        stage.setTitle("Finance Copilot");
        stage.setMinWidth(700);
        stage.setMinHeight(450);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
