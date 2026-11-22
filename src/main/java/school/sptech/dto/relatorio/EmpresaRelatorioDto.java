package school.sptech.dto.relatorio;

import java.util.List;

public record EmpresaRelatorioDto(String razaoSocial, String periodo, List<AlertaRelatorioDto> alertas) {

    public long total() {
        return alertas.size();
    }

    public long criticos() {
        return contar("CRITICO");
    }

    public long alertasNivel() {
        return contar("ALERTA");
    }

    public long atencao() {
        return contar("ATENCAO");
    }

    private long contar(String nivel) {
        return alertas.stream().filter(alerta -> nivel.equals(alerta.nivel())).count();
    }
}
