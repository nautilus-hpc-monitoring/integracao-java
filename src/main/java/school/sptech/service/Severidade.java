package school.sptech.service;

import java.util.Optional;
import java.math.BigDecimal;

public record Severidade(Nivel nivel, String prioridade) {

    public enum Nivel { ATENCAO, ALERTA, CRITICO }

    // Swap não tem limite mínimo de uso. Zero é normal; só alerta por uso alto.
    public static Optional<Severidade> deSwap(double percentual, double atencao, double critico) {
        if (!Double.isFinite(percentual) || percentual < 0 || percentual > 100
                || !Double.isFinite(atencao) || !Double.isFinite(critico)
                || atencao < 0 || atencao >= critico || critico > 100) {
            throw new IllegalArgumentException("Limites inválidos para swap: use 0 <= atenção < crítico <= 100.");
        }
        if (percentual == 0) return Optional.empty();
        if (percentual >= critico) return Optional.of(new Severidade(Nivel.CRITICO, "Highest"));
        if (percentual >= atencao) return Optional.of(new Severidade(Nivel.ATENCAO, "Medium"));
        return Optional.empty();
    }

    public static Optional<Severidade> de(double percentual, double minimo, double maximo) {
        if (!Double.isFinite(percentual) || percentual < 0 || percentual > 100
                || !Double.isFinite(minimo) || !Double.isFinite(maximo)
                || minimo < 0 || minimo >= maximo || maximo > 100) {
            throw new IllegalArgumentException("Percentual ou picos inválidos: use 0 <= mínimo < máximo <= 100.");
        }
        // Maximo e o limite critico: atencao em 90% dele e alerta em 95%.
        // Minimo zero desativa os alertas por baixo uso.
        BigDecimal valor = BigDecimal.valueOf(percentual);
        BigDecimal min = BigDecimal.valueOf(minimo);
        BigDecimal max = BigDecimal.valueOf(maximo);
        if ((minimo > 0 && valor.compareTo(min.multiply(new BigDecimal("0.90"))) <= 0)
                || valor.compareTo(max) >= 0)
            return Optional.of(new Severidade(Nivel.CRITICO, "Highest"));
        if ((minimo > 0 && valor.compareTo(min.multiply(new BigDecimal("0.95"))) <= 0)
                || valor.compareTo(max.multiply(new BigDecimal("0.95"))) >= 0)
            return Optional.of(new Severidade(Nivel.ALERTA, "High"));
        if ((minimo > 0 && valor.compareTo(min.multiply(new BigDecimal("1.05"))) <= 0)
                || valor.compareTo(max.multiply(new BigDecimal("0.90"))) >= 0)
            return Optional.of(new Severidade(Nivel.ATENCAO, "Medium"));
        return Optional.empty(); // dentro do normal: sem alerta
    }
}
