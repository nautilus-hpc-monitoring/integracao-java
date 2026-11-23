package school.sptech.service;

import school.sptech.dto.relatorio.EmpresaRelatorioDto;
import school.sptech.repository.RelatorioRepository;
import school.sptech.repository.RelatorioRepository.EmpresaBasica;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RelatorioDiarioService {

    public record ResultadoPublicacao(int publicados, int pulados, int falhas) {
    }

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

    public boolean publicar(int idEmpresa, LocalDate dia) {
        try (RelatorioS3Service destino = new RelatorioS3Service()) {
            return publicar(idEmpresa, dia, destino);
        }
    }

    public boolean publicar(int idEmpresa, LocalDate dia, RelatorioS3Service destino) {
        return destino.publicar(idEmpresa, dia, gerarBytes(idEmpresa, dia));
    }

    public ResultadoPublicacao publicarTodas(LocalDate dia) {
        try (RelatorioS3Service destino = new RelatorioS3Service()) {
            return publicarTodas(dia, destino);
        }
    }

    public ResultadoPublicacao publicarTodas(LocalDate dia, RelatorioS3Service destino) {
        int publicados = 0;
        int pulados = 0;
        int falhas = 0;
        for (EmpresaRelatorioDto dados : montarTodas(dia)) {
            try {
                if (destino.publicar(dados.idEmpresa(), dia, pdf.gerarBytes(dados))) {
                    publicados++;
                } else {
                    pulados++;
                }
            } catch (RuntimeException e) {
                falhas++;
                System.err.println("Falha publicando relatório da empresa " + dados.idEmpresa()
                        + " em " + dia + ": " + e.getMessage());
            }
        }
        return new ResultadoPublicacao(publicados, pulados, falhas);
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
