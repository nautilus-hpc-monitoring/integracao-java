package school.sptech.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import school.sptech.config.AppConfig;
import school.sptech.service.ProcessarAlertasService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class AlertaS3EventHandler implements RequestHandler<S3Event, Void> {

    private static final ProcessarAlertasService SERVICO = new ProcessarAlertasService();
    private static final String PREFIXO = AppConfig.getOuPadrao("S3_PREFIXO", "02-silver/");

    @Override
    public Void handleRequest(S3Event event, Context context) {
        for (S3EventNotificationRecord registro : event.getRecords()) {
            String bucket = registro.getS3().getBucket().getName();
            String chave = URLDecoder.decode(registro.getS3().getObject().getKey(), StandardCharsets.UTF_8);
            if (!chave.startsWith(PREFIXO) || !chave.endsWith(".csv")) {
                continue;
            }
            SERVICO.processarChave(bucket, chave);
        }
        return null;
    }
}
