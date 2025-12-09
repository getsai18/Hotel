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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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

        if (nombre.isEmpty() || username.isEmpty()) {
            Alertas.mostrarError("Error de Campos", "Nombre y Usuario son obligatorios.");
            return;
        }

        boolean existeGerente = usuarioDao.existsGerente();

        if (!existeGerente) {
            if (rawPassword.isEmpty()) {
                Alertas.mostrarError("Error de Campos", "Contraseña requerida para crear el Gerente.");
                return;
            }
            String hashed = SecurityUtils.hashPassword(rawPassword);
            if (hashed == null) return;
            Usuario nuevo = new Usuario(0, nombre, username, hashed, "GERENTE");
            if (usuarioDao.create(nuevo)) {
                Alertas.mostrarInformacion("Éxito", "Gerente '" + username + "' creado correctamente.");
                txtGerenteNombre.clear();
                txtGerenteUsername.clear();
                txtGerentePassword.clear();
                cargarEstadoInicial();
            } else {
                Alertas.mostrarError("Error de BD", "No se pudo crear el Gerente.");
            }
            return;
        }

        Optional<Usuario> actualOpt = usuarioDao.getGerente();
        if (actualOpt.isEmpty()) {
            Alertas.mostrarError("Error", "No se pudo obtener el Gerente actual.");
            return;
        }
        Usuario actual = actualOpt.get();

        String hashedToUse;
        if (rawPassword.isEmpty()) {
            hashedToUse = actual.getPassword();
        } else {
            String hashed = SecurityUtils.hashPassword(rawPassword);
            if (hashed == null) return;
            hashedToUse = hashed;
        }

        Usuario actualizado = new Usuario(actual.getIdUsuario(), nombre, username, hashedToUse, "GERENTE");
        if (usuarioDao.updateGerente(actualizado)) {
            Alertas.mostrarInformacion("Éxito", "Gerente actualizado correctamente.");
            txtGerenteNombre.clear();
            txtGerenteUsername.clear();
            txtGerentePassword.clear();
            cargarEstadoInicial();
        } else {
            Alertas.mostrarError("Error de BD", "No se pudo actualizar el Gerente.");
        }
    }

    @FXML
    private void handleCargarGerente() {
        java.util.Optional<Usuario> gerenteOpt = usuarioDao.getGerente();
        if (gerenteOpt.isEmpty()) {
            Alertas.mostrarError("Error", "No existe un gerente para editar.");
            return;
        }
        Usuario g = gerenteOpt.get();
        txtGerenteNombre.setText(g.getNombre());
        txtGerenteUsername.setText(g.getUsername());
        txtGerentePassword.clear();
    }

    @FXML
    private void handleEliminarGerente() {
        if (!usuarioDao.existsGerente()) {
            Alertas.mostrarError("Error", "No existe un gerente para eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar gerente actual?", ButtonType.YES, ButtonType.NO);
        java.util.Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            if (usuarioDao.deleteGerente()) {
                Alertas.mostrarInformacion("Éxito", "Gerente eliminado.");
                txtGerenteNombre.clear();
                txtGerenteUsername.clear();
                txtGerentePassword.clear();
                cargarEstadoInicial();
            } else {
                Alertas.mostrarError("Error de BD", "No se pudo eliminar el Gerente.");
            }
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


