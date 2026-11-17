package school.sptech.service;

import school.sptech.config.AlertaConfig;
import school.sptech.dto.Componente;
import school.sptech.dto.LeituraDto;
import school.sptech.dto.NodeContextoDto;
import school.sptech.repository.NodeContextoRepository;
import school.sptech.repository.AlertaRepository;
import school.sptech.repository.UltimosAlertasRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class AlertaService {

    private final NodeContextoRepository nodes;
    private final JiraClient jira;
    private final UltimosAlertasRepository ultimosAlertas;
    private final AlertaConfig config;
    private final AlertaRepository alertas;
    private int totalCriados = 0;

    public AlertaService(NodeContextoRepository nodes, JiraClient jira,
                         UltimosAlertasRepository ultimosAlertas, AlertaConfig config,
                         AlertaRepository alertas) {
        this.nodes = nodes;
        this.jira = jira;
        this.ultimosAlertas = ultimosAlertas;
        this.config = config;
        this.alertas = alertas;
    }

    public void avaliar(LeituraDto leitura) {
        if (leituraAntiga(leitura)) {
            return;
        }
        for (Map.Entry<Componente, Double> entrada : leitura.percentuais().entrySet()) {
            String chaveLeitura = leitura.hostname() + "|" + entrada.getKey() + "|LEITURA";
            if (ultimosAlertas.leituraJaAvaliada(chaveLeitura, leitura.timestamp())) {
                continue;
            }
            avaliarComponente(leitura, entrada.getKey(), entrada.getValue());
            // Persiste só depois de avaliar com sucesso, inclusive quando continua normal.
            ultimosAlertas.registrar(chaveLeitura, leitura.timestamp());
        }
    }

    private boolean leituraAntiga(LeituraDto leitura) {
        if (config.idadeMaxima().isZero()) {
            return false; // 0 = não descarta nada
        }
        return leitura.timestamp().isBefore(LocalDateTime.now().minus(config.idadeMaxima()));
    }

    private void avaliarComponente(LeituraDto leitura, Componente componente, double percentual) {
        AlertaRepository.LimitesComponente limites =
                alertas.buscarComponenteNode(leitura.hostname(), componente);
        // O CSV pode incluir componentes que o usuário não selecionou no cadastro.
        if (limites == null) {
            return;
        }

        Optional<Severidade> resultado = componente == Componente.SWAP
                ? Severidade.deSwap(percentual, limites.atencao(), limites.critico())
                : Severidade.de(percentual, limites.minimo(), limites.maximo());
        if (resultado.isEmpty()) {
            int encerrados = alertas.encerrarNormalizados(limites.id(), leitura.timestamp());
            if (encerrados > 0) {
                System.out.println("Alertas encerrados: " + leitura.hostname() + " / "
                        + componente + " em " + leitura.timestamp() + " (" + encerrados + ")");
            }
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

        String tipoId = severidade.nivel() != Severidade.Nivel.ATENCAO
                ? config.tipoAlertaId()
                : config.tipoAtencaoId();

        // O banco limita o título a 150 caracteres; usa o mesmo título nos dois sistemas.
        String titulo = resumo.length() > 150 ? resumo.substring(0, 150) : resumo;
        String issueKey = jira.createIssue(config.projectKey(), titulo, descricao, tipoId, severidade.prioridade());
        alertas.inserir(titulo, descricao, severidade.nivel().name(), percentual,
                issueKey, leitura.timestamp(), limites.id());

        // Só registra depois que a issue foi criada de verdade
        ultimosAlertas.registrar(chave, leitura.timestamp());
        totalCriados++;
        System.out.println("Issue criada: " + issueKey + " -> " + resumo);
    }

    public int getTotalCriados() {
        return totalCriados;
    }
}
