package school.sptech.service;

import school.sptech.dto.relatorio.EmpresaRelatorioDto;
import school.sptech.repository.RelatorioRepository;
import school.sptech.repository.RelatorioRepository.EmpresaBasica;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RelatorioDiarioService {

    private final RelatorioRepository repository;
    private final RelatorioPdfService pdf;

    public RelatorioDiarioService() {
        this(new RelatorioRepository(), new RelatorioPdfService());
    }

    public RelatorioDiarioService(RelatorioRepository repository, RelatorioPdfService pdf) {
        this.repository = repository;
        this.pdf = pdf;
    }

    public EmpresaRelatorioDto montar(int idEmpresa, LocalDate dia) {
        EmpresaBasica empresa = repository.listarEmpresas().stream()
                .filter(candidata -> candidata.id() == idEmpresa)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada: " + idEmpresa));
        return repository.buscarDados(empresa, dia);
    }

    public List<EmpresaRelatorioDto> montarTodas(LocalDate dia) {
        List<EmpresaRelatorioDto> relatorios = new ArrayList<>();
        for (EmpresaBasica empresa : repository.listarEmpresas()) {
            relatorios.add(repository.buscarDados(empresa, dia));
        }
        return relatorios;
    }

    public byte[] gerarBytes(int idEmpresa, LocalDate dia) {
        return pdf.gerarBytes(montar(idEmpresa, dia));
    }

    public void gerar(int idEmpresa, LocalDate dia, Path destino) {
        pdf.gerar(montar(idEmpresa, dia), destino);
    }

    public void gerar(EmpresaRelatorioDto dados, Path destino) {
        pdf.gerar(dados, destino);
    }
}
