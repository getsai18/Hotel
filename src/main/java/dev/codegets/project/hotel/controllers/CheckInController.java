package dev.codegets.project.hotel.controllers;

import dev.codegets.project.hotel.models.Habitacion;
import dev.codegets.project.hotel.models.Pago;
import dev.codegets.project.hotel.models.Reserva;
import dev.codegets.project.hotel.models.dao.HabitacionDao;
import dev.codegets.project.hotel.models.dao.PagoDao;
import dev.codegets.project.hotel.models.dao.ReservaDao;
import dev.codegets.project.hotel.utils.Alertas;
import dev.codegets.project.hotel.utils.CalculoReserva;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class CheckInController {

    // --- CAMBIO: Tabla en lugar de TextField ---
    @FXML private TableView<Reserva> tblReservasPendientes;
    @FXML private TableColumn<Reserva, Integer> colIdReserva;
    @FXML private TableColumn<Reserva, String> colCliente;
    @FXML private TableColumn<Reserva, Integer> colHabitacion;
    @FXML private TableColumn<Reserva, String> colFechaInicio;

    @FXML private Label lblDetallesReserva;
    @FXML private Label lblRecargo;
    @FXML private Button btnRealizarCheckin;

    private final ReservaDao reservaDao = new ReservaDao();
    private final PagoDao pagoDao = new PagoDao();
    private final HabitacionDao habitacionDao = new HabitacionDao();

    private Reserva reservaActual;
    private double recargoPorTarde = 0.0;

    @FXML
    public void initialize() {
        // Configurar columnas
        colIdReserva.setCellValueFactory(new PropertyValueFactory<>("idReserva"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colHabitacion.setCellValueFactory(new PropertyValueFactory<>("idHabitacion"));
        colFechaInicio.setCellValueFactory(new PropertyValueFactory<>("fechaInicio"));

        // Cargar datos iniciales
        cargarReservasPendientes();

        // Evento de Doble Clic para seleccionar
        tblReservasPendientes.setRowFactory(tv -> {
            TableRow<Reserva> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Reserva rowData = row.getItem();
                    seleccionarReserva(rowData);
                }
            });
            return row;
        });
    }

    private void cargarReservasPendientes() {
        ObservableList<Reserva> lista = FXCollections.observableArrayList(reservaDao.getReservasPendientesCheckIn());
        tblReservasPendientes.setItems(lista);
    }

    private void seleccionarReserva(Reserva reserva) {
        this.reservaActual = reserva;
        mostrarDetalles();
    }

    private void mostrarDetalles() {
        Optional<Habitacion> habOpt = habitacionDao.getById(reservaActual.getIdHabitacion());
        if (habOpt.isEmpty()) return;
        Habitacion habitacion = habOpt.get();

        LocalDateTime fechaLlegada = LocalDateTime.now();
        LocalTime horaLlegada = fechaLlegada.toLocalTime();
        java.time.LocalDate fechaReserva = reservaActual.getFechaInicio().toLocalDate();
        java.time.LocalDate fechaActual = fechaLlegada.toLocalDate();

        // 1. Verificar si la llegada es ANTICIPADA
        if (fechaActual.isBefore(fechaReserva)) {
            long diasAnticipacion = java.time.temporal.ChronoUnit.DAYS.between(fechaActual, fechaReserva);
            Alertas.mostrarInformacion("Llegada Anticipada",
                    "El cliente ha llegado " + diasAnticipacion + " días antes.\nSi procede, se cobrarán las noches extra.");
        }

        recargoPorTarde = CalculoReserva.calcularRecargoCheckInTarde(
                fechaReserva, horaLlegada, habitacion.getPrecioBase()
        );

        String detalles = String.format(
                "Cliente: %s\nHabitación: %d (%s)\nFecha Reservada: %s\nCheck-in Estándar: %s",
                reservaActual.getNombreCliente(),
                habitacion.getNumero(), habitacion.getTipo(),
                fechaReserva,
                CalculoReserva.getHoraEstandar("HORA_CHECKIN")
        );
        lblDetallesReserva.setText(detalles);

        if (recargoPorTarde > 0) {
            lblRecargo.setText(String.format("⚠️ Recargo Aplicable: %.2f", recargoPorTarde));
            lblRecargo.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        } else {
            lblRecargo.setText("✅ Llegada a tiempo.");
            lblRecargo.setStyle("-fx-text-fill: green;");
        }
        btnRealizarCheckin.setDisable(false);
    }

    @FXML
    private void handleRealizarCheckin() {
        if (reservaActual == null) return;

        LocalDateTime now = LocalDateTime.now();
        Optional<Habitacion> habOpt = habitacionDao.getById(reservaActual.getIdHabitacion());
        if (habOpt.isEmpty()) return;
        Habitacion habitacion = habOpt.get();

        // Lógica de cobro anticipado
        double costoAnticipado = CalculoReserva.calcularCostoAnticipado(
                now.toLocalDate(), reservaActual.getFechaInicio().toLocalDate(), habitacion.getPrecioBase());

        if (costoAnticipado > 0) {
            Pago pagoAnticipado = new Pago(0, reservaActual.getIdReserva(), costoAnticipado, "RECARGO", now,
                    "Cobro por llegada anticipada.");
            pagoDao.create(pagoAnticipado);
            Alertas.mostrarInformacion("Cobro Extra", "Se cobró " + costoAnticipado + " por llegada anticipada.");
        }

        if (!reservaDao.updateCheckIn(reservaActual.getIdReserva(), now)) {
            Alertas.mostrarError("Error", "Fallo al registrar Check-in.");
            return;
        }

        if (recargoPorTarde > 0) {
            Pago pagoRecargo = new Pago(0, reservaActual.getIdReserva(), recargoPorTarde, "RECARGO", now,
                    "Recargo por llegada tarde.");
            pagoDao.create(pagoRecargo);
        }

        habitacionDao.updateEstado(reservaActual.getIdHabitacion(), "OCUPADA");

        Alertas.mostrarInformacion("Éxito", "Check-in completado para " + reservaActual.getNombreCliente());
        limpiarDetalles();
    }

    private void limpiarDetalles() {
        lblDetallesReserva.setText("Seleccione una reserva de la tabla...");
        lblRecargo.setText("");
        reservaActual = null;
        recargoPorTarde = 0.0;
        btnRealizarCheckin.setDisable(true);
        cargarReservasPendientes(); // Recargar la tabla para quitar la reserva procesada
    }
}