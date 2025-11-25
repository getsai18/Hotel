package dev.codegets.project.hotel.controllers;

import dev.codegets.project.hotel.models.Usuario;
import dev.codegets.project.hotel.models.dao.UsuarioDao;
import dev.codegets.project.hotel.utils.Alertas;
import dev.codegets.project.hotel.utils.SecurityUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;

    private UsuarioDao usuarioDao = new UsuarioDao();

    @FXML
    public void initialize() {
        // Configurar Enter en el campo de usuario: pasa el foco al campo de contraseña
        txtUsername.setOnAction(e -> txtPassword.requestFocus());
        
        // Configurar Enter para iniciar sesión solo desde el campo de contraseña
        txtPassword.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            Alertas.mostrarError("Error de Login", "Por favor, ingresa usuario y contraseña.");
            return;
        }

        Usuario usuario = usuarioDao.getByUsername(username);

        if (usuario != null && SecurityUtils.verifyPassword(password, usuario.getPassword())) {
            // Autenticación exitosa
            Alertas.mostrarInformacion("Login Exitoso", "Bienvenido/a al sistema, " + usuario.getNombre() + " (" + usuario.getRol() + ").");

            // Cargar la ventana principal
            loadMenuPrincipal(usuario);

            // Cerrar la ventana de login
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.close();

        } else {
            // Credenciales inválidas
            Alertas.mostrarError("Error de Login", "Usuario o contraseña incorrectos. Inténtalo de nuevo.");
            txtPassword.clear();
        }
    }

    /**
     * Carga el menú principal y pasa el objeto Usuario.
     */
    private void loadMenuPrincipal(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dev/codegets/project/hotel/fxml/menuPrincipal.fxml"));
            Parent root = loader.load();

            // Pasar el usuario al controlador del menú principal
            MenuPrincipalController controller = loader.getController();
            controller.setUsuarioActual(usuario); // Este método lo crearemos en la Fase 3

            Stage stage = new Stage();
            stage.setTitle("Sistema de Gestión Hotelera - " + usuario.getRol());
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            Alertas.mostrarError("Error de Aplicación", "No se pudo cargar la ventana principal.");
            e.printStackTrace();
        }
    }
}
