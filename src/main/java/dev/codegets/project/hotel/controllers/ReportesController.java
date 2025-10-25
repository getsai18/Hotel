package dev.codegets.project.hotel.controllers;

import dev.codegets.project.hotel.models.dao.PagoDao;
import dev.codegets.project.hotel.models.dao.ReservaDao;
import dev.codegets.project.hotel.utils.Alertas;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;

import java.sql.*;
import java.time.LocalDate;

public class ReportesController {

    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private Button btnGenerarIngresos;
    @FXML private Button btnGenerarOcupacion;
    @FXML private TextArea txtReporteArea;
    @FXML private Label lblTituloReporte;

    private final PagoDao pagoDao = new PagoDao();
    private final ReservaDao reservaDao = new ReservaDao();

    @FXML
    public void initialize() {
        dpFechaInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpFechaFin.setValue(LocalDate.now());
    }

    @FXML
    private void handleGenerarIngresos() {
        lblTituloReporte.setText("Reporte de Ingresos y Penalizaciones");

        LocalDate inicio = dpFechaInicio.getValue();
        LocalDate fin = dpFechaFin.getValue().plusDays(1); // Incluir el día final completo

        String sql = "SELECT tipo, SUM(monto) as total FROM pagos WHERE fecha_pago >= ? AND fecha_pago < ? GROUP BY tipo";

        StringBuilder reporte = new StringBuilder("--- REPORTE DE INGRESOS (" + inicio + " a " + dpFechaFin.getValue() + ") ---\n\n");
        double totalIngresos = 0.0;
        double totalPenalizaciones = 0.0;

        try (Connection conn = utils.ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(fin.atStartOfDay()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String tipo = rs.getString("tipo");
                    double monto = rs.getDouble("total");
                    reporte.append(String.format(" - %s: %.2f\n", tipo, monto));

                    if (tipo.equals("RESERVA") || tipo.equals("RECARGO") || tipo.equals("DIA_EXTRA")) {
                        totalIngresos += monto;
                    } else if (tipo.equals("PENALIZACION")) {
                        totalPenalizaciones += monto;
                    }
                }
            }
            reporte.append("\n----------------------------------------\n");
            reporte.append(String.format("TOTAL BRUTO DE INGRESOS (Reservas + Recargos): %.2f\n", totalIngresos));
            reporte.append(String.format("TOTAL PENALIZACIONES (No-Show, Tardía): %.2f\n", totalPenalizaciones));

            txtReporteArea.setText(reporte.toString());

        } catch (SQLException e) {
            Alertas.mostrarError("Error de BD", "Fallo al generar el reporte de ingresos.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGenerarOcupacion() {
        lblTituloReporte.setText("Reporte de Ocupación por Habitación");

        LocalDate inicio = dpFechaInicio.getValue();
        LocalDate fin = dpFechaFin.getValue();

        // Query simple para ver cuántas reservas hubo por tipo
        String sql = "SELECT h.tipo, COUNT(r.id_reserva) as num_reservas FROM reservas r " +
                "JOIN habitaciones h ON r.id_habitacion = h.id_habitacion " +
                "WHERE r.fecha_inicio >= ? AND r.fecha_fin <= ? GROUP BY h.tipo";

        StringBuilder reporte = new StringBuilder("--- REPORTE DE OCUPACIÓN (Reservas entre " + inicio + " y " + fin + ") ---\n\n");
        int totalReservas = 0;

        try (Connection conn = utils.ConexionDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(fin.atStartOfDay().plusDays(1)));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String tipo = rs.getString("tipo");
                    int count = rs.getInt("num_reservas");
                    reporte.append(String.format(" - Habitaciones %s: %d Reservas\n", tipo, count));
                    totalReservas += count;
                }
            }
            reporte.append("\n----------------------------------------\n");
            reporte.append("TOTAL DE RESERVAS EN PERIODO: " + totalReservas);

            txtReporteArea.setText(reporte.toString());

        } catch (SQLException e) {
            Alertas.mostrarError("Error de BD", "Fallo al generar el reporte de ocupación.");
            e.printStackTrace();
        }
    }
}
