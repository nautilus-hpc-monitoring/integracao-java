package school.sptech.service;

import java.util.Optional;
import java.math.BigDecimal;

public record Severidade(Nivel nivel, String prioridade) {

    public enum Nivel { ATENCAO, ALERTA, CRITICO }

    public static Optional<Severidade> de(double percentual, double minimo, double maximo) {
        if (!Double.isFinite(percentual) || percentual < 0 || percentual > 100
                || !Double.isFinite(minimo) || !Double.isFinite(maximo)
                || minimo < 0 || minimo >= maximo || maximo > 100) {
            throw new IllegalArgumentException("Percentual ou picos inválidos: use 0 <= mínimo < máximo <= 100.");
        }
        // Comparação decimal para preservar fronteiras como 80 + 10% = 88.
        BigDecimal valor = BigDecimal.valueOf(percentual);
        BigDecimal min = BigDecimal.valueOf(minimo);
        BigDecimal max = BigDecimal.valueOf(maximo);
        if (valor.compareTo(min.multiply(new BigDecimal("0.90"))) <= 0
                || valor.compareTo(max.multiply(new BigDecimal("1.10"))) >= 0)
            return Optional.of(new Severidade(Nivel.CRITICO, "Highest"));
        if (valor.compareTo(min.multiply(new BigDecimal("0.95"))) <= 0
                || valor.compareTo(max.multiply(new BigDecimal("1.05"))) >= 0)
            return Optional.of(new Severidade(Nivel.ALERTA, "High"));
        if (valor.compareTo(min.multiply(new BigDecimal("1.05"))) <= 0
                || valor.compareTo(max.multiply(new BigDecimal("0.95"))) >= 0)
            return Optional.of(new Severidade(Nivel.ATENCAO, "Medium"));
        return Optional.empty(); // dentro do normal: sem alerta
    }
}
