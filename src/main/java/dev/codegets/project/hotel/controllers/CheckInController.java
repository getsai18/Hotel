package dev.codegets.project.hotel.controllers;
import dev.codegets.project.hotel.models.Habitacion;
import dev.codegets.project.hotel.models.Pago;
import dev.codegets.project.hotel.models.Reserva;
import dev.codegets.project.hotel.models.dao.HabitacionDao;
import dev.codegets.project.hotel.models.dao.PagoDao;
import dev.codegets.project.hotel.models.dao.ReservaDao;
import dev.codegets.project.hotel.utils.Alertas;
import dev.codegets.project.hotel.utils.CalculoReserva;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;


public class CheckInController {
    @FXML private TextField txtIdReserva;
    @FXML private Button btnBuscarReserva;
    @FXML private Label lblDetallesReserva;
    @FXML private Label lblRecargo;
    @FXML private Button btnRealizarCheckin;

    private final ReservaDao reservaDao = new ReservaDao();
    private final PagoDao pagoDao = new PagoDao();
    private final HabitacionDao habitacionDao = new HabitacionDao();
    private Reserva reservaActual;
    private double recargoPorTarde = 0.0;

    // Método para manejar la asignación directa de habitaciones (sin reserva previa)
    // Para simplificar, esta lógica se remite a crear una "Reserva Express"
    // con fecha de inicio = ahora, y fecha fin = mañana a la hora de check-out.
    // Esto es un requisito del rol Gerente.

    @FXML
    private void handleBuscarReserva() {
        if (txtIdReserva.getText().isEmpty()) {
            Alertas.mostrarError("Error", "Ingresa el ID de la reserva.");
            return;
        }

        try {
            int idReserva = Integer.parseInt(txtIdReserva.getText());
            Optional<Reserva> resOpt = reservaDao.getById(idReserva);

            if (resOpt.isEmpty()) {
                Alertas.mostrarError("Error", "Reserva no encontrada.");
                limpiarDetalles();
                return;
            }

            reservaActual = resOpt.get();

            if (!reservaActual.getEstado().equals("ACTIVA")) {
                Alertas.mostrarError("Error", "La reserva no está ACTIVA. Estado: " + reservaActual.getEstado());
                limpiarDetalles();
                return;
            }

            mostrarDetalles();

        } catch (NumberFormatException e) {
            Alertas.mostrarError("Error", "ID de reserva inválido.");
        }
    }

    private void mostrarDetalles() {
        Optional<Habitacion> habOpt = habitacionDao.getById(reservaActual.getIdHabitacion());
        if (habOpt.isEmpty()) return;
        Habitacion habitacion = habOpt.get();

        LocalDateTime fechaLlegada = LocalDateTime.now();
        LocalTime horaLlegada = fechaLlegada.toLocalTime();

        // Obtener las fechas
        java.time.LocalDate fechaReserva = reservaActual.getFechaInicio().toLocalDate();
        java.time.LocalDate fechaActual = fechaLlegada.toLocalDate();

        // 1. Verificar si la llegada es ANTICIPADA (Business Validation)
        if (fechaActual.isBefore(fechaReserva)) {
            long diasAnticipacion = java.time.temporal.ChronoUnit.DAYS.between(fechaActual, fechaReserva);

            // Mostrar ADVERTENCIA
            Alertas.mostrarInformacion("Llegada Anticipada",
                    "El cliente ha llegado " + diasAnticipacion +
                            " días antes. La reserva comienza el " + fechaReserva +
                            ".\n\nSi procede con el Check-in ahora, se le cobrarán las noches extra.");

            // Opcional: Podrías deshabilitar el botón si no quieres permitir esto sin intervención
            // btnRealizarCheckin.setDisable(true);
            // return;

            // NOTA: Si llegamos aquí, permitimos continuar, pero el gerente debe estar avisado.
        }
        // Fin de la validación anticipada


        // Calcular recargo por llegada tarde (usando el precio base de la habitación)
        // La lógica de CalculoReserva.java debe asegurar que si llega antes de la fecha (y pasó la validación de arriba), no hay recargo.
        recargoPorTarde = CalculoReserva.calcularRecargoCheckInTarde(
                fechaReserva, horaLlegada, habitacion.getPrecioBase()
        );

        String detalles = String.format(
                "Habitación: %d (%s)\nFecha Reservada: %s\nFecha Actual: %s\nCheck-in Estándar: %s\nMonto Total Reserva: %.2f",
                habitacion.getNumero(), habitacion.getTipo(),
                fechaReserva, fechaActual,
                CalculoReserva.getHoraEstandar("HORA_CHECKIN"), reservaActual.getMontoTotal()
        );
        lblDetallesReserva.setText(detalles);

        if (recargoPorTarde > 0) {
            lblRecargo.setText(String.format("⚠️ Recargo Aplicable por retraso: %.2f", recargoPorTarde));
            lblRecargo.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        } else {
            lblRecargo.setText("✅ Llegada a tiempo. No hay recargo.");
            lblRecargo.setStyle("-fx-text-fill: green;");
        }
        btnRealizarCheckin.setDisable(false);
    }
    // src/controllers/CheckInController.java

