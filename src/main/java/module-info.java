module com.example.weatherapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires okhttp3;
    requires com.google.gson;
    requires java.logging;

    opens com.example.weatherapp to javafx.fxml;
    exports com.example.weatherapp;
}