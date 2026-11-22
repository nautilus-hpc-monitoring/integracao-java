package school.sptech;

import school.sptech.dto.relatorio.EmpresaRelatorioDto;
import school.sptech.service.RelatorioDiarioService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class TesteRelatorioPdfBanco {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    public static void main(String[] args) throws Exception {
        LocalDate dia = args.length > 0 ? LocalDate.parse(args[0]) : LocalDate.now(ZONA).minusDays(1);

        RelatorioDiarioService service = new RelatorioDiarioService();
        List<EmpresaRelatorioDto> relatorios = service.montarTodas(dia);

        Path diretorio = Path.of("relatorios-teste");
        Files.createDirectories(diretorio);

        for (EmpresaRelatorioDto dados : relatorios) {
            Path destino = diretorio.resolve("relatorio-" + dados.idEmpresa() + "-" + dia + ".pdf");
            service.gerar(dados, destino);
            System.out.println("PDF gerado: " + destino.toAbsolutePath()
                    + " | " + dados.razaoSocial()
                    + " | " + dados.total() + " alerta(s)"
                    + " | " + Files.size(destino) + " bytes");
        }
    }
}
