package com.example.weatherapp;

import com.google.gson.JsonArray;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private VBox forecastContainer;
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

            // Fetch and display 5-day forecast
            fetchAndDisplayForecast(city);

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

    /**
     * Fetch and display the 5-day weather forecast.
     *
     * @param city The city name.
     */
    private void fetchAndDisplayForecast(String city) {
        try {
            // Get lat/lon from Geocoding API
            double[] latLon = getLatLonForCity(city);
            if (latLon != null) {
                // Fetch 5-day forecast using lat and lon
                JsonObject forecastData = fetchWeatherForecast(latLon[0], latLon[1]);
                if (forecastData != null) {
                    updateForecastTiles(forecastData);
                }
            } else {
                weatherLabel.setText("Error fetching location data.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching forecast data", e);
            weatherLabel.setText("Failed to fetch forecast data.");
        }
    }

    /**
     * Fetch latitude and longitude for a given city using Geocoding API.
     *
     * @param city The city name.
     * @return A double array containing latitude and longitude.
     * @throws IOException If there is a network issue.
     */
    private double[] getLatLonForCity(String city) throws IOException {
        String geocodeUrl = "http://api.openweathermap.org/geo/1.0/direct?q=" + city + "&limit=1&appid=" + API_KEY;
        Request request = new Request.Builder().url(geocodeUrl).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();
                JsonObject locationData = JsonParser.parseString(jsonResponse).getAsJsonArray().get(0).getAsJsonObject();
                double lat = locationData.get("lat").getAsDouble();
                double lon = locationData.get("lon").getAsDouble();
                return new double[]{lat, lon};
            }
        }
        return null;
    }

    /**
     * Fetch 5-day weather forecast using latitude and longitude.
     *
     * @param lat The latitude of the city.
     * @param lon The longitude of the city.
     * @return A JsonObject representing the forecast data.
     * @throws IOException If there is a network issue.
     */
    private JsonObject fetchWeatherForecast(double lat, double lon) throws IOException {
        String forecastUrl = "http://api.openweathermap.org/data/2.5/forecast?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric";
        Request request = new Request.Builder().url(forecastUrl).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();
                return JsonParser.parseString(jsonResponse).getAsJsonObject();
            }
        }
        return null;
    }

    /**
     * Update forecast tiles in the UI based on forecast data.
     *
     * @param forecastData The JsonObject containing forecast data.
     */
    private void updateForecastTiles(JsonObject forecastData) {
        Platform.runLater(() -> {
            forecastContainer.getChildren().clear(); // Clear previous forecast
            JsonArray forecastList = forecastData.getAsJsonArray("list");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE"); // Day of week formatter

            // Iterate over the forecast and generate tiles
            for (int i = 0; i < forecastList.size(); i += 8) { // Select 1 item per day
                JsonObject forecastEntry = forecastList.get(i).getAsJsonObject();
                String day = LocalDate.parse(forecastEntry.get("dt_txt").getAsString().substring(0, 10)).format(formatter);

                JsonObject main = forecastEntry.getAsJsonObject("main");
                double tempMin = main.get("temp_min").getAsDouble();
                double tempMax = main.get("temp_max").getAsDouble();
                String description = forecastEntry.getAsJsonArray("weather").get(0).getAsJsonObject().get("description").getAsString();
                String iconCode = forecastEntry.getAsJsonArray("weather").get(0).getAsJsonObject().get("icon").getAsString();

                try {
                    // Create and update the forecast tile for each day
                    forecastContainer.getChildren().add(createForecastTile(day, tempMin, tempMax, description, iconCode));
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error loading forecast tile FXML", e);
                }
            }
        });
    }

    /**
     * Create and return a forecast tile for each day.
     *
     * @param day The day of the week.
     * @param tempMin The minimum temperature.
     * @param tempMax The maximum temperature.
     * @param description The weather description.
     * @param iconCode The weather icon code.
     * @return An HBox representing the forecast tile.
     * @throws IOException If there is an error loading the FXML.
     */
    private HBox createForecastTile(String day, double tempMin, double tempMax, String description, String iconCode) throws IOException {
        // Load the forecast tile layout from FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("forecast_tile.fxml"));
        HBox forecastTile = loader.load();

        // Get the components from the loaded FXML
        Label dayLabel = (Label) forecastTile.lookup("#dayLabel");
        ImageView weatherIcon = (ImageView) forecastTile.lookup("#weatherIcon");
        Label tempLabel = (Label) forecastTile.lookup("#tempLabel");
        Label descLabel = (Label) forecastTile.lookup("#descLabel");

        // Update the components with data
        dayLabel.setText(day);
        weatherIcon.setImage(new Image(ICON_BASE_URL + iconCode + "@2x.png"));
        tempLabel.setText(String.format("%.1f°C / %.1f°C", tempMin, tempMax));
        descLabel.setText(description);

        return forecastTile;
    }

}
