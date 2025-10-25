package dev.codegets.project.hotel.models;

public class Habitacion {
    private int idHabitacion;
    private int numero; // Número de habitación (ej: 101, 205)
    private String tipo; // 'MASTER' o 'ESTANDAR'
    private String estado; // 'DISPONIBLE', 'OCUPADA', 'RESERVADA', 'MANTENIMIENTO'
    private double precioBase;

    // Constructor completo
    public Habitacion(int idHabitacion, int numero, String tipo, String estado, double precioBase) {
        this.idHabitacion = idHabitacion;
        this.numero = numero;
        this.tipo = tipo;
        this.estado = estado;
        this.precioBase = precioBase;
    }

    // Getters y Setters
    public int getIdHabitacion() { return idHabitacion; }
    public void setIdHabitacion(int idHabitacion) { this.idHabitacion = idHabitacion; }
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public double getPrecioBase() { return precioBase; }
    public void setPrecioBase(double precioBase) { this.precioBase = precioBase; }

}
