package school.sptech.dto.relatorio;

public record AlertaRelatorioDto(String horario, String hostname, String componente,
                                 String nivel, String valor, String chaveJira) {}
