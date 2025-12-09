package dev.codegets.project.hotel.utils;



import dev.codegets.project.hotel.models.Configuracion;
import dev.codegets.project.hotel.models.Habitacion;
import dev.codegets.project.hotel.models.dao.ConfiguracionDao;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

public class CalculoReserva {
    private static final ConfiguracionDao configDao = new ConfiguracionDao();


    public static double calcularMontoTotal(Habitacion habitacion, LocalDateTime fechaInicio, LocalDateTime fechaFin, boolean pagaAnticipada) {

        // 1. Calcular días de estancia
        // Usamos días completos (ej: 3:00 PM Lunes a 3:00 PM Martes = 1 día)
        // La duración es hasta el checkout estándar (3:00 PM del día siguiente)
        Duration duration = Duration.between(fechaInicio, fechaFin);
        long days = duration.toDays();

        // Si la diferencia es menor a un día, cobramos 1 día.
        if (days == 0 && duration.toHours() > 0) {
            days = 1;
        } else if (days == 0 && duration.toHours() == 0) {
            return 0.00; // No se permiten reservas de 0 horas.
        }

        double montoBase = days * habitacion.getPrecioBase();
        double descuento = 0;

        // 2. Aplicar descuento por pago anticipado
        if (pagaAnticipada) {
            Optional<Configuracion> descConfig = configDao.getParametro("PORCENTAJE_DESCUENTO_ANTICIPADA");
            if (descConfig.isPresent()) {
                double porcentaje = descConfig.get().getValor();
                descuento = montoBase * porcentaje;
                System.out.println("Descuento anticipado aplicado: " + descuento);
            }
        }

        return montoBase - descuento;
    }


    public static double calcularRecargoCheckInTarde(LocalDate fechaReserva, LocalTime horaLlegada, double montoTotalReserva) {
        Optional<Configuracion> horaConfig = configDao.getParametro("HORA_CHECKIN");
        Optional<Configuracion> recargoConfig = configDao.getParametro("PORCENTAJE_RECARGO_CHECKIN_TARDE");

        if (horaConfig.isEmpty() || recargoConfig.isEmpty()) {
            Alertas.mostrarError("Error de Configuración", "Parámetros de Check-in no definidos por el Administrador.");
            return 0.00;
        }

        LocalDate fechaActual = LocalDate.now();
        LocalTime horaEstandar = LocalTime.of((int) horaConfig.get().getValor(), (int) Math.round((horaConfig.get().getValor() - (int) horaConfig.get().getValor()) * 60));

        // 1. Si la llegada es ANTES del día reservado (ej., llega el 5 para el 8), NO hay recargo por retraso.
        if (fechaActual.isBefore(fechaReserva)) {
            return 0.00;
        }

        // 2. Si la llegada es el DÍA RESERVADO (o después), aplicamos la lógica de retraso por hora.
        if (fechaActual.isEqual(fechaReserva)) {

            // Aplica recargo solo si es el día correcto Y la hora es posterior a la estándar (15:00).
            if (horaLlegada.isAfter(horaEstandar)) {
                return montoTotalReserva * recargoConfig.get().getValor();
            }
        }

        // Nota: Las llegadas en días posteriores (No-Show) se manejan con un estado diferente
        // y se penalizan por el módulo de Check-out, no con este recargo simple.

        return 0.00;
    }

    public static boolean esReservaAnticipada(boolean pagoAlReservar) {
        return pagoAlReservar;
    }

    /**
     * Convierte un valor decimal de hora (HH.MM) a LocalTime.
     */
    public static LocalTime getHoraEstandar(String paramName) {
        Optional<Configuracion> config = configDao.getParametro(paramName);
        if (config.isEmpty()) return null;

        double horaStd = config.get().getValor();
        int horaStdInt = (int) horaStd;
        int minutoStdInt = (int) Math.round((horaStd - horaStdInt) * 60);

        return LocalTime.of(horaStdInt, minutoStdInt);
    }



    public static double calcularCostoAnticipado(LocalDate fechaLlegadaReal, LocalDate fechaInicioReserva, double precioBaseNoche) {
        if (!fechaLlegadaReal.isBefore(fechaInicioReserva)) {
            return 0.0;
        }

        long nochesAnticipadas = java.time.temporal.ChronoUnit.DAYS.between(fechaLlegadaReal, fechaInicioReserva);

        // El check-in ocurre usualmente a las 3:00 PM del día reservado.
        // Si llega el día 2 para el día 4, son 2 noches extra (noche del 2 al 3, y del 3 al 4).
        return nochesAnticipadas * precioBaseNoche;
    }

}
