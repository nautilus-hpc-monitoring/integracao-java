package school.sptech.service;

import school.sptech.config.AppConfig;
import school.sptech.service.RelatorioDiarioService.ResultadoPublicacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RelatorioAgendamentoService implements AutoCloseable {

    static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final int LOOKBACK_PADRAO = 3;
    private static final int INTERVALO_PADRAO_MINUTOS = 1;

    private final RelatorioDiarioService relatorios;
    private final ScheduledExecutorService executor;
    private final int lookbackDias;
    private final int intervaloMinutos;

    public RelatorioAgendamentoService() {
        this(new RelatorioDiarioService(), criarExecutor(),
                lookbackDiasDoAmbiente(), intervaloMinutosDoAmbiente());
    }

    RelatorioAgendamentoService(RelatorioDiarioService relatorios,
                                ScheduledExecutorService executor,
                                int lookbackDias) {
        this(relatorios, executor, lookbackDias, INTERVALO_PADRAO_MINUTOS);
    }

    RelatorioAgendamentoService(RelatorioDiarioService relatorios,
                                ScheduledExecutorService executor,
                                int lookbackDias,
                                int intervaloMinutos) {
        if (lookbackDias < 1) {
            throw new IllegalArgumentException("lookbackDias deve ser >= 1");
        }
        if (intervaloMinutos < 1) {
            throw new IllegalArgumentException("intervaloMinutos deve ser >= 1");
        }
        this.relatorios = relatorios;
        this.executor = executor;
        this.lookbackDias = lookbackDias;
        this.intervaloMinutos = intervaloMinutos;
    }

    public void iniciar() {
        executor.scheduleAtFixedRate(() -> {
            long inicio = System.nanoTime();
            try {
                System.out.println("Executando rotina de relatórios do dia atual...");
                ResultadoPublicacao resultado = publicarHoje();
                long segundos = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - inicio);
                System.out.println("Rotina concluída em " + segundos + "s: " + resultado);
            } catch (RuntimeException e) {
                System.err.println("Falha na rotina de relatórios: " + e.getMessage());
            }
        }, 0, intervaloMinutos, TimeUnit.MINUTES);
    }

    public ResultadoPublicacao publicarDia(LocalDate dia) {
        return publicar(dia, LocalDateTime.now(ZONA));
    }

    public ResultadoPublicacao publicarHoje() {
        return publicar(LocalDate.now(ZONA), LocalDateTime.now(ZONA));
    }

    public ResultadoPublicacao publicarOntem() {
        LocalDate hoje = LocalDate.now(ZONA);
        return publicar(hoje.minusDays(1), LocalDateTime.now(ZONA));
    }

    private ResultadoPublicacao publicar(LocalDate diaReferencia, LocalDateTime instanteGeracao) {
        try (RelatorioS3Service destino = new RelatorioS3Service()) {
            return relatorios.publicarTodas(diaReferencia, instanteGeracao, destino);
        }
    }

    public ResultadoPublicacao publicarUltimosDias() {
        int publicados = 0;
        int pulados = 0;
        int falhas = 0;
        LocalDate hoje = LocalDate.now(ZONA);
        LocalDateTime instanteGeracao = LocalDateTime.now(ZONA);
        try (RelatorioS3Service destino = new RelatorioS3Service()) {
            for (int diasAtras = lookbackDias; diasAtras >= 1; diasAtras--) {
                LocalDate dia = hoje.minusDays(diasAtras);
                try {
                    ResultadoPublicacao resultado = relatorios.publicarTodas(dia, instanteGeracao, destino);
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

    private static int intervaloMinutosDoAmbiente() {
        String valor = AppConfig.getOuPadrao("RELATORIO_INTERVALO_MINUTOS",
                String.valueOf(INTERVALO_PADRAO_MINUTOS));
        try {
            int minutos = Integer.parseInt(valor.trim());
            if (minutos < 1) {
                throw new NumberFormatException();
            }
            return minutos;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("RELATORIO_INTERVALO_MINUTOS deve ser um inteiro >= 1: " + valor, e);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
