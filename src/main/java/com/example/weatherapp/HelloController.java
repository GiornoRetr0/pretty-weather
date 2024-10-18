package com.example.weatherapp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.scene.image.ImageView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HelloController {

    private static final Logger LOGGER = Logger.getLogger(HelloController.class.getName());

    private static final String API_KEY = System.getenv("OPENWEATHER_API_KEY");
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/weather";
    private static final String ICON_BASE_URL = "http://openweathermap.org/img/wn/";

    @FXML
    private Label weatherLabel;
    @FXML
    private TextField cityInput;
    @FXML
    private Label temperatureLabel;
    @FXML
    private Label humidityLabel;
    @FXML
    private Label windSpeedLabel;
    @FXML
    private Label conditionLabel;
    @FXML
    private ImageView temperatureIcon;
    private final OkHttpClient httpClient = new OkHttpClient();

    @FXML
    protected void onGetWeatherButtonClick() {
        String city = cityInput.getText();

        if (API_KEY == null || API_KEY.isEmpty()) {
            weatherLabel.setText("API key is missing. Please set the API key.");
            return;
        }

        if (city == null || city.trim().isEmpty()) {
            weatherLabel.setText("Please enter a city name.");
            return;
        }

        try {
            JsonObject weatherData = fetchWeatherData(city);
            if (weatherData != null) {
                updateWeatherTiles(weatherData);
            } else {
                weatherLabel.setText("Error fetching weather data.");
            }
        } catch (IOException e) {
            weatherLabel.setText("Failed to fetch weather data.");
            LOGGER.log(Level.SEVERE, "Error fetching weather data", e);
        }
    }


    /**
     * Fetch weather data from OpenWeatherMap API.
     *
     * @param city The city name.
     * @return A JsonObject representing the weather data, or null in case of an error.
     * @throws IOException If there is a network issue.
     */
    private JsonObject fetchWeatherData(String city) throws IOException {
        String url = buildWeatherApiUrl(city);
        Request request = new Request.Builder().url(url).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();
                return JsonParser.parseString(jsonResponse).getAsJsonObject();
            } else {
                LOGGER.log(Level.WARNING, "Failed to fetch weather data. HTTP Response Code: {0}", response.code());
                return null;
            }
        }
    }


    /**
     * Update the weather tiles with data from the API response.
     *
     * @param weatherData The JsonObject containing weather data.
     */
    private void updateWeatherTiles(JsonObject weatherData) {
        JsonObject main = weatherData.getAsJsonObject("main");
        double temperature = main.get("temp").getAsDouble();
        int humidity = main.get("humidity").getAsInt();

        JsonObject wind = weatherData.getAsJsonObject("wind");
        double windSpeed = wind.get("speed").getAsDouble();

        String description = weatherData.getAsJsonArray("weather").get(0).getAsJsonObject().get("description").getAsString();
        String iconCode = weatherData.getAsJsonArray("weather").get(0).getAsJsonObject().get("icon").getAsString();

        // Update the labels with the data
        temperatureLabel.setText(String.format("%.1f°C", temperature));
        humidityLabel.setText(humidity + "%");
        windSpeedLabel.setText(String.format("%.1f m/s", windSpeed));
        conditionLabel.setText(description);

        // Update the weather icon
        updateWeatherIcon(iconCode);
    }


    /**
     * Update the weather icon based on the icon code from the API.
     *
     * @param iconCode The icon code from the weather data.
     */
    private void updateWeatherIcon(String iconCode) {
        String iconUrl = ICON_BASE_URL + iconCode + "@2x.png";
        Image iconImage = new Image(iconUrl);
        temperatureIcon.setImage(iconImage); // Update ImageView with the weather icon
    }


    /**
     * Build the API URL for fetching weather data.
     *
     * @param city The city name.
     * @return The fully formed API URL.
     */
    private String buildWeatherApiUrl(String city) {
        return BASE_URL + "?q=" + city + "&appid=" + API_KEY + "&units=metric";
    }
}
