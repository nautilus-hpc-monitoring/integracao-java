package school.sptech;

import school.sptech.config.AppConfig;
import school.sptech.config.S3Provider;
import school.sptech.dto.LeituraDto;
import school.sptech.service.S3LeitorService;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;

/** Testa só o S3 + parser (sem Jira e sem banco). Precisa de credenciais AWS, S3_BUCKET e S3_PREFIXO. */
public class TesteS3 {

    public static void main(String[] args) {
        String prefixo = AppConfig.getOuPadrao("S3_PREFIXO", "02-silver/");

        try (S3Client s3 = new S3Provider().getS3Client()) {
            S3LeitorService leitor = new S3LeitorService(s3, AppConfig.get("S3_BUCKET"), prefixo);

            List<String> arquivos = leitor.listarArquivos();
            System.out.println(arquivos.size() + " arquivo(s) .csv em " + prefixo);

            // Só os 5 primeiros, para não imprimir o bucket inteiro
            for (String chave : arquivos.stream().limit(5).toList()) {
                List<LeituraDto> leituras = leitor.lerLeituras(chave);
                System.out.println(chave + " -> " + leituras.size() + " leitura(s)");
                if (!leituras.isEmpty()) {
                    System.out.println("   primeira: " + leituras.get(0));
                }
            }
        }
    }
}
