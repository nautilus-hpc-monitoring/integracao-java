package school.sptech.config;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3Provider {

    private final AwsCredentialsProvider credentials;

    public S3Provider() {
        String accessKey = AppConfig.getOpcional("AWS_ACCESS_KEY_ID");
        String secretKey = AppConfig.getOpcional("AWS_SECRET_ACCESS_KEY");
        String sessionToken = AppConfig.getOpcional("AWS_SESSION_TOKEN");
        if (accessKey != null || secretKey != null || sessionToken != null) {
            accessKey = AppConfig.get("AWS_ACCESS_KEY_ID");
            secretKey = AppConfig.get("AWS_SECRET_ACCESS_KEY");
            this.credentials = StaticCredentialsProvider.create(
                    sessionToken == null || sessionToken.isBlank()
                            ? AwsBasicCredentials.create(accessKey, secretKey)
                            : AwsSessionCredentials.create(accessKey, secretKey, sessionToken));
        } else {
            this.credentials = DefaultCredentialsProvider.create();
        }
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
