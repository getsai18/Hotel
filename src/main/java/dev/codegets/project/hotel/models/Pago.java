package dev.codegets.project.hotel.models;
import java.time.LocalDateTime;
public class Pago {
    private int idPago;
    private Integer idReserva; // Nullable
    private double monto;
    private String tipo; // 'RESERVA', 'RECARGO', 'PENALIZACION', 'REEMBOLSO', 'DIA_EXTRA'
    private LocalDateTime fechaPago;
    private String descripcion;

    // Constructor
    public Pago(int idPago, Integer idReserva, double monto, String tipo, LocalDateTime fechaPago, String descripcion) {
        this.idPago = idPago;
        this.idReserva = idReserva;
        this.monto = monto;
        this.tipo = tipo;
        this.fechaPago = fechaPago;
        this.descripcion = descripcion;
    }

    public int getIdPago() {
        return idPago;
    }

    public void setIdPago(int idPago) {
        this.idPago = idPago;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
