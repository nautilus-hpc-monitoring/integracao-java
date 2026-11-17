package school.sptech.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lembra, em um arquivo texto, quando foi o último alerta de cada
 * "hostname|COMPONENTE|NIVEL". É o que impede de abrir uma issue nova a cada
 * leitura (ou a cada execução) enquanto o problema continua.
 * Também guarda a última leitura avaliada por componente para rejeitar dados
 * repetidos ou fora de ordem, inclusive depois de reiniciar o programa.
 *
 * Usa o horário da LEITURA (não o do relógio), então funciona igual ao
 * reprocessar dados antigos. Para zerar (ex.: em testes), apague ultimos-alertas.txt.
 */
public class UltimosAlertasRepository {

    private final Path arquivo;
    private final Map<String, LocalDateTime> ultimos = new HashMap<>();

    public UltimosAlertasRepository(Path arquivo) {
        this.arquivo = arquivo;
        try {
            if (Files.exists(arquivo)) {
                for (String linha : Files.readAllLines(arquivo)) {
                    String[] partes = linha.split(";", 2);
                    if (partes.length == 2) {
                        try {
                            ultimos.put(partes[0], LocalDateTime.parse(partes[1]));
                        } catch (DateTimeParseException ignorada) {
                            // linha corrompida: ignora
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro lendo " + arquivo, e);
        }
    }

    /** true se nunca alertou essa chave ou se já passou o cooldown desde o último alerta. */
    public boolean podeAlertar(String chave, LocalDateTime momentoDaLeitura, Duration cooldown) {
        LocalDateTime ultimo = ultimos.get(chave);
        return ultimo == null || !momentoDaLeitura.isBefore(ultimo.plus(cooldown));
    }

    /** Evita aplicar uma leitura repetida ou antiga após uma leitura mais recente. */
    public boolean leituraJaAvaliada(String chave, LocalDateTime momentoDaLeitura) {
        LocalDateTime ultimo = ultimos.get(chave);
        return ultimo != null && !momentoDaLeitura.isAfter(ultimo);
    }

    public void registrar(String chave, LocalDateTime momentoDaLeitura) {
        ultimos.put(chave, momentoDaLeitura);
        salvar();
    }

    private void salvar() {
        List<String> linhas = new ArrayList<>();
        ultimos.forEach((chave, momento) -> linhas.add(chave + ";" + momento));
        try {
            Files.write(arquivo, linhas);
        } catch (IOException e) {
            throw new RuntimeException("Erro gravando " + arquivo, e);
        }
    }
}
