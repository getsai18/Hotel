package dev.codegets.project.hotel.models.dao;

import dev.codegets.project.hotel.models.Reserva;
import dev.codegets.project.hotel.utils.ConexionDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ReservaDao {
    /**
     * Guarda una nueva reserva y actualiza el ID generado.
     * @return El ID de la reserva creada.
     */
    public Optional<Integer> create(Reserva reserva) {
        String sql = "INSERT INTO reservas (id_cliente, id_habitacion, fecha_inicio, fecha_fin, estado, monto_total) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, reserva.getIdCliente());
            stmt.setInt(2, reserva.getIdHabitacion());
            stmt.setTimestamp(3, Timestamp.valueOf(reserva.getFechaInicio()));
            stmt.setTimestamp(4, Timestamp.valueOf(reserva.getFechaFin()));
            stmt.setString(5, reserva.getEstado());
            stmt.setDouble(6, reserva.getMontoTotal());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        reserva.setIdReserva(generatedKeys.getInt(1));
                        return Optional.of(reserva.getIdReserva());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Verifica si una habitación está disponible en un rango de fechas.
     */
    public boolean isHabitacionAvailable(int idHabitacion, LocalDateTime start, LocalDateTime end) {
        // Busca reservas (ACTIVA) u ocupaciones que se superpongan con el rango dado.
        String sql = "SELECT COUNT(*) FROM reservas WHERE id_habitacion = ? AND estado IN ('ACTIVA') AND " +
                "( (? < fecha_fin) AND (? > fecha_inicio) )";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idHabitacion);
            stmt.setTimestamp(2, Timestamp.valueOf(start));
            stmt.setTimestamp(3, Timestamp.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Si el conteo es > 0, hay un conflicto.
                    return rs.getInt(1) == 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Obtiene una reserva por ID.
     */
    public Optional<Reserva> getById(int idReserva) {
        String sql = "SELECT * FROM reservas WHERE id_reserva = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idReserva);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Reserva(
                            rs.getInt("id_reserva"),
                            rs.getInt("id_cliente"),
                            rs.getInt("id_habitacion"),
                            rs.getTimestamp("fecha_inicio").toLocalDateTime(),
                            rs.getTimestamp("fecha_fin").toLocalDateTime(),
                            rs.getString("estado"),
                            rs.getDouble("monto_total"),
                            rs.getTimestamp("checkin_real") != null ? rs.getTimestamp("checkin_real").toLocalDateTime() : null,
                            rs.getTimestamp("checkout_real") != null ? rs.getTimestamp("checkout_real").toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // Método para buscar reservas activas por habitación
    public Optional<Reserva> getActiveReservaByHabitacion(int idHabitacion) {
        String sql = "SELECT * FROM reservas WHERE id_habitacion = ? AND estado = 'ACTIVA' AND checkin_real IS NULL";
        // Esta consulta busca la reserva 'ACTIVA' antes del Check-in
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idHabitacion);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Reserva(
                            rs.getInt("id_reserva"),
                            rs.getInt("id_cliente"),
                            rs.getInt("id_habitacion"),
                            rs.getTimestamp("fecha_inicio").toLocalDateTime(),
                            rs.getTimestamp("fecha_fin").toLocalDateTime(),
                            rs.getString("estado"),
                            rs.getDouble("monto_total"),
                            null, // Check-in no realizado
                            null
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // Método para buscar Ocupaciones (Reservas con Check-in hecho)
    public Optional<Reserva> getOccupiedByHabitacion(int idHabitacion) {
        String sql = "SELECT * FROM reservas WHERE id_habitacion = ? AND checkin_real IS NOT NULL AND checkout_real IS NULL";
        // Busca una reserva que ya tiene check-in, pero no check-out
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idHabitacion);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Reserva(
                            rs.getInt("id_reserva"),
                            rs.getInt("id_cliente"),
                            rs.getInt("id_habitacion"),
                            rs.getTimestamp("fecha_inicio").toLocalDateTime(),
                            rs.getTimestamp("fecha_fin").toLocalDateTime(),
                            rs.getString("estado"),
                            rs.getDouble("monto_total"),
                            rs.getTimestamp("checkin_real").toLocalDateTime(),
                            null
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Actualiza el estado de una reserva (usado para Cancelar/No-Show).
     */
    public boolean updateEstado(int idReserva, String nuevoEstado) {
        String sql = "UPDATE reservas SET estado = ? WHERE id_reserva = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idReserva);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean updateCheckIn(int idReserva, LocalDateTime checkinTime) {
        String sql = "UPDATE reservas SET checkin_real = ? WHERE id_reserva = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(checkinTime));
            stmt.setInt(2, idReserva);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<Reserva> getReservasPendientesCheckIn() {
        List<Reserva> lista = new ArrayList<>();
        // Hacemos JOIN para obtener el nombre del cliente
        String sql = "SELECT r.*, c.nombre as nombre_cliente FROM reservas r " +
                "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                "WHERE r.estado = 'ACTIVA' AND r.checkin_real IS NULL";

        try (Connection conn = ConexionDB.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Reserva r = new Reserva(
                        rs.getInt("id_reserva"),
                        rs.getInt("id_cliente"),
                        rs.getInt("id_habitacion"),
                        rs.getTimestamp("fecha_inicio").toLocalDateTime(),
                        rs.getTimestamp("fecha_fin").toLocalDateTime(),
                        rs.getString("estado"),
                        rs.getDouble("monto_total"),
                        null, null
                );
                // Asignamos el nombre del cliente recuperado
                r.setNombreCliente(rs.getString("nombre_cliente"));
                lista.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}
