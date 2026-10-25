package school.sptech.service;

import school.sptech.config.AlertaConfig;
import school.sptech.dto.Componente;
import school.sptech.dto.LeituraDto;
import school.sptech.dto.NodeContextoDto;
import school.sptech.repository.NodeContextoRepository;
import school.sptech.repository.UltimosAlertasRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class AlertaService {

    private final NodeContextoRepository nodes;
    private final JiraClient jira;
    private final UltimosAlertasRepository ultimosAlertas;
    private final AlertaConfig config;
    private int totalCriados = 0;

    public AlertaService(NodeContextoRepository nodes, JiraClient jira,
                         UltimosAlertasRepository ultimosAlertas, AlertaConfig config) {
        this.nodes = nodes;
        this.jira = jira;
        this.ultimosAlertas = ultimosAlertas;
        this.config = config;
    }

    /** Avalia cada componente preenchido da leitura (CPU, RAM, SWAP, DISCO). */
    public void avaliar(LeituraDto leitura) {
        if (leituraAntiga(leitura)) {
            return;
        }
        for (Map.Entry<Componente, Double> entrada : leitura.percentuais().entrySet()) {
            avaliarComponente(leitura, entrada.getKey(), entrada.getValue());
        }
    }

    private boolean leituraAntiga(LeituraDto leitura) {
        if (config.idadeMaxima().isZero()) {
            return false; // 0 = não descarta nada
        }
        return leitura.timestamp().isBefore(LocalDateTime.now().minus(config.idadeMaxima()));
    }

    private void avaliarComponente(LeituraDto leitura, Componente componente, double percentual) {
        Optional<Severidade> resultado = Severidade.de(percentual,
                config.limiteAtencao(), config.limiteAlerta(), config.limiteCritico());
        if (resultado.isEmpty()) {
            return;
        }
        Severidade severidade = resultado.get();

        // Subir de Atençao para Alerta é outra chave, então abre issue nova na hora
        String chave = leitura.hostname() + "|" + componente + "|" + severidade.nivel();
        if (!ultimosAlertas.podeAlertar(chave, leitura.timestamp(), config.cooldown())) {
            return; // já alertamos isso há pouco tempo
        }

        NodeContextoDto c = nodes.buscarPorHostname(leitura.hostname())
                .orElse(NodeContextoDto.desconhecido(leitura.hostname()));

        String resumo = "%s a %.1f%% | %s / %s / %s"
                .formatted(componente, percentual, c.hpc(), c.cluster(), c.node());

        String descricao = """
                Empresa: %s
                HPC: %s
                Cluster: %s
                Node: %s
                Componente: %s
                Uso: %.1f%%
                Horário da leitura: %s
                Proprietário: %s (%s)
                """.formatted(c.empresa(), c.hpc(), c.cluster(), c.node(), componente,
                percentual, leitura.timestamp(), c.proprietarioNome(), c.proprietarioEmail());

        String tipoId = severidade.nivel() == Severidade.Nivel.ALERTA
                ? config.tipoAlertaId()
                : config.tipoAtencaoId();

        String issueKey = jira.createIssue(config.projectKey(), resumo, descricao, tipoId, severidade.prioridade());

        // Só registra depois que a issue foi criada de verdade
        ultimosAlertas.registrar(chave, leitura.timestamp());
        totalCriados++;
        System.out.println("Issue criada: " + issueKey + " -> " + resumo);
    }

    public int getTotalCriados() {
        return totalCriados;
    }
}
