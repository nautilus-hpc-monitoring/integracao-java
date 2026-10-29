package school.sptech.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record LeituraDto(LocalDateTime timestamp, String hostname, Map<Componente, Double> percentuais) {}
