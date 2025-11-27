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

    @FXML
    private void handleCrearReserva() {
        // Validaciones básicas de campos...

        // 1. Obtener datos y calcular monto final
        Habitacion habitacion = cmbHabitacion.getValue();
        double montoTotal;
        try {
            montoTotal = Double.parseDouble(lblMontoCalculado.getText().replace(",", "."));
        } catch (NumberFormatException e) {
            Alertas.mostrarError("Error", "Monto no calculado correctamente.");
            return;
        }

        LocalDateTime fechaInicio = dpFechaInicio.getValue().atTime(CalculoReserva.getHoraEstandar("HORA_CHECKIN"));
        LocalDateTime fechaFin = dpFechaFin.getValue().atTime(CalculoReserva.getHoraEstandar("HORA_CHECKOUT"));

        // 2. Buscar o crear Cliente
        Optional<Cliente> clienteOpt = clienteDao.findOrCreate(
                txtNombreCliente.getText(), txtTelefonoCliente.getText(), txtCorreoCliente.getText());

        if (clienteOpt.isEmpty()) {
            Alertas.mostrarError("Error", "No se pudo registrar el cliente.");
            return;
        }
        Cliente cliente = clienteOpt.get();

        // 3. Crear Reserva
        Reserva nuevaReserva = new Reserva(cliente.getIdCliente(), habitacion.getIdHabitacion(),
                fechaInicio, fechaFin, montoTotal);

        Optional<Integer> idReservaOpt = reservaDao.create(nuevaReserva);

        if (idReservaOpt.isPresent()) {
            // 4. Registrar Pago si el cliente paga al reservar (REQUISITO: Registrar automáticamente un pago)
            if (chkPagaTotal.isSelected()) {
                Pago pagoReserva = new Pago(0, idReservaOpt.get(), montoTotal, "RESERVA", LocalDateTime.now(), "Pago total de la reserva");
                pagoDao.create(pagoReserva);
            }

            // 5. Actualizar estado de la habitación (RESERVADA)
            // NOTA: La habitación no necesita cambiar de estado en la tabla 'habitaciones',
            // el estado de 'RESERVADA' se deduce de la tabla 'reservas' (ACTIVA sin check-in).
            // Sin embargo, para fines visuales en el módulo de Habitaciones:
            // habitacionDao.updateEstado(habitacion.getIdHabitacion(), "RESERVADA");

            Alertas.mostrarInformacion("Reserva Creada", "Reserva #" + idReservaOpt.get() + " creada con éxito. Monto total: " + montoTotal);
            // Limpiar campos...
        } else {
            Alertas.mostrarError("Error", "Fallo al crear la reserva en la base de datos.");
        }
    }

}
