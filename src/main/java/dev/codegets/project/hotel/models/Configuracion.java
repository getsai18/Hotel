package dev.codegets.project.hotel.models;

public class Configuracion {
    private int idConfig;
    private String nombreParametro;
    private double valor;

    // Constructor
    public Configuracion(int idConfig, String nombreParametro, double valor) {
        this.idConfig = idConfig;
        this.nombreParametro = nombreParametro;
        this.valor = valor;
    }

    // Getters y Setters
    public int getIdConfig() { return idConfig; }
    public void setIdConfig(int idConfig) { this.idConfig = idConfig; }
    public String getNombreParametro() { return nombreParametro; }
    public void setNombreParametro(String nombreParametro) { this.nombreParametro = nombreParametro; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

}
