package dev.codegets.project.hotel.models;

public class Usuario {
    private int idUsuario;
    private String nombre;
    private String username;
    private String password; // Se almacena el hash
    private String rol; // 'ADMIN' o 'GERENTE'

    // Constructor
    public Usuario(int idUsuario, String nombre, String username, String password, String rol) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.username = username;
        this.password = password;
        this.rol = rol;
    }

    // Getters y Setters
    public int getIdUsuario() { return idUsuario; }
    public String getNombre() { return nombre; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRol() { return rol; }

    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRol(String rol) { this.rol = rol; }

}
