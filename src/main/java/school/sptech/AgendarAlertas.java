package school.sptech;

import school.sptech.config.AppConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgendarAlertas {

    private static final long INTERVALO_PADRAO_MIN = 5;

    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                runnable -> new Thread(runnable, "alertas-periodicos"));

        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown, "alertas-shutdown"));

        long intervaloMin = intervaloMin();
        System.out.println("Rotina de alertas: primeira execução imediata, repetindo a cada "
                + intervaloMin + " minuto(s)");

        executor.scheduleWithFixedDelay(() -> {
            try {
                Main.executarUmaVez();
            } catch (RuntimeException e) {
                System.err.println("Falha na rotina de alertas: " + e.getMessage());
            }
        }, 0, intervaloMin, TimeUnit.MINUTES);
    }

    private static long intervaloMin() {
        String valor = AppConfig.getOuPadrao("ALERTA_INTERVALO_MIN", String.valueOf(INTERVALO_PADRAO_MIN));
        try {
            long minutos = Long.parseLong(valor.trim());
            if (minutos < 1) {
                throw new NumberFormatException();
            }
            return minutos;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("ALERTA_INTERVALO_MIN deve ser um inteiro >= 1: " + valor, e);
        }
    }
}
