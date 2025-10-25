package dev.codegets.project.hotel.models;

import java.time.LocalDateTime;
public class Reserva {
    private int idReserva;
    private int idCliente;
    private int idHabitacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String estado; // 'ACTIVA', 'CANCELADA', 'FINALIZADA', 'NO_SHOW'
    private double montoTotal;
    private LocalDateTime checkinReal;
    private LocalDateTime checkoutReal;

    // Constructor completo (usado para cargar desde DB)
    public Reserva(int idReserva, int idCliente, int idHabitacion, LocalDateTime fechaInicio,
                   LocalDateTime fechaFin, String estado, double montoTotal,
                   LocalDateTime checkinReal, LocalDateTime checkoutReal) {
        this.idReserva = idReserva;
        this.idCliente = idCliente;
        this.idHabitacion = idHabitacion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.montoTotal = montoTotal;
        this.checkinReal = checkinReal;
        this.checkoutReal = checkoutReal;
    }

    // Constructor para crear nueva reserva
    public Reserva(int idCliente, int idHabitacion, LocalDateTime fechaInicio, LocalDateTime fechaFin, double montoTotal) {
        this.idCliente = idCliente;
        this.idHabitacion = idHabitacion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = "ACTIVA";
        this.montoTotal = montoTotal;
    }

    // Getters y Setters
    public int getIdReserva() { return idReserva; }
    public void setIdReserva(int idReserva) { this.idReserva = idReserva; }
    public int getIdCliente() { return idCliente; }
    public int getIdHabitacion() { return idHabitacion; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public double getMontoTotal() { return montoTotal; }
    public void setMontoTotal(double montoTotal) { this.montoTotal = montoTotal; }
    public LocalDateTime getCheckinReal() { return checkinReal; }
    public void setCheckinReal(LocalDateTime checkinReal) { this.checkinReal = checkinReal; }
    public LocalDateTime getCheckoutReal() { return checkoutReal; }
    public void setCheckoutReal(LocalDateTime checkoutReal) { this.checkoutReal = checkoutReal; }

}
