package school.sptech.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Guarda em um arquivo texto (uma chave do S3 por linha) quais arquivos da prata
 * já foram processados. Assim, rodar o programa de novo não recria as mesmas issues.
 * Para reprocessar tudo (ex.: em testes), apague o arquivo arquivos-processados.txt.
 */
public class ArquivosProcessadosRepository {

    private final Path arquivo;
    private final Set<String> processados = new HashSet<>();

    public ArquivosProcessadosRepository(Path arquivo) {
        this.arquivo = arquivo;
        try {
            if (Files.exists(arquivo)) {
                processados.addAll(Files.readAllLines(arquivo));
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro lendo " + arquivo, e);
        }
    }

    public boolean jaProcessado(String chave) {
        return processados.contains(chave);
    }

    public void marcar(String chave) {
        if (!processados.add(chave)) {
            return;
        }
        try {
            Files.writeString(arquivo, chave + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Erro gravando " + arquivo, e);
        }
    }
}
