package dev.codegets.project.hotel;

import dev.codegets.project.hotel.models.dao.ConfiguracionDao;
import dev.codegets.project.hotel.models.dao.HabitacionDao;
import dev.codegets.project.hotel.utils.Alertas;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // --- INICIALIZACIÓN DE DATOS MAESTROS (Fase 3) ---
        new HabitacionDao().inicializarHabitaciones();
        new ConfiguracionDao().inicializarParametros();
        // ----------------------------------------------------

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dev/codegets/project/hotel/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setTitle("Login - Sistema Hotelero TPS");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            Alertas.mostrarError("Error Fatal", "No se pudo cargar la interfaz de login. Verifica la estructura de carpetas.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}