package com.github.bzalyaliev.regulatorfx.controller;


import com.github.bzalyaliev.regulator.RegulatorImpl;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.github.bzalyaliev.regulator.Regulator;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;

public class MainController {
    @FXML
    private Button getDataButton;

    @FXML
    private ListView<Double> dataListView;

    @FXML
    private TextField temperatureField;

    @FXML
    private Button setTemperatureButton;

    private Regulator regulator = RegulatorImpl.getInstance();


    @FXML
    private void getData(ActionEvent event) {
        dataListView.getItems().clear(); // Очищаем список перед добавлением новых данных

        Task<List<Double>> getDataTask = new Task<>() {
            @Override
            protected List<Double> call() throws Exception {
                String url = "http://localhost:8080/regulator/all";
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = reader.readLine()) != null) {
                        response.append(inputLine);
                    }

                    reader.close();
                    connection.disconnect();

                    // Здесь преобразуем полученные данные в список Double
                    JSONArray jsonArray = new JSONArray(response.toString());
                    List<Double> dataList = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        dataList.add(jsonArray.getDouble(i));
                    }

                    // Определите, сколько последних значений нужно показать
                    int numToShow = 10 + (int) (Math.random() * 3);

                    // Получите подсписок последних значений
                    int startIndex = Math.max(0, dataList.size() - numToShow);
                    List<Double> lastValues = dataList.subList(startIndex, dataList.size());

                    return lastValues;
                } else {
                    throw new Exception("Failed to retrieve data from web service");
                }
            }
        };

        getDataTask.setOnSucceeded(e -> {
            List<Double> lastValues = getDataTask.getValue();
            dataListView.getItems().addAll(lastValues);
        });

        getDataTask.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Data Retrieval Error");
            alert.setContentText("An error occurred while retrieving data.");
            alert.showAndWait();
        });

        Thread thread = new Thread(getDataTask);
        thread.start();
    }

    @FXML
    private void setTemperature(ActionEvent event) {
        // Получаем значение температуры из поля ввода
        double temperature = Double.parseDouble(temperatureField.getText());

        // Задача для установки температуры на сервере
        Task<Void> setTemperatureTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String setUrl = "http://localhost:8080/regulator/set";
                HttpURLConnection setConnection = (HttpURLConnection) new URL(setUrl).openConnection();
                setConnection.setRequestMethod("POST");
                setConnection.setRequestProperty("Content-Type", "application/json");
                setConnection.setDoOutput(true);

                // Отправляем значение температуры на сервер
                try (OutputStream os = setConnection.getOutputStream()) {
                    byte[] input = String.valueOf(temperature).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int setResponseCode = setConnection.getResponseCode();
                if (setResponseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Failed to set temperature on the regulator");
                }

                setConnection.disconnect();
                return null;
            }
        };

        // После успешной установки температуры выполняем запрос для получения всех значений температуры
        setTemperatureTask.setOnSucceeded(e -> {
            // Задача для получения списка значений температуры
            Task<List<Double>> getAllTemperatureTask = new Task<>() {
                @Override
                protected List<Double> call() throws Exception {
                    String getUrl = "http://localhost:8080/regulator/all";
                    HttpURLConnection getConnection = (HttpURLConnection) new URL(getUrl).openConnection();
                    getConnection.setRequestMethod("GET");

                    int getResponseCode = getConnection.getResponseCode();
                    if (getResponseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(getConnection.getInputStream()));
                        String response = reader.readLine();
                        reader.close();
                        getConnection.disconnect();

                        JSONArray jsonArray = new JSONArray(response);
                        List<Double> temperatureList = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            temperatureList.add(jsonArray.getDouble(i));
                        }

                        return temperatureList;
                    } else {
                        throw new Exception("Failed to retrieve temperature data from the regulator");
                    }
                }
            };

            // После получения списка значений температуры
            getAllTemperatureTask.setOnSucceeded(ev -> {
                List<Double> temperatureList = getAllTemperatureTask.getValue();
                if (!temperatureList.isEmpty()) {
                    // Получаем последнее значение температуры
                    double currentTemperature = temperatureList.get(temperatureList.size() - 1);
                    temperatureField.setText(String.valueOf(currentTemperature));
                    // Показываем окно предупреждения, если необходимо
                    if (currentTemperature > 1000 || currentTemperature < -200) {
                        showWarningWindow();
                    }
                }
            });

            getAllTemperatureTask.setOnFailed(ev -> {
                // Обработка ошибки при получении списка значений температуры с сервера
                System.out.println("Failed to retrieve temperature data from the regulator");
            });
            // Запускаем задачу получения списка значений температуры
            Thread getAllTemperatureThread = new Thread(getAllTemperatureTask);
            getAllTemperatureThread.start();
        });
        // Обработка ошибки при установке температуры на сервере
        setTemperatureTask.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Temperature Setting Error");
            alert.setContentText("An error occurred while setting the temperature.");
            alert.showAndWait();
        });
        // Запускаем задачу установки температуры на сервере
        Thread thread = new Thread(setTemperatureTask);
        thread.start();
    }

    private void showWarningWindow() {
        Stage warningStage = new Stage();
        warningStage.setTitle("Предупреждение");

        Text text = new Text("Значение выходит за пределы допустимого диапазона!");
        text.setFont(Font.font("Arial", FontWeight.NORMAL, 16));

        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().add(text);

        Scene scene = new Scene(vbox, 600, 100);
        warningStage.setScene(scene);

        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(event -> warningStage.hide());
        delay.play();

        warningStage.show();
    }

}