    @FXML
    private void handleRealizarCheckin() {
        if (reservaActual == null) {
            Alertas.mostrarError("Error", "Primero busca una reserva válida.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Datos de la habitación y fechas
        Optional<Habitacion> habOpt = habitacionDao.getById(reservaActual.getIdHabitacion());
        if (habOpt.isEmpty()) {
            Alertas.mostrarError("Error", "No se encontraron detalles de la habitación.");
            return;
        }
        Habitacion habitacion = habOpt.get();

        // ----------------------------------------------------
        // LÓGICA DE COBRO POR LLEGADA ANTICIPADA
        // ----------------------------------------------------
        double costoAnticipado = CalculoReserva.calcularCostoAnticipado(
                now.toLocalDate(),
                reservaActual.getFechaInicio().toLocalDate(),
                habitacion.getPrecioBase()
        );

        // Si hay noches anticipadas, las registramos como un pago de RECARGO.
        if (costoAnticipado > 0) {
            Pago pagoAnticipado = new Pago(0, reservaActual.getIdReserva(), costoAnticipado, "RECARGO", now,
                    "Cobro por " + (int)(costoAnticipado / habitacion.getPrecioBase()) + " noches de llegada anticipada.");
            pagoDao.create(pagoAnticipado);

            Alertas.mostrarInformacion("Cobro Anticipado",
                    String.format("Se ha cobrado un RECARGO de %.2f por noches anticipadas (llegada en %s).",
                            costoAnticipado, now.toLocalDate()));

            // Nota: La fecha de inicio de la reserva NO se modifica, solo se factura el adelanto.
        }
        // ----------------------------------------------------

        // 1. Registrar Check-in real en la reserva
        if (!reservaDao.updateCheckIn(reservaActual.getIdReserva(), now)) {
            Alertas.mostrarError("Error de BD", "Fallo al registrar la hora de Check-in.");
            return;
        }

        // 2. Registrar el recargo por HORA TARDÍA (si aplica en la fecha reservada)
        if (recargoPorTarde > 0) {
            Pago pagoRecargo = new Pago(0, reservaActual.getIdReserva(), recargoPorTarde, "RECARGO", now,
                    "Recargo por llegada después del Check-in estándar.");
            pagoDao.create(pagoRecargo);
            Alertas.mostrarInformacion("Check-in Exitoso",
                    String.format("Check-in de la Reserva #%d completado. Recargo de %.2f registrado.", reservaActual.getIdReserva(), recargoPorTarde));
        } else {
            Alertas.mostrarInformacion("Check-in Exitoso", "Check-in de la Reserva #" + reservaActual.getIdReserva() + " completado sin recargos adicionales.");
        }

        // 3. Actualizar el estado de la habitación a OCUPADA
        habitacionDao.updateEstado(reservaActual.getIdHabitacion(), "OCUPADA");

        limpiarDetalles();
    }

    private void limpiarDetalles() {
        txtIdReserva.clear();
        lblDetallesReserva.setText("Detalles de la reserva aparecerán aquí...");
        lblRecargo.setText("");
        reservaActual = null;
        recargoPorTarde = 0.0;
        btnRealizarCheckin.setDisable(true);
    }

}
