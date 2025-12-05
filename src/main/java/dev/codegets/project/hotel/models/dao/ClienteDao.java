package dev.codegets.project.hotel.models.dao;


import dev.codegets.project.hotel.models.Cliente;
import dev.codegets.project.hotel.utils.ConexionDB;

import java.sql.*;
import java.util.Optional;

public class ClienteDao {
    /**
     * Busca o crea un cliente. Esto es útil para Reservas/Check-in.
     * Si el cliente existe (por nombre o correo), lo devuelve.
     * Si no, lo crea y lo devuelve.
     */
    public Optional<Cliente> findOrCreate(String nombre, String telefono, String correo) {
        // 1. Intentar buscar por correo/nombre
        String findSql = "SELECT * FROM clientes WHERE correo = ? OR nombre = ? LIMIT 1";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement findStmt = conn.prepareStatement(findSql)) {

            findStmt.setString(1, correo);
            findStmt.setString(2, nombre);

            try (ResultSet rs = findStmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Cliente(
                            rs.getInt("id_cliente"),
                            rs.getString("nombre"),
                            rs.getString("telefono"),
                            rs.getString("correo")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        // 2. Si no existe, crear uno nuevo
        String insertSql = "INSERT INTO clientes (nombre, telefono, correo) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            insertStmt.setString(1, nombre);
            insertStmt.setString(2, telefono);
            insertStmt.setString(3, correo);

            int rows = insertStmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        return Optional.of(new Cliente(id, nombre, telefono, correo));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty(); // Falla si no se encuentra ni se puede crear
    }


    public Optional<Cliente> getById(int idCliente) {
        String sql = "SELECT * FROM clientes WHERE id_cliente = ? LIMIT 1";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Cliente(
                            rs.getInt("id_cliente"),
                            rs.getString("nombre"),
                            rs.getString("telefono"),
                            rs.getString("correo")
                    ));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }



}
