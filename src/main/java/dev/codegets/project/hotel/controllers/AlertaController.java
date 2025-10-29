package dev.codegets.project.hotel.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class AlertaController {

    @FXML private Label lblIcono;
    @FXML private Label lblTitulo;
    @FXML private Label lblContenido;
    @FXML private Button btnAceptar;

    /**
     * Inicializa los datos de la alerta.
     */
    public void setDatos(String titulo, String contenido, String tipo) {
        lblTitulo.setText(titulo);
        lblContenido.setText(contenido);

        // Define el icono y el estilo basado en el tipo
        if ("ERROR".equals(tipo)) {
            lblIcono.setText("❌");
            lblIcono.setStyle("-fx-text-fill: #E63946;"); // Color de error
        } else if ("INFO".equals(tipo)) {
            lblIcono.setText("✅");
            lblIcono.setStyle("-fx-text-fill: #00A389;"); // Color de éxito/info
        }
    }

    @FXML
    private void handleAceptar() {
        // Cierra la ventana (Stage) actual
        Stage stage = (Stage) btnAceptar.getScene().getWindow();
        stage.close();
    }
}