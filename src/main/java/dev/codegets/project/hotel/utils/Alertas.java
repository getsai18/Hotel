// src/utils/Alertas.java
package dev.codegets.project.hotel.utils;

import dev.codegets.project.hotel.controllers.AlertaController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class Alertas {

    private static final String FXML_PATH = "/dev/codegets/project/hotel/fxml/advertencia.fxml";

    private static void mostrarAlertaPersonalizada(String titulo, String contenido, String tipo) {
        try {
            FXMLLoader loader = new FXMLLoader(Alertas.class.getResource(FXML_PATH));
            Parent root = loader.load();

            AlertaController controller = loader.getController();
            controller.setDatos(titulo, contenido, tipo);

            Stage stage = new Stage();
            // Esto hace que la ventana sea modal (bloquea la ventana principal)
            stage.initModality(Modality.APPLICATION_MODAL);
            // Esto elimina la barra de título nativa del sistema
            stage.initStyle(StageStyle.UNDECORATED);

            stage.setScene(new Scene(root));
            stage.setTitle(titulo);
            stage.showAndWait();

        } catch (IOException e) {
            System.err.println("Error al cargar FXML de Alerta Personalizada: " + FXML_PATH);
            e.printStackTrace();
            // Fallback a consola si el FXML no carga
            // new Alert(Alert.AlertType.ERROR, contenido).showAndWait(); 
        }
    }

    public static void mostrarError(String titulo, String contenido) {
        mostrarAlertaPersonalizada(titulo, contenido, "ERROR");
    }

    public static void mostrarInformacion(String titulo, String contenido) {
        mostrarAlertaPersonalizada(titulo, contenido, "INFO");
    }
}