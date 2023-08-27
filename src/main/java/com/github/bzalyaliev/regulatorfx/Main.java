package com.github.bzalyaliev.regulatorfx;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("view/MainView.fxml"));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("view/styles.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Регулятор температуры");
        primaryStage.show();
    }

    public static void main(String[] args) {

        launch(args);
    }
}