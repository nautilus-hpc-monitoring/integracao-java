package school.sptech;

import school.sptech.config.AlertaConfig;
import school.sptech.config.AppConfig;
import school.sptech.config.S3Provider;
import school.sptech.dto.LeituraDto;
import school.sptech.repository.ArquivosProcessadosRepository;
import school.sptech.repository.AlertaRepository;
import school.sptech.repository.NodeContextoRepository;
import school.sptech.repository.UltimosAlertasRepository;
import school.sptech.service.AlertaService;
import school.sptech.service.JiraClient;
import school.sptech.service.S3LeitorService;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        String bucket = AppConfig.get("S3_BUCKET"); // mesmo valor do BUCKET_NAME do .env do Python
        String prefixo = AppConfig.getOuPadrao("S3_PREFIXO", "02-silver/");

        AlertaConfig config = AlertaConfig.doAmbiente();

        JiraClient jira = new JiraClient(
                AppConfig.get("JIRA_BASE_URL"),
                AppConfig.get("JIRA_EMAIL"),
                AppConfig.get("JIRA_API_TOKEN"));

        AlertaService alertas = new AlertaService(
                new NodeContextoRepository(),
                jira,
                new UltimosAlertasRepository(Path.of("ultimos-alertas.txt")),
                config,
                new AlertaRepository());

        ArquivosProcessadosRepository processados =
                new ArquivosProcessadosRepository(Path.of("arquivos-processados.txt"));

        try (S3Client s3 = new S3Provider().getS3Client()) {
            S3LeitorService leitor = new S3LeitorService(s3, bucket, prefixo);

            int pulados = 0;
            for (String chave : leitor.listarArquivos()) {
                if (processados.jaProcessado(chave)) {
                    pulados++;
                    continue;
                }

                System.out.println("Processando: " + chave);
                try {
                    for (LeituraDto leitura : leitor.lerLeituras(chave)) {
                        alertas.avaliar(leitura);
                    }
                    processados.marcar(chave); // só marca se terminou sem erro
                } catch (RuntimeException e) {
                    System.err.println("Falha em " + chave + ": " + e.getMessage());
                }
            }
            System.out.println("Arquivos já processados (pulados): " + pulados);
        }

        System.out.println("Fim. Alertas criados nesta execução: " + alertas.getTotalCriados());
    }
}
