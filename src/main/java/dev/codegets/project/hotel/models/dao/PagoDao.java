package dev.codegets.project.hotel.models.dao;


import dev.codegets.project.hotel.models.Pago;
import dev.codegets.project.hotel.utils.ConexionDB;

import java.sql.*;
import java.time.LocalDateTime;


public class PagoDao {
    /**
     * Registra un nuevo pago en la base de datos.
     */
    public boolean create(Pago pago) {
        String sql = "INSERT INTO pagos (id_reserva, monto, tipo, fecha_pago, descripcion) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // idReserva puede ser null (ej: pago de penalización directa), por eso usamos setObject
            stmt.setObject(1, pago.getIdReserva(), Types.INTEGER);
            stmt.setDouble(2, pago.getMonto());
            stmt.setString(3, pago.getTipo());
            stmt.setTimestamp(4, Timestamp.valueOf(pago.getFechaPago() != null ? pago.getFechaPago() : LocalDateTime.now()));
            stmt.setString(5, pago.getDescripcion());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
