package school.sptech.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uma linha do CSV da silver (uma coleta de uma máquina).
 * 'percentuais' só tem os componentes que vieram preenchidos: se a máquina não
 * monitora SWAP, por exemplo, a coluna vem vazia e SWAP não aparece no mapa.
 */
public record LeituraDto(LocalDateTime timestamp, String hostname, Map<Componente, Double> percentuais) {}
