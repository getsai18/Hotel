package dev.codegets.project.hotel.models.dao;

import dev.codegets.project.hotel.models.Configuracion;
import dev.codegets.project.hotel.utils.ConexionDB;

import java.sql.*;
import java.util.Optional;


public class ConfiguracionDao {
    /**
     * Inicializa los parámetros de configuración si no existen.
     */
    public void inicializarParametros() {
        // Parámetros obligatorios según los requisitos:
        String[] params = {
                "PORCENTAJE_RECARGO_CHECKIN_TARDE", // Porcentaje por llegada después del check-in (ej: 0.05 = 5%)
                "PORCENTAJE_PENALIZACION_NO_SHOW",    // Porcentaje por no presentarse (ej: 0.50 = 50% de penalización)
                "PORCENTAJE_DESCUENTO_ANTICIPADA",  // Porcentaje por reserva anticipada (ej: 0.10 = 10% de descuento)
                "HORA_CHECKIN",                     // Hora estándar de check-in (ej: 15.00 = 3:00 PM)
                "HORA_CHECKOUT"                     // Hora estándar de check-out (ej: 15.00 = 3:00 PM del día siguiente)
        };

        // Valores iniciales (se podrán cambiar en Fase 4 por el Admin)
        double[] valores = {0.05, 0.50, 0.10, 15.00, 15.00};

        String checkSql = "SELECT COUNT(*) FROM configuracion WHERE nombre_parametro = ?";
        String insertSql = "INSERT INTO configuracion (nombre_parametro, valor) VALUES (?, ?)";

        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false); // Iniciar Transacción (TPS)

            for (int i = 0; i < params.length; i++) {
                // 1. Verificar si ya existe
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, params[i]);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) == 0) {
                        // 2. Insertar si no existe
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, params[i]);
                            insertStmt.setDouble(2, valores[i]);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
            conn.commit();
            System.out.println("Parámetros de configuración inicializados/verificados.");

        } catch (SQLException e) {
            System.err.println("Error al inicializar configuración: " + e.getMessage());
            try {
                // Rollback en caso de error
                ConexionDB.getConnection().rollback();
            } catch (SQLException rollbackEx) {
                // Ignorar
            }
        }
    }

    /**
     * Obtiene el valor de un parámetro específico.
     */
    public Optional<Configuracion> getParametro(String nombre) {
        String sql = "SELECT * FROM configuracion WHERE nombre_parametro = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nombre);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Configuracion(
                            rs.getInt("id_config"),
                            rs.getString("nombre_parametro"),
                            rs.getDouble("valor")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // Método para actualizar (usado por el Administrador en Fase 4)
    public boolean updateParametro(String nombre, double nuevoValor) {
        String sql = "UPDATE configuracion SET valor = ? WHERE nombre_parametro = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, nuevoValor);
            stmt.setString(2, nombre);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
