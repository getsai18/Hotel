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

    /**
     * Calcula el monto total de una reserva.
     * @param habitacion La habitación seleccionada.
     * @param fechaInicio Fecha y hora de inicio de la estancia.
     * @param fechaFin Fecha y hora de fin de la estancia.
     * @param pagaAnticipada Indica si se aplica el descuento por reserva anticipada.
     * @return Monto total calculado.
     */
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

    /**
     * Valida si la hora de check-in actual aplica recargo.
     * @param fechaReserva La fecha de inicio de la reserva (solo día).
     * @param horaLlegada La hora real en que llegó el cliente.
     * @return Recargo a aplicar (0.00 si no aplica).
     */
    public static double calcularRecargoCheckInTarde(LocalDate fechaReserva, LocalTime horaLlegada, double montoBasePorNoche) {
        Optional<Configuracion> horaConfig = configDao.getParametro("HORA_CHECKIN");
        Optional<Configuracion> recargoConfig = configDao.getParametro("PORCENTAJE_RECARGO_CHECKIN_TARDE");

        if (horaConfig.isEmpty() || recargoConfig.isEmpty()) {
            Alertas.mostrarError("Error de Configuración", "Parámetros de Check-in no definidos por el Administrador.");
            return 0.00;
        }

        double horaStd = horaConfig.get().getValor(); // Ej: 15.00
        int horaStdInt = (int) horaStd;
        int minutoStdInt = (int) ((horaStd - horaStdInt) * 60);

        LocalTime horaEstandar = LocalTime.of(horaStdInt, minutoStdInt);

        // La penalización aplica si la llegada es en un día posterior al check-in
        // O si la hora de llegada es posterior a la hora estándar
        if (horaLlegada.isAfter(horaEstandar)) {
            // El recargo se aplica sobre el precio base de la noche
            return montoBasePorNoche * recargoConfig.get().getValor();
        }

        return 0.00;
    }

    /**
     * Determina si la reserva es anticipada (para descuento).
     * Podríamos definirlo como "más de 7 días antes de la fecha de check-in".
     * Usamos una lógica simple: si el cliente paga en el momento de reservar.
     * En este caso, simplemente devolvemos el valor 'pagaAnticipada' proporcionado por el usuario.
     */
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

}
