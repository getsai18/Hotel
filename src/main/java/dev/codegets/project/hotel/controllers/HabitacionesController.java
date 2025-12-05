package dev.codegets.project.hotel.controllers;

import dev.codegets.project.hotel.models.Habitacion;
import dev.codegets.project.hotel.models.dao.HabitacionDao;
import dev.codegets.project.hotel.utils.Alertas;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;




import java.util.List;




public class HabitacionesController {
    @FXML private TableView<Habitacion> tblHabitaciones;
    @FXML private TableColumn<Habitacion, Integer> colNumero;
    @FXML private TableColumn<Habitacion, String> colTipo;
    @FXML private TableColumn<Habitacion, String> colEstado;
    @FXML private TableColumn<Habitacion, Double> colPrecio;
    @FXML private Label lblTotalHabitaciones;


    private final HabitacionDao habitacionDao = new HabitacionDao();
    private ObservableList<Habitacion> listaHabitaciones;

// src/controllers/HabitacionesController.java

    @FXML
    public void initialize() {
        // --- Configuración de celdas (EXISTENTE) ---
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioBase"));

        // --- LÓGICA CLAVE: Configuración de Fila para Estilos Dinámicos ---
        tblHabitaciones.setRowFactory(tv -> new TableRow<Habitacion>() {
            @Override
            protected void updateItem(Habitacion item, boolean empty) {
                super.updateItem(item, empty);

                // 1. Limpiamos todas las clases CSS que pueda tener la fila
                getStyleClass().remove("disponible");
                getStyleClass().remove("ocupada");
                getStyleClass().remove("reservada");
                getStyleClass().remove("mantenimiento");

                if (item != null && !empty) {
                    // 2. Convertimos el estado a minúsculas (ej: 'DISPONIBLE' -> 'disponible')
                    String estado = item.getEstado().toLowerCase();

                    // 3. Aplicamos la clase CSS correspondiente para darle color
                    getStyleClass().add(estado);
                }
            }
        });
        // --- FIN LÓGICA CLAVE ---

        cargarHabitaciones(); // Aseguramos que los datos se carguen después de configurar la tabla

    }

    private void cargarHabitaciones() {
        List<Habitacion> habitaciones = habitacionDao.getAll();
        listaHabitaciones = FXCollections.observableArrayList(habitaciones);
        tblHabitaciones.setItems(listaHabitaciones);
        lblTotalHabitaciones.setText(String.format("Total: %d | Disponibles: %d",
                habitaciones.size(),
                habitaciones.stream().filter(h -> h.getEstado().equals("DISPONIBLE")).count()));
    }

    @FXML
    private void handleRefrescar() {
        cargarHabitaciones();
    }

}
