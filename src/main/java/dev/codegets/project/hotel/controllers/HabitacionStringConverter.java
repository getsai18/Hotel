package dev.codegets.project.hotel.controllers;

import dev.codegets.project.hotel.models.Habitacion;
import javafx.util.StringConverter;
import java.util.List;


public class HabitacionStringConverter extends StringConverter<Habitacion> {
    private final List<Habitacion> habitaciones;

    public HabitacionStringConverter(List<Habitacion> habitaciones) {
        this.habitaciones = habitaciones;
    }

    // Método llamado por el ComboBox para mostrar el objeto como texto
    @Override
    public String toString(Habitacion habitacion) {
        if (habitacion == null) {
            return null;
        }
        // Mostrar el número y el tipo de la habitación
        return "Hab. " + habitacion.getNumero() + " (" + habitacion.getTipo() + " - " + String.format("%.2f", habitacion.getPrecioBase()) + ")";
    }

    // Método llamado por el ComboBox para encontrar el objeto Habitacion
    // basándose en el texto seleccionado (necesario si editas el texto directamente, aunque raro en ComboBox)
    @Override
    public Habitacion fromString(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        // Aquí buscamos el objeto Habitacion basándonos en el número que se muestra
        try {
            // Extraer el número de habitación del string (asumiendo que está al inicio)
            String numStr = string.substring(5, string.indexOf('(')).trim();
            int numero = Integer.parseInt(numStr);

            return habitaciones.stream()
                    .filter(h -> h.getNumero() == numero)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            // Manejar errores si el formato del string es incorrecto
            return null;
        }
    }

}
