package dev.codegets.project.hotel.controllers;

import dev.codegets.project.hotel.models.Usuario;
import dev.codegets.project.hotel.models.dao.ConfiguracionDao;
import dev.codegets.project.hotel.models.dao.HabitacionDao;
import dev.codegets.project.hotel.models.dao.UsuarioDao;
import dev.codegets.project.hotel.utils.Alertas;
import dev.codegets.project.hotel.utils.SecurityUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import java.util.HashMap;
import java.util.Optional;

public class ConfiguracionController {
    @FXML private Label lblEstadoGerente;
    @FXML private TextField txtGerenteNombre;
    @FXML private TextField txtGerenteUsername;
    @FXML private TextField txtGerentePassword;

    @FXML private TextField txtPrecioMaster;
    @FXML private TextField txtPrecioEstandar;

    @FXML private TextField txtRecargoTarde;
    @FXML private TextField txtPenalizacionNoShow;
    @FXML private TextField txtDescuentoAnticipada;
    @FXML private TextField txtHoraCheckin;
    @FXML private TextField txtHoraCheckout;

    private final UsuarioDao usuarioDao = new UsuarioDao();
    private final HabitacionDao habitacionDao = new HabitacionDao();
    private final ConfiguracionDao configDao = new ConfiguracionDao();

    // Mapeo para facilitar la actualización de parámetros
    private final HashMap<String, TextField> paramMap = new HashMap<>();

    @FXML
    public void initialize() {
        paramMap.put("PORCENTAJE_RECARGO_CHECKIN_TARDE", txtRecargoTarde);
        paramMap.put("PORCENTAJE_PENALIZACION_CANCELACION", txtPenalizacionNoShow);
        paramMap.put("PORCENTAJE_DESCUENTO_ANTICIPADA", txtDescuentoAnticipada);
        paramMap.put("HORA_CHECKIN", txtHoraCheckin);
        paramMap.put("HORA_CHECKOUT", txtHoraCheckout);

        java.util.function.UnaryOperator<TextFormatter.Change> decimalFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            if (newText.matches("\\d*\\.?\\d*")) return change;
            return null;
        };
        java.util.function.UnaryOperator<TextFormatter.Change> hourFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            if (newText.matches("([0-9]{0,2})(\\.[0-9]{0,2})?")) return change;
            return null;
        };

        txtRecargoTarde.setTextFormatter(new TextFormatter<>(decimalFilter));
        txtPenalizacionNoShow.setTextFormatter(new TextFormatter<>(decimalFilter));
        txtDescuentoAnticipada.setTextFormatter(new TextFormatter<>(decimalFilter));
        txtPrecioMaster.setTextFormatter(new TextFormatter<>(decimalFilter));
        txtPrecioEstandar.setTextFormatter(new TextFormatter<>(decimalFilter));
        txtHoraCheckin.setTextFormatter(new TextFormatter<>(hourFilter));
        txtHoraCheckout.setTextFormatter(new TextFormatter<>(hourFilter));

        cargarEstadoInicial();
    }

    private void cargarEstadoInicial() {
        // Cargar estado del Gerente
        if (usuarioDao.existsGerente()) {
            lblEstadoGerente.setText("✅ Gerente activo existente.");
            lblEstadoGerente.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
        } else {
            lblEstadoGerente.setText("❌ No existe un gerente activo.");
            lblEstadoGerente.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
        }

        // Cargar precios base (Asumimos que el precio de MASTER es el de la primera habitación MASTER)
        habitacionDao.getAll().stream()
                .filter(h -> h.getTipo().equals("MASTER")).findFirst()
                .ifPresent(h -> txtPrecioMaster.setText(String.format("%.2f", h.getPrecioBase())));

        habitacionDao.getAll().stream()
                .filter(h -> h.getTipo().equals("ESTANDAR")).findFirst()
                .ifPresent(h -> txtPrecioEstandar.setText(String.format("%.2f", h.getPrecioBase())));

        // Cargar parámetros de configuración
        paramMap.keySet().forEach(paramName ->
                configDao.getParametro(paramName)
                        .ifPresent(cfg -> paramMap.get(paramName).setText(String.format("%.2f", cfg.getValor())))
        );
    }

    @FXML
    private void handleGestionGerente() {
        String nombre = txtGerenteNombre.getText();
        String username = txtGerenteUsername.getText();
        String rawPassword = txtGerentePassword.getText();

        if (nombre.isEmpty() || username.isEmpty() || rawPassword.isEmpty()) {
            Alertas.mostrarError("Error de Campos", "Todos los campos del Gerente son obligatorios.");
            return;
        }

        // El requisito es que solo puede existir UN gerente. Si existe, este es un reemplazo/actualización.
        // Aquí simplificamos, permitiendo la creación/actualización por el ADMIN.

        // 1. Hashear la contraseña
        String hashedPassword = SecurityUtils.hashPassword(rawPassword);
        if (hashedPassword == null) return;

        // 2. Crear el objeto Usuario (el ID será 0, la DB lo asignará)
        Usuario nuevoGerente = new Usuario(0, nombre, username, hashedPassword, "GERENTE");

        // 3. Crear en la base de datos
        if (usuarioDao.create(nuevoGerente)) {
            Alertas.mostrarInformacion("Éxito", "Gerente '" + username + "' creado/actualizado correctamente.");
            txtGerenteNombre.clear();
            txtGerenteUsername.clear();
            txtGerentePassword.clear();
            cargarEstadoInicial(); // Recargar estado del gerente
        } else {
            Alertas.mostrarError("Error de BD", "No se pudo crear/actualizar el Gerente. El usuario podría ya existir.");
        }
    }

    @FXML
    private void handleGuardarPrecios() {
        try {
            double masterPrice = Double.parseDouble(txtPrecioMaster.getText());
            double estandarPrice = Double.parseDouble(txtPrecioEstandar.getText());

            boolean masterOk = habitacionDao.updatePrecioBase("MASTER", masterPrice);
            boolean estandarOk = habitacionDao.updatePrecioBase("ESTANDAR", estandarPrice);

            if (masterOk && estandarOk) {
                Alertas.mostrarInformacion("Éxito", "Precios base actualizados correctamente.");
            } else {
                Alertas.mostrarError("Error de BD", "No se pudieron actualizar todos los precios.");
            }
        } catch (NumberFormatException e) {
            Alertas.mostrarError("Error de Formato", "Asegúrate de ingresar números válidos para los precios.");
        }
    }

    @FXML
    private void handleGuardarParametros() {
        boolean allOk = true;
        for (String paramName : paramMap.keySet()) {
            TextField field = paramMap.get(paramName);
            try {
                double valor = Double.parseDouble(field.getText());
                if (!configDao.updateParametro(paramName, valor)) {
                    allOk = false;
                }
            } catch (NumberFormatException e) {
                Alertas.mostrarError("Error de Formato", "El valor para " + paramName + " no es un número válido.");
                return;
            }
        }

        if (allOk) {
            Alertas.mostrarInformacion("Éxito", "Todos los parámetros de configuración se guardaron correctamente.");
        } else {
            Alertas.mostrarError("Error de BD", "Ocurrió un error al guardar uno o más parámetros.");
        }
    }
}


