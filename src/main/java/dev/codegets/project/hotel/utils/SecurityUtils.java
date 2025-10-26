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
            System.out.println("si entre al try xd");
            return bytesToHex(encodedhash);

        } catch (NoSuchAlgorithmException e) {

            // Asegúrate que Alertas esté accesible o usa System.err.println si no puedes importarlo
            System.err.println("Error FATAL de seguridad: SHA-256 no disponible.");
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

        // --- LINEAS DE DEBUGGING ---
        System.out.println("--- DEBUG: VERIFICACIÓN DE CONTRASEÑA ---");
        System.out.println("Hash GENERADO (Length: " + newHash.length() + "): " + newHash);
        System.out.println("Hash DB LEÍDO (Length: " + hashedPassword.length() + "): " + hashedPassword);

        String cleanDBHash = hashedPassword.trim().toLowerCase();
        System.out.println("Hash DB LIMPIO (Length: " + cleanDBHash.length() + "): " + cleanDBHash);
        System.out.println("¿Coinciden los hashes?: " + newHash.equals(cleanDBHash));
        System.out.println("------------------------------------------");
        // ----------------------------

        return newHash != null && newHash.equals(cleanDBHash);
    }



}
