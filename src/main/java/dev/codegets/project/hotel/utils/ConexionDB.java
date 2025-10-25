package dev.codegets.project.hotel.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {
    private static final String URL = "jdbc:mysql://localhost:3306/hotel_tps";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        try {

            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver de MySQL.");
            throw new SQLException("Falta el driver de MySQL: " + e.getMessage());
        }
    }

    // metodo agregado por mi xd
    public static void main(String[] args) {
        try(Connection conn = getConnection()){
            if(conn != null && !conn.isClosed()){
                System.out.println("Coneccion exitosa");
            }else{
                System.out.println("Coneccion fallida");
            }

        }catch(SQLException e){
            e.printStackTrace();
        }
    }


}
