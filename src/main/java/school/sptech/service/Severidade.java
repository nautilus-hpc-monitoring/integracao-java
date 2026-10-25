package school.sptech.service;

import java.util.Optional;

/**
 * Regra de severidade por percentual de uso (os limites vêm do AlertaConfig):
 *   acima de LIMITE_ATENCAO -> Atençao (prioridade Medium)
 *   acima de LIMITE_ALERTA  -> Alerta  (prioridade High)
 *   acima de LIMITE_CRITICO -> Alerta  (prioridade Highest)
 * Usa "maior que" (>), igual à classificação do seu silver2gold.py.
 */
public record Severidade(Nivel nivel, String prioridade) {

    public enum Nivel { ATENCAO, ALERTA }

    public static Optional<Severidade> de(double percentual, double atencao, double alerta, double critico) {
        if (percentual > critico) return Optional.of(new Severidade(Nivel.ALERTA, "Highest"));
        if (percentual > alerta)  return Optional.of(new Severidade(Nivel.ALERTA, "High"));
        if (percentual > atencao) return Optional.of(new Severidade(Nivel.ATENCAO, "Medium"));
        return Optional.empty(); // dentro do normal: sem alerta
    }
}
