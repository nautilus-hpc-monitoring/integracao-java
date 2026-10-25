package school.sptech.config;

import java.time.Duration;

/**
 * Todos os "botões" do alerta em um lugar só. Cada valor vem de uma variável de
 * ambiente; se ela não existir, vale o padrão indicado.
 *
 * | Variável               | Padrão | Significado                                           |
 * |------------------------|--------|-------------------------------------------------------|
 * | JIRA_PROJECT_KEY       | (obrig)| key do projeto no Jira                                |
 * | JIRA_TIPO_ATENCAO_ID   | 10013  | id do tipo de issue "Atençao"                         |
 * | JIRA_TIPO_ALERTA_ID    | 10014  | id do tipo de issue "Alerta"                          |
 * | LIMITE_ATENCAO         | 75     | acima disso: Atençao / Medium  (igual à sua Gold)     |
 * | LIMITE_ALERTA          | 90     | acima disso: Alerta / High     (igual à sua Gold)     |
 * | LIMITE_CRITICO         | 95     | acima disso: Alerta / Highest                         |
 * | ALERTA_COOLDOWN_MIN    | 60     | minutos sem repetir o mesmo alerta (host+componente+nível) |
 * | ALERTA_IDADE_MAX_HORAS | 24     | ignora leituras mais velhas que isso (0 = não ignora) |
 */
public record AlertaConfig(String projectKey, String tipoAtencaoId, String tipoAlertaId,
                           double limiteAtencao, double limiteAlerta, double limiteCritico,
                           Duration cooldown, Duration idadeMaxima) {

    public AlertaConfig {
        if (!(limiteAtencao < limiteAlerta && limiteAlerta < limiteCritico)) {
            throw new IllegalStateException("Os limites precisam estar em ordem crescente "
                    + "(LIMITE_ATENCAO < LIMITE_ALERTA < LIMITE_CRITICO)");
        }
    }

    public static AlertaConfig doAmbiente() {
        return new AlertaConfig(
                AppConfig.get("JIRA_PROJECT_KEY"),
                AppConfig.getOuPadrao("JIRA_TIPO_ATENCAO_ID", "10013"),
                AppConfig.getOuPadrao("JIRA_TIPO_ALERTA_ID", "10014"),
                numero("LIMITE_ATENCAO", 75),
                numero("LIMITE_ALERTA", 90),
                numero("LIMITE_CRITICO", 95),
                Duration.ofMinutes((long) numero("ALERTA_COOLDOWN_MIN", 60)),
                Duration.ofHours((long) numero("ALERTA_IDADE_MAX_HORAS", 24)));
    }

    private static double numero(String chave, double padrao) {
        String valor = System.getenv(chave);
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
