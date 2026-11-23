package school.sptech.service;

import school.sptech.config.AppConfig;
import school.sptech.service.RelatorioDiarioService.ResultadoPublicacao;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RelatorioAgendamentoService implements AutoCloseable {

    static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final LocalTime HORA_PUBLICACAO = LocalTime.of(1, 0);
    private static final int LOOKBACK_PADRAO = 3;

    private final RelatorioDiarioService relatorios;
    private final ScheduledExecutorService executor;
    private final int lookbackDias;

    public RelatorioAgendamentoService() {
        this(new RelatorioDiarioService(), criarExecutor(), lookbackDiasDoAmbiente());
    }

    RelatorioAgendamentoService(RelatorioDiarioService relatorios,
                                ScheduledExecutorService executor,
                                int lookbackDias) {
        if (lookbackDias < 1) {
            throw new IllegalArgumentException("lookbackDias deve ser >= 1");
        }
        this.relatorios = relatorios;
        this.executor = executor;
        this.lookbackDias = lookbackDias;
    }

    public void iniciar() {
        agendarProxima();
    }

    public ResultadoPublicacao publicarDia(LocalDate dia) {
        return relatorios.publicarTodas(dia);
    }

    public ResultadoPublicacao publicarUltimosDias() {
        int publicados = 0;
        int pulados = 0;
        int falhas = 0;
        LocalDate hoje = LocalDate.now(ZONA);
        try (RelatorioS3Service destino = new RelatorioS3Service()) {
            for (int diasAtras = lookbackDias; diasAtras >= 1; diasAtras--) {
                LocalDate dia = hoje.minusDays(diasAtras);
                try {
                    ResultadoPublicacao resultado = relatorios.publicarTodas(dia, destino);
                    publicados += resultado.publicados();
                    pulados += resultado.pulados();
                    falhas += resultado.falhas();
                    System.out.println("Relatórios de " + dia + ": " + resultado);
                } catch (RuntimeException e) {
                    falhas++;
                    System.err.println("Falha nos relatórios de " + dia + ": " + e.getMessage());
                }
            }
        }
        return new ResultadoPublicacao(publicados, pulados, falhas);
    }

    static ZonedDateTime proximaExecucao(ZonedDateTime agora) {
        ZonedDateTime candidata = agora.toLocalDate().atTime(HORA_PUBLICACAO).atZone(ZONA);
        return candidata.isAfter(agora) ? candidata : candidata.plusDays(1);
    }

    private void agendarProxima() {
        if (executor.isShutdown()) {
            return;
        }
        ZonedDateTime agora = ZonedDateTime.now(ZONA);
        ZonedDateTime proxima = proximaExecucao(agora);
        System.out.println("Próxima execução dos relatórios: " + proxima);
        long atrasoMillis = Math.max(1, Duration.between(agora, proxima).toMillis());
        executor.schedule(() -> {
            try {
                System.out.println("Executando rotina diária de relatórios...");
                publicarUltimosDias();
            } catch (RuntimeException e) {
                System.err.println("Falha na rotina diária de relatórios: " + e.getMessage());
            } finally {
                if (!executor.isShutdown()) {
                    agendarProxima();
                }
            }
        }, atrasoMillis, TimeUnit.MILLISECONDS);
    }

    private static ScheduledExecutorService criarExecutor() {
        return Executors.newSingleThreadScheduledExecutor(
                runnable -> new Thread(runnable, "relatorios-diarios"));
    }

    private static int lookbackDiasDoAmbiente() {
        String valor = AppConfig.getOuPadrao("RELATORIO_LOOKBACK_DIAS", String.valueOf(LOOKBACK_PADRAO));
        try {
            int dias = Integer.parseInt(valor.trim());
            if (dias < 1) {
                throw new NumberFormatException();
            }
            return dias;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("RELATORIO_LOOKBACK_DIAS deve ser um inteiro >= 1: " + valor, e);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
