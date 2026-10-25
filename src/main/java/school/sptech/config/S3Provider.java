package school.sptech.config;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3Provider {

    private final AwsCredentialsProvider credentials;

    public S3Provider() {
        // Procura as credenciais na ordem da "default chain" (variáveis de ambiente, ~/.aws, etc.)
        this.credentials = DefaultCredentialsProvider.create();
    }

    public S3Client getS3Client() {
        // AWS_REGION = o mesmo valor do REGION_NAME do .env do Python
        Region regiao = Region.of(AppConfig.getOuPadrao("AWS_REGION", "us-east-1"));

        return S3Client.builder()
                .region(regiao)
                .credentialsProvider(credentials)
                .build();
    }
}
