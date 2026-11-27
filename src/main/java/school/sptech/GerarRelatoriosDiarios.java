package school.sptech;

import school.sptech.service.RelatorioAgendamentoService;

import java.time.LocalDate;

public class GerarRelatoriosDiarios {

    public static void main(String[] args) {
        LocalDate dia = null;
        boolean agendar = false;
        boolean lookback = false;
        boolean hoje = false;

        for (String arg : args) {
            if ("--once".equals(arg)) {
                continue;
            } else if ("--agendar".equals(arg)) {
                agendar = true;
            } else if ("--lookback".equals(arg)) {
                lookback = true;
            } else if ("--hoje".equals(arg)) {
                hoje = true;
            } else if (arg.startsWith("--dia=")) {
                dia = LocalDate.parse(arg.substring("--dia=".length()));
            } else {
                throw new IllegalArgumentException("Argumento desconhecido: " + arg
                        + ". Use --once, --agendar, --hoje, --lookback ou --dia=AAAA-MM-DD");
            }
        }

        RelatorioAgendamentoService servico = new RelatorioAgendamentoService();
        Runtime.getRuntime().addShutdownHook(new Thread(servico::close, "relatorios-shutdown"));

        try {
            if (agendar) {
                servico.iniciar();
            } else if (dia != null) {
                System.out.println("Relatórios de " + dia + ": " + servico.publicarDia(dia));
            } else if (hoje) {
                System.out.println("Relatório do dia atual: " + servico.publicarHoje());
            } else if (lookback) {
                System.out.println("Catch-up: " + servico.publicarUltimosDias());
            } else {
                System.out.println("Relatório do dia anterior: " + servico.publicarOntem());
            }
        } catch (RuntimeException e) {
            servico.close();
            throw e;
        }
    }
}
