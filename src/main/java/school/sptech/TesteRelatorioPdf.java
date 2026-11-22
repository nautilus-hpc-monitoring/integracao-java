package school.sptech;

import school.sptech.dto.relatorio.AlertaRelatorioDto;
import school.sptech.dto.relatorio.EmpresaRelatorioDto;
import school.sptech.service.RelatorioPdfService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TesteRelatorioPdf {

    public static void main(String[] args) throws Exception {
        List<AlertaRelatorioDto> alertas = List.of(
                new AlertaRelatorioDto("21/11/2026 10:15", "node-01", "CPU", "CRITICO", "98,50%", "LOG-123"),
                new AlertaRelatorioDto("21/11/2026 11:40", "node-02", "RAM", "ALERTA", "87,20%", "LOG-124"),
                new AlertaRelatorioDto("21/11/2026 13:05", "node-03", "SWAP", "ATENCAO", "12,00%", "LOG-125")
        );

        EmpresaRelatorioDto dados = new EmpresaRelatorioDto(
                "TechCorp Soluções em TI", "21/11/2026", alertas);

        byte[] pdf = new RelatorioPdfService().gerar(dados);

        Path destino = Path.of("relatorio-teste.pdf");
        Files.write(destino, pdf);
        System.out.println("PDF gerado: " + destino.toAbsolutePath() + " (" + pdf.length + " bytes)");
    }
}
