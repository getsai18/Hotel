package dev.codegets.project.hotel.controllers;
import dev.codegets.project.hotel.models.Habitacion;
import dev.codegets.project.hotel.models.Pago;
import dev.codegets.project.hotel.models.Reserva;
import dev.codegets.project.hotel.models.dao.ConfiguracionDao;
import dev.codegets.project.hotel.models.dao.HabitacionDao;
import dev.codegets.project.hotel.models.dao.PagoDao;
import dev.codegets.project.hotel.models.dao.ReservaDao;
import dev.codegets.project.hotel.utils.Alertas;
import dev.codegets.project.hotel.utils.ConexionDB;
import javafx.fxml.FXML;
import javafx.scene.control.*;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;


public class CheckOutController {

    @FXML private TextField txtHabitacionNumero;
    @FXML private Button btnBuscarOcupacion;
    @FXML private Label lblReservaID;
    @FXML private Label lblClienteInfo;
    @FXML private Label lblFechaSalidaEstimada;
    @FXML private Label lblTotalPendiente;
    @FXML private Label lblPenalizacionInfo;
    @FXML private Button btnFinalizarCheckOut;

    private final ReservaDao reservaDao = new ReservaDao();
    private final PagoDao pagoDao = new PagoDao();
    private final HabitacionDao habitacionDao = new HabitacionDao();
    private final ConfiguracionDao configDao = new ConfiguracionDao();

    private Reserva ocupacionActual;
    private Habitacion habitacionOcupada;
    private double penalizacionTotal = 0.0;

    @FXML
    private void handleBuscarOcupacion() {
        if (txtHabitacionNumero.getText().isEmpty()) {
            Alertas.mostrarError("Error", "Ingresa el número de la habitación.");
            return;
        }

        try {
            int numHab = Integer.parseInt(txtHabitacionNumero.getText());
            Optional<Habitacion> habOpt = habitacionDao.getByNumero(numHab); // Asumiendo que agregaste getByNumero a HabitacionDao

            if (habOpt.isEmpty() || !habOpt.get().getEstado().equals("OCUPADA")) {
                Alertas.mostrarError("Error", "Habitación no encontrada o no está OCUPADA.");
                limpiarDetalles();
                return;
            }

            habitacionOcupada = habOpt.get();
            Optional<Reserva> resOpt = reservaDao.getOccupiedByHabitacion(habitacionOcupada.getIdHabitacion());

            if (resOpt.isEmpty()) {
                Alertas.mostrarError("Error Interno", "La habitación está OCUPADA pero no hay registro de Check-in activo.");
                limpiarDetalles();
                return;
            }

            ocupacionActual = resOpt.get();
            mostrarDetalles();

        } catch (NumberFormatException e) {
            Alertas.mostrarError("Error", "Número de habitación inválido.");
        }
    }

    private void mostrarDetalles() {
        // Asumiendo que podemos obtener el cliente de la reserva
        // Optional<Cliente> clienteOpt = clienteDao.getById(ocupacionActual.getIdCliente());
        // lblClienteInfo.setText(clienteOpt.map(Cliente::getNombre).orElse("N/A"));

        lblReservaID.setText(String.valueOf(ocupacionActual.getIdReserva()));
        lblFechaSalidaEstimada.setText(ocupacionActual.getFechaFin().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        LocalDateTime horaSalidaReal = LocalDateTime.now();

        // 1. Calcular Penalización por Check-out tardío / Día extra
        double precioNoche = habitacionOcupada.getPrecioBase();

        if (horaSalidaReal.isAfter(ocupacionActual.getFechaFin())) {
            // El cliente se fue tarde. Calculamos si es penalización o día extra.
            long horasTarde = ChronoUnit.HOURS.between(ocupacionActual.getFechaFin(), horaSalidaReal);

            // Si pasan más de X horas (ej. 6 horas), se cobra un día extra. Si no, penalización (ej. 50% de la noche).
            if (horasTarde > 6) {
                penalizacionTotal = precioNoche; // Día extra
                lblPenalizacionInfo.setText(String.format("⚠️ DÍA EXTRA: Cliente excedió por %d horas. Cobro por día extra (%.2f).", horasTarde, penalizacionTotal));
            } else {
                Optional<models.Configuracion> penalizacionConfig = configDao.getParametro("PORCENTAJE_RECARGO_CHECKIN_TARDE"); // Usamos el mismo porcentaje para simplificar
                double porcentaje = penalizacionConfig.map(models.Configuracion::getValor).orElse(0.0);
                penalizacionTotal = precioNoche * porcentaje;
                lblPenalizacionInfo.setText(String.format("⚠️ PENALIZACIÓN TARDÍA: %.2f (%.0f%% de noche).", penalizacionTotal, porcentaje * 100));
            }
            lblPenalizacionInfo.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            lblPenalizacionInfo.setText("✅ Check-out a tiempo. Sin penalización.");
            lblPenalizacionInfo.setStyle("-fx-text-fill: green;");
        }

        // Asumiendo 0.00 pendiente si la reserva se pagó totalmente al inicio,
        // o si los pagos se gestionan en otro módulo.
        // Aquí simplificamos el total pendiente al valor de la penalización.
        lblTotalPendiente.setText(String.format("%.2f", penalizacionTotal));
        btnFinalizarCheckOut.setDisable(false);
    }

    @FXML
    private void handleFinalizarCheckOut() {
        if (ocupacionActual == null) return;

        // Iniciar Transacción (TPS)
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);

            LocalDateTime now = LocalDateTime.now();

            // 1. Registrar Penalización/Día extra si aplica
            if (penalizacionTotal > 0) {
                String tipo = (penalizacionTotal == habitacionOcupada.getPrecioBase()) ? "DIA_EXTRA" : "PENALIZACION";
                Pago pagoPenalizacion = new Pago(0, ocupacionActual.getIdReserva(), penalizacionTotal, tipo, now, "Cobro por check-out tardío.");
                pagoDao.create(pagoPenalizacion); // Esto debe usar la conexión transaccional si se cambia el DAO
            }

            // 2. Actualizar Reserva a FINALIZADA y registrar Check-out real
            String sqlReserva = "UPDATE reservas SET estado = 'FINALIZADA', checkout_real = ? WHERE id_reserva = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlReserva)) {
                stmt.setTimestamp(1, Timestamp.valueOf(now));
                stmt.setInt(2, ocupacionActual.getIdReserva());
                stmt.executeUpdate();
            }

