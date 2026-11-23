package school.sptech;

import school.sptech.service.RelatorioAgendamentoService;

import java.time.LocalDate;

public class GerarRelatoriosDiarios {

    public static void main(String[] args) {
        LocalDate dia = null;
        boolean umaVez = false;

        for (String arg : args) {
            if ("--once".equals(arg)) {
                umaVez = true;
            } else if (arg.startsWith("--dia=")) {
                dia = LocalDate.parse(arg.substring("--dia=".length()));
            } else {
                throw new IllegalArgumentException(
                        "Argumento desconhecido: " + arg + ". Use --once ou --dia=AAAA-MM-DD");
            }
        }
        if (dia != null) {
            umaVez = true;
        }

        RelatorioAgendamentoService servico = new RelatorioAgendamentoService();
        Runtime.getRuntime().addShutdownHook(new Thread(servico::close, "relatorios-shutdown"));

        try {
            if (dia != null) {
                System.out.println("Relatórios de " + dia + ": " + servico.publicarDia(dia));
            } else {
                System.out.println("Catch-up: " + servico.publicarUltimosDias());
            }
            if (!umaVez) {
                servico.iniciar();
            }
        } catch (RuntimeException e) {
            servico.close();
            throw e;
        }
    }
}
