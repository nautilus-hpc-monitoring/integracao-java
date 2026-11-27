package school.sptech.service;

import school.sptech.config.AppConfig;
import school.sptech.config.S3Provider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RelatorioS3Service implements AutoCloseable {

    private static final String PREFIXO = "04-relatorios/";
    private static final DateTimeFormatter FORMATO_INSTANTE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd/HH-mm-ss.SSS");

    private final S3Client s3;
    private final String bucket;
    private final boolean ownsClient;

    public RelatorioS3Service() {
        this(new S3Provider().getS3Client(), bucketPadrao(), true);
    }

    public RelatorioS3Service(S3Client s3, String bucket) {
        this(s3, bucket, false);
    }

    private RelatorioS3Service(S3Client s3, String bucket, boolean ownsClient) {
        this.s3 = s3;
        this.bucket = bucket;
        this.ownsClient = ownsClient;
    }

    public static String chave(int idEmpresa, LocalDateTime instanteGeracao) {
        return PREFIXO + "id_empresa=" + idEmpresa + "/"
                + instanteGeracao.format(FORMATO_INSTANTE) + ".pdf";
    }

    public boolean existe(int idEmpresa, LocalDateTime instanteGeracao) {
        return existe(chave(idEmpresa, instanteGeracao));
    }

    public boolean existe(String chave) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(chave).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            String detalhes = e.awsErrorDetails() == null ? "" : " ["
                    + e.awsErrorDetails().errorCode() + ": " + e.awsErrorDetails().errorMessage() + "]";
            throw new IllegalStateException("Erro verificando PDF no S3: " + chave + detalhes, e);
        }
    }

    public boolean publicar(int idEmpresa, LocalDateTime instanteGeracao, byte[] pdf) {
        String chave = chave(idEmpresa, instanteGeracao);

        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(chave)
                        .contentType("application/pdf")
                        .contentLength((long) pdf.length)
                        .build(),
                RequestBody.fromBytes(pdf));

        System.out.println("PDF publicado no S3: s3://" + bucket + "/" + chave);
        return true;
    }

    public byte[] baixar(int idEmpresa, LocalDateTime instanteGeracao) {
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(chave(idEmpresa, instanteGeracao))
                .build();
        return s3.getObject(req, ResponseTransformer.toBytes()).asByteArray();
    }

    @Override
    public void close() {
        if (ownsClient) {
            s3.close();
        }
    }

    private static String bucketPadrao() {
        String especifico = AppConfig.getOpcional("S3_BUCKET_RELATORIOS");
        return (especifico == null || especifico.isBlank())
                ? AppConfig.get("S3_BUCKET")
                : especifico;
    }
}
