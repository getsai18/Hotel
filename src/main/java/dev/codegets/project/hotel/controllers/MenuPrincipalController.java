package dev.codegets.project.hotel.controllers;

import dev.codegets.project.hotel.models.Usuario;
import dev.codegets.project.hotel.utils.Alertas;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


import java.io.IOException;

public class MenuPrincipalController {
    @FXML private Label lblBienvenida;
    @FXML private Label lblRol;
    @FXML private VBox menuAdmin;
    @FXML private VBox menuGerente;
    @FXML private VBox contentArea;

    private Usuario usuarioActual;

    /**
     * Llamado desde LoginController para establecer el usuario logeado.
     */
    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
        lblBienvenida.setText("Hola, " + usuario.getNombre().split(" ")[0]);
        lblRol.setText("Rol: " + usuario.getRol());

        // Control de visibilidad del menú por rol
        if ("ADMIN".equals(usuario.getRol())) {
            menuAdmin.setVisible(true);
            menuAdmin.setManaged(true);
            menuGerente.setVisible(false);
            menuGerente.setManaged(false);
            // Cargar Configuración por defecto para ADMIN
            handleConfiguracion();
        } else if ("GERENTE".equals(usuario.getRol())) {
            menuAdmin.setVisible(false);
            menuAdmin.setManaged(false);
            menuGerente.setVisible(true);
            menuGerente.setManaged(true);
            // Cargar Habitaciones por defecto para GERENTE (Se implementa en Fase 5)
            // handleHabitaciones();
        }
    }

    // --- Métodos de Navegación del ADMIN ---

    @FXML
    private void handleConfiguracion() {
        if (!"ADMIN".equals(usuarioActual.getRol())) {
            Alertas.mostrarError("Acceso Denegado", "Solo los Administradores pueden acceder a la Configuración.");
            return;
        }
        cargarVista("/resources/fxml/configuracion.fxml");
    }

    // --- Métodos de Navegación del GERENTE (Implementados en Fases 5 y 6) ---

    @FXML private void handleReservas() {
        if (!"GERENTE".equals(usuarioActual.getRol())) return;
        cargarVista("/resources/fxml/reservas.fxml");
    }
    @FXML private void handleCheckIn() {
        if (!"GERENTE".equals(usuarioActual.getRol())) return;
        cargarVista("/resources/fxml/checkIn.fxml");
    }
    @FXML private void handleHabitaciones() {
        if (!"GERENTE".equals(usuarioActual.getRol())) return;
        cargarVista("/resources/fxml/habitaciones.fxml");
    }
    @FXML private void handleCheckOut() { /* Lógica en Fase 6 */ }
    @FXML private void handleReportes() { /* Lógica en Fase 6 */ }

    // --- Utilidades ---

    private void cargarVista(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Limpiar y cargar la nueva vista en el área central
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            VBox.setVgrow(view, javafx.scene.layout.Priority.ALWAYS);

        } catch (IOException e) {
            Alertas.mostrarError("Error de Carga", "No se pudo cargar la vista FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCerrarSesion() {
        // Cargar la ventana de Login
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/login.fxml"));
            Parent root = loader.load();

            Stage currentStage = (Stage) lblBienvenida.getScene().getWindow();

            Stage newStage = new Stage();
            newStage.setTitle("Login - Sistema Hotelero TPS");
            newStage.setScene(new Scene(root));
            newStage.show();

            currentStage.close();
        } catch (IOException e) {
            Alertas.mostrarError("Error", "No se pudo cargar la pantalla de login.");
        }
    }

}
