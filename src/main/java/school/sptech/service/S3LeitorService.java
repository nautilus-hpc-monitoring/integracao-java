package school.sptech.service;

import school.sptech.dto.Componente;
import school.sptech.dto.LeituraDto;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class S3LeitorService {

    private static final String SEPARADOR = ";";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final S3Client s3;
    private final String bucket;
    private final String prefixo;

    public S3LeitorService(S3Client s3, String bucket, String prefixo) {
        this.s3 = s3;
        this.bucket = bucket;
        this.prefixo = prefixo;
    }

    /** Lista as chaves dos .csv sob o prefixo (o paginator percorre todas as páginas). */
    public List<String> listarArquivos() {
        ListObjectsV2Request req = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefixo)
                .build();

        return s3.listObjectsV2Paginator(req).contents().stream()
                .map(S3Object::key)
                .filter(chave -> chave.endsWith(".csv"))
                .sorted()
                .toList();
    }

    /**
     * Lê um CSV da silver direto do S3 (sem salvar em disco).
     * As colunas são localizadas pelo NOME no cabeçalho (não pela posição), então
     * o pipeline pode ganhar colunas novas sem quebrar a leitura.
     */
    public List<LeituraDto> lerLeituras(String chave) {
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(chave)
                .build();

        List<LeituraDto> leituras = new ArrayList<>();

        try (var stream = s3.getObject(req, ResponseTransformer.toInputStream());
             var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String cabecalho = reader.readLine();
            if (cabecalho == null) {
                return leituras; // arquivo vazio
            }

            Map<String, Integer> colunas = indexarColunas(cabecalho);
            int iTimestamp = indiceObrigatorio(colunas, "TIMESTAMP", chave);
            int iHostname = indiceObrigatorio(colunas, "HOSTNAME", chave);

            String linha;
            int numeroLinha = 1;
            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                if (linha.isBlank()) {
                    continue;
                }

                // limite -1: mantém as colunas vazias no fim da linha (componente não monitorado)
                String[] c = linha.split(SEPARADOR, -1);

                try {
                    LocalDateTime timestamp = LocalDateTime.parse(c[iTimestamp].trim(), FORMATO_DATA);
                    String hostname = c[iHostname].trim();

                    Map<Componente, Double> percentuais = new EnumMap<>(Componente.class);
                    for (Componente componente : Componente.values()) {
                        Integer indice = colunas.get(componente.getColuna());
                        if (indice == null || indice >= c.length) {
                            continue;
                        }
                        String bruto = c[indice].trim();
                        if (bruto.isEmpty()) {
                            continue; // vazio = componente não monitorado nesta máquina
                        }
                        percentuais.put(componente, Double.parseDouble(bruto));
                    }

                    leituras.add(new LeituraDto(timestamp, hostname, percentuais));

                } catch (DateTimeParseException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.err.println("Linha " + numeroLinha + " ignorada em " + chave + ": " + linha);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro lendo " + chave, e);
        }
        return leituras;
    }

    private Map<String, Integer> indexarColunas(String cabecalho) {
        String[] nomes = cabecalho.replace("\uFEFF", "").split(SEPARADOR, -1); // \uFEFF = BOM, se existir
        Map<String, Integer> indices = new HashMap<>();
        for (int i = 0; i < nomes.length; i++) {
            indices.put(nomes[i].trim().toUpperCase(), i);
        }
        return indices;
    }

    private int indiceObrigatorio(Map<String, Integer> colunas, String nome, String chave) {
        Integer indice = colunas.get(nome);
        if (indice == null) {
            throw new IllegalStateException("Coluna " + nome + " não encontrada no cabeçalho de " + chave);
        }
        return indice;
    }
}
