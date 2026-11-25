package school.sptech.config;

import java.time.Duration;

public record AlertaConfig(String projectKey, String tipoAtencaoId, String tipoAlertaId,
                           Duration idadeMaxima) {

    public static AlertaConfig doAmbiente() {
        return new AlertaConfig(
                AppConfig.get("JIRA_PROJECT_KEY"),
                AppConfig.getOuPadrao("JIRA_TIPO_ATENCAO_ID", "10013"),
                AppConfig.getOuPadrao("JIRA_TIPO_ALERTA_ID", "10014"),
                Duration.ofHours((long) numero("ALERTA_IDADE_MAX_HORAS", 24)));
    }

    private static double numero(String chave, double padrao) {
        String valor = AppConfig.getOpcional(chave);
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            return Double.parseDouble(valor.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Valor inválido em " + chave + ": " + valor);
        }
    }
}