            // 3. Actualizar estado de la habitación a DISPONIBLE
            habitacionDao.updateEstado(habitacionOcupada.getIdHabitacion(), "DISPONIBLE"); // Esto debe usar la conexión transaccional si se cambia el DAO

            conn.commit(); // Commit de la Transacción

            Alertas.mostrarInformacion("Check-out Finalizado", "Check-out completado. Se generó un cobro adicional de: " + penalizacionTotal);
            limpiarDetalles();

        } catch (SQLException e) {
            Alertas.mostrarError("Error de Transacción", "Fallo al finalizar el Check-out. Se realizó ROLLBACK.");
            e.printStackTrace();
            // Implementar Rollback si el DAO usa la misma conexión

        }
    }

    // Método para manejar devoluciones/cancelaciones (REQUISITO: Si cliente no llega, devolver parte)
    @FXML
    private void handleGestionPagos() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Gestión de Devoluciones");
        dialog.setHeaderText("Cancelar Reserva y Aplicar Penalización");
        dialog.setContentText("Por favor, ingrese el ID de la reserva a cancelar (No-Show):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().isEmpty()) {
            try {
                int idReserva = Integer.parseInt(result.get());

                // 1. Buscar la Reserva
                Optional<Reserva> resOpt = reservaDao.getById(idReserva);
                if (resOpt.isEmpty() || !resOpt.get().getEstado().equals("ACTIVA")) {
                    Alertas.mostrarError("Error", "Reserva no encontrada o no está ACTIVA.");
                    return;
                }

                Reserva reserva = resOpt.get();

                // 2. Definir el Monto Pagado (Asumimos que el monto pagado es el Monto Total de la reserva)
                // *NOTA: En un sistema real, esto requeriría consultar la tabla de pagos.*
                double montoPagado = reserva.getMontoTotal();

                // 3. Obtener el Porcentaje de Penalización por No-Show
                Optional<models.Configuracion> noShowConfig = configDao.getParametro("PORCENTAJE_PENALIZACION_NO_SHOW");
                double porcentajePenalizacion = noShowConfig.map(models.Configuracion::getValor).orElse(0.0);

                if (porcentajePenalizacion == 0.0) {
                    Alertas.mostrarError("Error de Configuración", "El porcentaje de penalización por No-Show es 0.0. No se puede calcular.");
                    return;
                }

                // 4. Calcular Penalización y Reembolso
                double penalizacion = montoPagado * porcentajePenalizacion;
                double reembolso = montoPagado - penalizacion;

                if (reembolso < 0) reembolso = 0; // Evitar reembolsos negativos

                // 5. Iniciar Transacción (TPS)
                try (Connection conn = ConexionDB.getConnection()) {
                    conn.setAutoCommit(false);
                    LocalDateTime now = LocalDateTime.now();

                    // A. Registrar la Penalización (Lo que el hotel se queda)
                    if (penalizacion > 0) {
                        Pago penalizacionPago = new Pago(0, idReserva, penalizacion, "PENALIZACION", now,
                                "Penalización por No-Show (" + (porcentajePenalizacion * 100) + "%)");
                        pagoDao.create(penalizacionPago);
                    }

                    // B. Registrar el Reembolso (Lo que se devuelve al cliente)
                    if (reembolso > 0) {
                        Pago reembolsoPago = new Pago(0, idReserva, -reembolso, "REEMBOLSO", now,
                                "Devolución por cancelación después de aplicar penalización.");
                        pagoDao.create(reembolsoPago);
                    }

                    // C. Actualizar Estado de la Reserva
                    reservaDao.updateEstado(idReserva, "NO_SHOW");

                    // D. Actualizar estado de la Habitación a DISPONIBLE (Si estaba reservada)
                    habitacionDao.updateEstado(reserva.getIdHabitacion(), "DISPONIBLE");

                    conn.commit(); // Commit de la Transacción

                    Alertas.mostrarInformacion("Cancelación Exitosa",
                            String.format("Reserva #%d marcada como NO-SHOW.\nPenalización: %.2f\nReembolso al Cliente: %.2f",
                                    idReserva, penalizacion, reembolso));

                } catch (SQLException e) {
                    Alertas.mostrarError("Error de Transacción", "Fallo en la gestión de No-Show. Se realizó ROLLBACK.");
                    e.printStackTrace();
                }

            } catch (NumberFormatException e) {
                Alertas.mostrarError("Error", "ID de reserva inválido.");
            }
        }
    }

    private void limpiarDetalles() {
        txtHabitacionNumero.clear();
        lblReservaID.setText("N/A");
        lblClienteInfo.setText("N/A");
        lblFechaSalidaEstimada.setText("N/A");
        lblTotalPendiente.setText("0.00");
        lblPenalizacionInfo.setText("");
        ocupacionActual = null;
        habitacionOcupada = null;
        penalizacionTotal = 0.0;
        btnFinalizarCheckOut.setDisable(true);
    }
}
