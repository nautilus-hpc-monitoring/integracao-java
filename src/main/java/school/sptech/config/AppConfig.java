package school.sptech.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Lê o .env do diretório de execução, com prioridade sobre o ambiente do processo.
 * Nada de senha/token fixo no código.
 */
public class AppConfig {

    private static final Map<String, String> VALORES = carregarEnv();

    private static Map<String, String> carregarEnv() {
        Path arquivo = Path.of(".env").toAbsolutePath().normalize();
        Map<String, String> valores = new HashMap<>();
        if (!Files.exists(arquivo)) {
            return valores; // Mantém compatibilidade com execuções configuradas pelo ambiente.
        }
        try {
            int numeroLinha = 0;
            for (String original : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                numeroLinha++;
                String linha = original.replace("\uFEFF", "").trim();
                if (linha.isEmpty() || linha.startsWith("#")) continue;
                if (linha.startsWith("export ")) linha = linha.substring(7).trim();
                int separador = linha.indexOf('=');
                if (separador <= 0) {
                    throw new IllegalStateException("Formato inválido no .env, linha " + numeroLinha);
                }
                String chave = linha.substring(0, separador).trim();
                String valor = linha.substring(separador + 1).trim();
                if (valor.startsWith("\"") || valor.startsWith("'")) {
                    if (valor.length() < 2 || valor.charAt(valor.length() - 1) != valor.charAt(0)) {
                        throw new IllegalStateException("Aspas inválidas no .env, linha " + numeroLinha);
                    }
                    valor = valor.substring(1, valor.length() - 1);
                }
                valores.put(chave, valor);
            }
            return valores;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler " + arquivo, e);
        }
    }

    public static String getOpcional(String chave) {
        return VALORES.containsKey(chave) ? VALORES.get(chave) : System.getenv(chave);
    }

    private AppConfig() {}

    /** Obrigatória: falha cedo, com mensagem clara, se não existir. */
    public static String get(String chave) {
        String valor = getOpcional(chave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Configuração ausente: " + chave
                    + ". Preencha o .env em " + Path.of(".env").toAbsolutePath().normalize());
        }
        return valor;
    }

    /** Opcional: usa o padrão se a variável não existir. */
    public static String getOuPadrao(String chave, String padrao) {
        String valor = getOpcional(chave);
        return (valor == null || valor.isBlank()) ? padrao : valor;
    }
}
