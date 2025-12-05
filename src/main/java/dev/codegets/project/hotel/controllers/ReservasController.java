package dev.codegets.project.hotel.controllers;
import dev.codegets.project.hotel.models.Cliente;
import dev.codegets.project.hotel.models.Habitacion;
import dev.codegets.project.hotel.models.Pago;
import dev.codegets.project.hotel.models.Reserva;
import dev.codegets.project.hotel.models.dao.ClienteDao;
import dev.codegets.project.hotel.models.dao.HabitacionDao;
import dev.codegets.project.hotel.models.dao.PagoDao;
import dev.codegets.project.hotel.models.dao.ReservaDao;
import dev.codegets.project.hotel.utils.Alertas;
import dev.codegets.project.hotel.utils.CalculoReserva;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.DateCell;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;


public class ReservasController {
    // Campos del Cliente
    @FXML private TextField txtNombreCliente;
    @FXML private TextField txtTelefonoCliente;
    @FXML private TextField txtCorreoCliente;

    // Campos de la Reserva
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private ComboBox<Habitacion> cmbHabitacion; // Usamos ComboBox para la selección
    @FXML private CheckBox chkPagaTotal; // Para aplicar descuento anticipado

    // Campos de Monto
    @FXML private Label lblMontoCalculado;

    @FXML private Label lblPagoMinimo; // Se necesita en el FXML
    @FXML private TextField txtMontoPago; // Se necesita en el FXML

    private final HabitacionDao habitacionDao = new HabitacionDao();
    private final ReservaDao reservaDao = new ReservaDao();
    private final ClienteDao clienteDao = new ClienteDao();
    private final PagoDao pagoDao = new PagoDao();

    private List<Habitacion> todasLasHabitaciones;

