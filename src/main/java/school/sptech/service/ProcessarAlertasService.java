package school.sptech.service;

import school.sptech.config.AlertaConfig;
import school.sptech.config.AppConfig;
import school.sptech.config.S3Provider;
import school.sptech.dto.LeituraDto;
import school.sptech.repository.AlertaRepository;
import school.sptech.repository.NodeContextoRepository;
import software.amazon.awssdk.services.s3.S3Client;

public class ProcessarAlertasService implements AutoCloseable {

    private final S3Client s3;
    private final String prefixo;
    private final AlertaService alertas;

    public ProcessarAlertasService() {
        this(new S3Provider().getS3Client(),
                AppConfig.getOuPadrao("S3_PREFIXO", "02-silver/"),
                new AlertaService(
                        new NodeContextoRepository(),
                        new JiraClient(
                                AppConfig.get("JIRA_BASE_URL"),
                                AppConfig.get("JIRA_EMAIL"),
                                AppConfig.get("JIRA_API_TOKEN")),
                        AlertaConfig.doAmbiente(),
                        new AlertaRepository()));
    }

    public ProcessarAlertasService(S3Client s3, String prefixo, AlertaService alertas) {
        this.s3 = s3;
        this.prefixo = prefixo;
        this.alertas = alertas;
    }

    public int processarChave(String bucketOrigem, String chave) {
        S3LeitorService leitor = new S3LeitorService(s3, bucketOrigem, prefixo);
        int leituras = 0;
        for (LeituraDto leitura : leitor.lerLeituras(chave)) {
            alertas.avaliar(leitura);
            leituras++;
        }
        return leituras;
    }

    public int getTotalCriados() {
        return alertas.getTotalCriados();
    }

    @Override
    public void close() {
        s3.close();
    }
}
