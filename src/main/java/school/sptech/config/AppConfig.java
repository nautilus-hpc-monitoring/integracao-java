package school.sptech.config;

/**
 * Lê configurações das variáveis de ambiente.
 * Nada de senha/token fixo no código.
 */
public class AppConfig {

    private AppConfig() {}

    /** Obrigatória: falha cedo, com mensagem clara, se não existir. */
    public static String get(String chave) {
        String valor = System.getenv(chave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente: " + chave);
        }
        return valor;
    }

    /** Opcional: usa o padrão se a variável não existir. */
    public static String getOuPadrao(String chave, String padrao) {
        String valor = System.getenv(chave);
        return (valor == null || valor.isBlank()) ? padrao : valor;
    }
}
