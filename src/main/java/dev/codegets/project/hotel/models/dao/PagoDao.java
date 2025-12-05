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

    // src/models/dao/PagoDao.java (Método Final)

    /**
     * Calcula la suma neta total de pagos (Ingresos - Egresos/Reembolsos)
     * realizados para una reserva específica.
     */
    // src/models/dao/PagoDao.java

    /**
     * Calcula SOLO los pagos abonados al costo base de la reserva (Tipo 'RESERVA').
     * Ignora recargos y penalizaciones porque son costos adicionales.
     */
    public double getTotalPagadoByReserva(int idReserva) {
        // CORRECCIÓN: Solo sumar pagos de tipo 'RESERVA'
        String sql = "SELECT SUM(monto) FROM pagos WHERE id_reserva = ? AND tipo = 'RESERVA'";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idReserva);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

}
