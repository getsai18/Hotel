package dev.codegets.project.hotel.controllers;
import dev.codegets.project.hotel.models.*;
import dev.codegets.project.hotel.models.dao.*;
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
    private final ClienteDao clienteDao = new ClienteDao();


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

    // src/controllers/CheckOutController.java

    private void mostrarDetalles() {
        // Asumimos que los DAOs (clienteDao, pagoDao, etc.) están declarados.

        // ============================
        // 1. Mostrar información general (Existente)
        // ============================
        Optional<Cliente> clienteOpt = clienteDao.getById(ocupacionActual.getIdCliente());

        if (clienteOpt.isPresent()) {
            Cliente c = clienteOpt.get();
            lblClienteInfo.setText(c.getNombre());
        } else {
            lblClienteInfo.setText("Cliente no encontrado");
        }

        lblReservaID.setText(String.valueOf(ocupacionActual.getIdReserva()));
        lblFechaSalidaEstimada.setText(
                ocupacionActual.getFechaFin().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                )
        );

        // ============================
        // 2. Calcular penalización por check-out tardío (Existente)
        // ============================
        LocalDateTime horaSalidaReal = LocalDateTime.now();
        double precioNoche = habitacionOcupada.getPrecioBase();
        penalizacionTotal = 0.0; // Resetear antes del cálculo

        if (horaSalidaReal.isAfter(ocupacionActual.getFechaFin())) {
            long horasTarde = ChronoUnit.HOURS.between(ocupacionActual.getFechaFin(), horaSalidaReal);
            // ... (Lógica para calcular penalizacionTotal basada en horasTarde) ...

            Optional<Configuracion> penalizacionConfig = configDao.getParametro("PORCENTAJE_RECARGO_CHECKIN_TARDE");
            double porcentaje = penalizacionConfig.map(Configuracion::getValor).orElse(0.0);

            if (horasTarde > 6) {
                penalizacionTotal = precioNoche; // Día extra completo
                lblPenalizacionInfo.setText(String.format("⚠️ DÍA EXTRA: Cliente excedió por %d horas. Cobro por día extra (%.2f).", horasTarde, penalizacionTotal));
            } else {
                penalizacionTotal = precioNoche * porcentaje;
                lblPenalizacionInfo.setText(String.format("⚠️ PENALIZACIÓN TARDÍA: %.2f (%.0f%% de la noche).", penalizacionTotal, porcentaje * 100));
            }

            lblPenalizacionInfo.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        } else {
            lblPenalizacionInfo.setText("✅ Check-out a tiempo. Sin penalización.");
            lblPenalizacionInfo.setStyle("-fx-text-fill: green;");
        }

        // ============================
        // 3. CÁLCULO DEL SALDO PENDIENTE TOTAL
        // ============================
        double montoTotalReserva = ocupacionActual.getMontoTotal();

        // **USO DEL DAO CORREGIDO**
        double totalPagadoNeto = pagoDao.getTotalPagadoByReserva(ocupacionActual.getIdReserva());

        double saldoEstanciaPendiente = montoTotalReserva - totalPagadoNeto;

        // Sumamos el saldo pendiente de la estancia + cualquier penalización
        double totalACobrar = saldoEstanciaPendiente + penalizacionTotal;

        // 4. Actualizar Label
        lblTotalPendiente.setText(String.format("%.2f", totalACobrar));

        // Cambiamos el color si el saldo es alto
        if (totalACobrar > 0.01) {
            lblTotalPendiente.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: red;");
        } else {
            lblTotalPendiente.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: green;");
        }

        btnFinalizarCheckOut.setDisable(false);
    }


    // src/controllers/CheckOutController.java

    // src/controllers/CheckOutController.java (Dentro de handleFinalizarCheckOut)

    // src/controllers/CheckOutController.java

    @FXML
    private void handleFinalizarCheckOut() {
        if (ocupacionActual == null) return;

        // Nota: El totalACobrar ya fue calculado en mostrarDetalles()
        // Lo recalculamos aquí para seguridad antes de la transacción.
        double montoTotalReserva = ocupacionActual.getMontoTotal();
        double totalPagado = pagoDao.getTotalPagadoByReserva(ocupacionActual.getIdReserva());
        double saldoEstanciaPendiente = montoTotalReserva - totalPagado;

        // La variable penalizacionTotal es una variable de clase y ya contiene el cargo por salida tardía.
        double totalACobrar = saldoEstanciaPendiente + penalizacionTotal;

        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            LocalDateTime now = LocalDateTime.now();

            // ----------------------------------------------------
            // PASO 1: REGISTRAR PAGO FINAL (Cubre Saldo + Penalizaciones)
            // ----------------------------------------------------

            if (totalACobrar > 0.01) {
                // src/controllers/CheckOutController.java (Línea ~125)

                Pago pagoFinal = new Pago(0, ocupacionActual.getIdReserva(), totalACobrar, "RESERVA", now,
                        "Pago final de estancia y cargos pendientes.");
                pagoDao.create(pagoFinal); // Asumimos que el DAO registra la transacción con éxito
            } else {
                // Si el saldo es cero o negativo (por reembolso), no se registra un pago final.
            }

            // ----------------------------------------------------
            // PASO 2: CERRAR RESERVA Y LIBERAR HABITACIÓN
            // ----------------------------------------------------

            // 1. Actualizar Reserva a FINALIZADA y registrar Check-out real
            String sqlReserva = "UPDATE reservas SET estado = 'FINALIZADA', checkout_real = ? WHERE id_reserva = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlReserva)) {
                stmt.setTimestamp(1, Timestamp.valueOf(now));
                stmt.setInt(2, ocupacionActual.getIdReserva());
                stmt.executeUpdate();
            }

            // 2. Actualizar estado de la habitación
            habitacionDao.updateEstado(habitacionOcupada.getIdHabitacion(), "DISPONIBLE");

            conn.commit(); // CONFIRMAR TRANSACCIÓN

            Alertas.mostrarInformacion("Check-out Finalizado",
                    String.format("Check-out completado. Se saldó la cuenta cobrando: %.2f.", totalACobrar));
            limpiarDetalles();

        } catch (SQLException e) {
            Alertas.mostrarError("Error de Transacción", "Fallo al finalizar el Check-out. Se realizó ROLLBACK.");
            e.printStackTrace();
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
                Optional<Configuracion> noShowConfig = configDao.getParametro("PORCENTAJE_PENALIZACION_NO_SHOW");
                double porcentajePenalizacion = noShowConfig.map(Configuracion::getValor).orElse(0.0);

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
