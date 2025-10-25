package dev.codegets.project.hotel.models.dao;


import dev.codegets.project.hotel.models.Habitacion;
import dev.codegets.project.hotel.utils.ConexionDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HabitacionDao {

    public void inicializarHabitaciones() {
        if (countHabitaciones() == 0) {
            System.out.println("Inicializando 22 habitaciones...");
            String sql = "INSERT INTO habitaciones (numero, tipo, estado, precio_base) VALUES (?, ?, 'DISPONIBLE', ?)";

            try (Connection conn = ConexionDB.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                // 5 MASTER (Usaremos números del 101 al 105)
                double precioMaster = 150.00; // Precio inicial de ejemplo
                for (int i = 1; i <= 5; i++) {
                    stmt.setInt(1, 100 + i);
                    stmt.setString(2, "MASTER");
                    stmt.setDouble(3, precioMaster);
                    stmt.addBatch();
                }

                // 17 ESTANDAR (Usaremos números del 201 al 217)
                double precioEstandar = 80.00; // Precio inicial de ejemplo
                for (int i = 1; i <= 17; i++) {
                    stmt.setInt(1, 200 + i);
                    stmt.setString(2, "ESTANDAR");
                    stmt.setDouble(3, precioEstandar);
                    stmt.addBatch();
                }

                stmt.executeBatch();
                System.out.println("22 habitaciones inicializadas correctamente.");

            } catch (SQLException e) {
                System.err.println("Error al inicializar habitaciones: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private int countHabitaciones() {
        String sql = "SELECT COUNT(*) FROM habitaciones";
        try (Connection conn = ConexionDB.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Habitacion> getAll() {
        List<Habitacion> habitaciones = new ArrayList<>();
        String sql = "SELECT * FROM habitaciones";
        try (Connection conn = ConexionDB.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                habitaciones.add(new Habitacion(
                        rs.getInt("id_habitacion"),
                        rs.getInt("numero"),
                        rs.getString("tipo"),
                        rs.getString("estado"),
                        rs.getDouble("precio_base")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return habitaciones;
    }


    public boolean updatePrecioBase(String tipo, double nuevoPrecio) {
        String sql = "UPDATE habitaciones SET precio_base = ? WHERE tipo = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, nuevoPrecio);
            stmt.setString(2, tipo);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    public Optional<Habitacion> getById(int id) {
        String sql = "SELECT * FROM habitaciones WHERE id_habitacion = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Habitacion(
                            rs.getInt("id_habitacion"),
                            rs.getInt("numero"),
                            rs.getString("tipo"),
                            rs.getString("estado"),
                            rs.getDouble("precio_base")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Actualiza el estado de una habitación (usado en Check-in/Check-out/Mantenimiento).
     */
    public boolean updateEstado(int idHabitacion, String nuevoEstado) {
        String sql = "UPDATE habitaciones SET estado = ? WHERE id_habitacion = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idHabitacion);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<Habitacion> getByNumero(int numero) {
        String sql = "SELECT * FROM habitaciones WHERE numero = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, numero);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Habitacion(
                            rs.getInt("id_habitacion"),
                            rs.getInt("numero"),
                            rs.getString("tipo"),
                            rs.getString("estado"),
                            rs.getDouble("precio_base")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }


}
