package dev.codegets.project.hotel.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecurityUtils {
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            // Asegúrate que Alertas esté accesible o usa System.err.println si no puedes importarlo
            // Alertas.mostrarError("Error de Seguridad", "El algoritmo de hasheo SHA-256 no está disponible.");
            return null;
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        // CAMBIO CLAVE 1: Aseguramos que el hash generado esté siempre en minúsculas.
        return hexString.toString().toLowerCase();
    }


    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        String newHash = hashPassword(rawPassword);

        // CAMBIO CLAVE 2: Comparamos el nuevo hash con el hash almacenado,
        // convirtiendo el hash almacenado a minúsculas para asegurar que el case no falle.
        return newHash != null && newHash.equals(hashedPassword.toLowerCase());
    }

}