    @FXML
    public void initialize() {
        todasLasHabitaciones = habitacionDao.getAll();
        cmbHabitacion.setConverter(new HabitacionStringConverter(todasLasHabitaciones)); // Necesitas un StringConverter
        cmbHabitacion.getItems().setAll(todasLasHabitaciones);

        // Listener para recalcular al cambiar fechas o checkbox
        dpFechaInicio.valueProperty().addListener((obs, oldV, newV) -> calcularMonto());
        dpFechaFin.valueProperty().addListener((obs, oldV, newV) -> calcularMonto());
        cmbHabitacion.valueProperty().addListener((obs, oldV, newV) -> calcularMonto());
        chkPagaTotal.selectedProperty().addListener((obs, oldV, newV) -> calcularMonto());

        dpFechaInicio.setDayCellFactory(new Callback<DatePicker, DateCell>() {
            @Override
            public DateCell call(DatePicker picker) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        setDisable(empty || item.isBefore(LocalDate.now()));
                    }
                };
            }
        });

        dpFechaFin.setDayCellFactory(new Callback<DatePicker, DateCell>() {
            @Override
            public DateCell call(DatePicker picker) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        LocalDate min = dpFechaInicio.getValue() != null ? dpFechaInicio.getValue() : LocalDate.now();
                        setDisable(empty || item.isBefore(min));
                    }
                };
            }
        });
    }

    @FXML
    private void handleBuscarDisponibilidad() {
        // Implementar lógica de filtrado de habitaciones por disponibilidad y tipo (omitido por brevedad)
        Alertas.mostrarInformacion("Búsqueda", "Funcionalidad de filtro completa se implementa en un servicio aparte.");
    }

    @FXML
    private void calcularMonto() {
        if (cmbHabitacion.getValue() == null || dpFechaInicio.getValue() == null || dpFechaFin.getValue() == null) {
            lblMontoCalculado.setText("Faltan datos");
            return;
        }

        Habitacion habitacion = cmbHabitacion.getValue();
        LocalDateTime fechaInicio = dpFechaInicio.getValue().atTime(CalculoReserva.getHoraEstandar("HORA_CHECKIN"));
        LocalDateTime fechaFin = dpFechaFin.getValue().atTime(CalculoReserva.getHoraEstandar("HORA_CHECKOUT"));
        boolean pagaAnticipada = chkPagaTotal.isSelected();

        if (fechaInicio.isAfter(fechaFin) || fechaInicio.isEqual(fechaFin)) {
            lblMontoCalculado.setText("Fechas inválidas");
            return;
        }

        // VALIDACIÓN CLAVE: Disponibilidad antes de calcular
        if (!reservaDao.isHabitacionAvailable(habitacion.getIdHabitacion(), fechaInicio, fechaFin)) {
            Alertas.mostrarError("Error de Disponibilidad", "La habitación ya está reservada en esas fechas.");
            lblMontoCalculado.setText("NO DISPONIBLE");
            cmbHabitacion.getSelectionModel().clearSelection();
            return;
        }

        double monto = CalculoReserva.calcularMontoTotal(habitacion, fechaInicio, fechaFin, pagaAnticipada);
        lblMontoCalculado.setText(String.format("%.2f", monto));
    }

    // src/controllers/ReservasController.java

    @FXML
    private void handleCrearReserva() {
        String nombre = txtNombreCliente.getText() != null ? txtNombreCliente.getText().trim() : "";
        String telefono = txtTelefonoCliente.getText() != null ? txtTelefonoCliente.getText().replaceAll("\\s+", "") : "";
        String correo = txtCorreoCliente.getText() != null ? txtCorreoCliente.getText().trim() : "";

        // Validación de campos de cliente
        if (nombre.isEmpty() || telefono.isEmpty() || correo.isEmpty()) {
            Alertas.mostrarError("Datos incompletos", "Nombre, teléfono y correo son obligatorios para realizar la reserva.");
            return;
        }
        if (!correo.contains("@")) {
            Alertas.mostrarError("Correo inválido", "El correo debe contener el símbolo '@'.");
            return;
        }
        if (!telefono.matches("\\d{10}")) {
            Alertas.mostrarError("Teléfono inválido", "El número debe tener exactamente 10 dígitos.");
            return;
        }

        // 1. Obtener datos y calcular monto final
        Habitacion habitacion = cmbHabitacion.getValue();
        double montoTotalConDescuento;
        try {
            montoTotalConDescuento = Double.parseDouble(lblMontoCalculado.getText().replace(",", "."));
        } catch (NumberFormatException e) {
            Alertas.mostrarError("Error", "Monto no calculado correctamente.");
            return;
        }

        LocalDateTime fechaInicio = dpFechaInicio.getValue().atTime(CalculoReserva.getHoraEstandar("HORA_CHECKIN"));
        LocalDateTime fechaFin = dpFechaFin.getValue().atTime(CalculoReserva.getHoraEstandar("HORA_CHECKOUT"));

        // 2. Buscar o crear Cliente
        Optional<Cliente> clienteOpt = clienteDao.findOrCreate(nombre, telefono, correo);

        if (clienteOpt.isEmpty()) {
            Alertas.mostrarError("Error", "No se pudo registrar el cliente.");
            return;
        }
        Cliente cliente = clienteOpt.get();

        // ====================================================
        // LÓGICA DE PAGO MANUAL Y VALIDACIÓN (70% vs 100%)
        // ====================================================
        double pagoRealizado;

        try {
            // Se asume que txtMontoPago es un campo válido del FXML
            pagoRealizado = Double.parseDouble(txtMontoPago.getText().trim());
        } catch (NumberFormatException e) {
            Alertas.mostrarError("Error de Pago", "Monto de pago inválido. Por favor, ingrese un valor numérico (ej: 0.00).");
            return;
        }

        // --- CÁLCULO DE REQUERIMIENTOS ---

        // 1. Obtener el monto base (SIN NINGÚN DESCUENTO) para calcular el 70%
        double precioBaseTotalSinDescuento = CalculoReserva.calcularMontoTotal(habitacion, fechaInicio, fechaFin, false);
        long diasEstancia = ChronoUnit.DAYS.between(fechaInicio.toLocalDate(), fechaFin.toLocalDate());
        if (diasEstancia == 0) diasEstancia = 1;

        double montoMinimo70 = precioBaseTotalSinDescuento * 0.70;

        // --- VALIDACIÓN ---

        if (chkPagaTotal.isSelected()) {
            // Caso 1: Descuento Aplicado (Requiere 100% del monto total con descuento)
            if (pagoRealizado < montoTotalConDescuento) {
                Alertas.mostrarError("Pago Insuficiente", "Para aplicar el descuento, el pago debe ser el monto total final: " + String.format("%.2f", montoTotalConDescuento));
                return;
            }
        } else {
            // Caso 2: Pago Parcial (Mínimo 70% del precio base)
            if (pagoRealizado < montoMinimo70) {
                Alertas.mostrarError("Pago Insuficiente", "El pago mínimo para confirmar la reserva es del 70%: " + String.format("%.2f", montoMinimo70));
                return;
            }
        }

        // Si el código llega aquí, el pago cumple con la regla (70% o 100%).

        // 3. Crear Reserva
        Reserva nuevaReserva = new Reserva(cliente.getIdCliente(), habitacion.getIdHabitacion(),
                fechaInicio, fechaFin, montoTotalConDescuento);

        Optional<Integer> idReservaOpt = reservaDao.create(nuevaReserva);

        if (idReservaOpt.isPresent()) {
            // 4. Registrar el pago REALIZADO por el gerente (sea 70% o 100%)
            if (pagoRealizado > 0) {
                Pago pagoReserva = new Pago(0, idReservaOpt.get(), pagoRealizado, "RESERVA", LocalDateTime.now(), "Pago inicial de la reserva.");
                pagoDao.create(pagoReserva);
            }

            // 5. Actualizar estado de la habitación
            habitacionDao.updateEstado(habitacion.getIdHabitacion(), "RESERVADA");


            Alertas.mostrarInformacion("Reserva Creada", "Reserva #" + idReservaOpt.get() + " creada con éxito. Monto total: " + String.format("%.2f", montoTotalConDescuento));
            limpiarCampos();

        } else {
            Alertas.mostrarError("Error", "Fallo al crear la reserva en la base de datos.");
        }
    }

    private void limpiarCampos() {
        // Limpiar campos del Cliente
        txtNombreCliente.clear();
        txtTelefonoCliente.clear();
        txtCorreoCliente.clear();

        // Limpiar campos de la Reserva
        dpFechaInicio.setValue(null);
        dpFechaFin.setValue(null);
        cmbHabitacion.getSelectionModel().clearSelection();
        chkPagaTotal.setSelected(false);

        // Resetear el Monto
        lblMontoCalculado.setText("0.00");
    }
}
