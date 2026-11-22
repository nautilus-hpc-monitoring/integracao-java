package school.sptech.repository;

import school.sptech.config.Conexao;
import school.sptech.dto.relatorio.AlertaRelatorioDto;
import school.sptech.dto.relatorio.EmpresaRelatorioDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RelatorioRepository {

    public record EmpresaBasica(int id, String razaoSocial) {
    }

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_HORARIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_PERIODO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    public List<EmpresaBasica> listarEmpresas() {
        String sql = """
                SELECT id_empresa, razao_social
                FROM empresa
                ORDER BY id_empresa
                """;
        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<EmpresaBasica> empresas = new ArrayList<>();
            while (rs.next()) {
                empresas.add(new EmpresaBasica(
                        rs.getInt("id_empresa"),
                        rs.getString("razao_social")));
            }
            return empresas;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro listando empresas para o relatório", e);
        }
    }

    public EmpresaRelatorioDto buscarDados(EmpresaBasica empresa, LocalDate dia) {
        ZonedDateTime inicio = dia.atStartOfDay(ZONA);
        ZonedDateTime fim = dia.plusDays(1).atStartOfDay(ZONA);

        String sql = """
                SELECT a.dt_hora, n.hostname, c.tipo, a.nivel,
                       a.valor_capturado, a.chave_jira
                FROM alerta a
                JOIN componente_node cn ON cn.id_componente_node = a.fk_componente_node
                JOIN componente c ON c.id_componente = cn.fk_componente
                JOIN node n ON n.id_node = cn.fk_node
                JOIN cluster cl ON cl.id_cluster = n.fk_cluster
                JOIN ambiente_hpc ah ON ah.id_ambiente_hpc = cl.fk_ambiente_hpc
                WHERE ah.fk_empresa = ?
                  AND a.dt_hora >= ?
                  AND a.dt_hora < ?
                ORDER BY a.dt_hora DESC
                """;

        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empresa.id());
            ps.setTimestamp(2, Timestamp.valueOf(inicio.toLocalDateTime()));
            ps.setTimestamp(3, Timestamp.valueOf(fim.toLocalDateTime()));

            List<AlertaRelatorioDto> alertas = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String chaveJira = rs.getString("chave_jira");
                    alertas.add(new AlertaRelatorioDto(
                            rs.getTimestamp("dt_hora").toLocalDateTime().format(FORMATO_HORARIO),
                            rs.getString("hostname"),
                            rs.getString("tipo"),
                            rs.getString("nivel"),
                            String.format(PT_BR, "%.2f%%", rs.getBigDecimal("valor_capturado")),
                            chaveJira == null ? "-" : chaveJira));
                }
            }

            return new EmpresaRelatorioDto(
                    empresa.id(),
                    empresa.razaoSocial(),
                    dia.format(FORMATO_PERIODO),
                    alertas);
        } catch (SQLException e) {
            throw new IllegalStateException("Erro buscando alertas da empresa " + empresa.id()
                    + " em " + dia, e);
        }
    }
}
