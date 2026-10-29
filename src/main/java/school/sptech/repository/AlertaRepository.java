package school.sptech.repository;

import school.sptech.config.Conexao;
import school.sptech.dto.Componente;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

public class AlertaRepository {

    public record LimitesComponente(int id, double minimo, double maximo) {}

    public LimitesComponente buscarComponenteNode(String hostname, Componente componente) {
        String sql = """
                SELECT cn.id_componente_node, cn.pico_min, cn.pico_max
                FROM componente_node cn
                JOIN node n ON n.id_node = cn.fk_node
                JOIN componente c ON c.id_componente = cn.fk_componente
                WHERE n.hostname = ? AND UPPER(TRIM(c.tipo)) = ?
                """;
        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hostname);
            ps.setString(2, componente.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Componente sem cadastro em componente_node: "
                            + hostname + " / " + componente);
                }
                int id = rs.getInt("id_componente_node");
                BigDecimal minimo = rs.getBigDecimal("pico_min");
                BigDecimal maximo = rs.getBigDecimal("pico_max");
                if (minimo == null || maximo == null || minimo.signum() < 0
                        || minimo.compareTo(maximo) >= 0 || maximo.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalStateException("Picos inválidos para " + hostname + " / " + componente
                            + ": informe 0 <= pico_min < pico_max <= 100, em porcentagem.");
                }
                if (rs.next()) {
                    throw new IllegalStateException("Mais de um componente do mesmo tipo para "
                            + hostname + " / " + componente + ". O CSV não identifica qual utilizar.");
                }
                return new LimitesComponente(id, minimo.doubleValue(), maximo.doubleValue());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro consultando componente_node para " + hostname, e);
        }
    }

    public void inserir(String titulo, String descricao, String nivel, double valor,
                        String chaveJira, LocalDateTime horario, int componenteNodeId) {
        String sql = """
                INSERT INTO alerta
                    (titulo, descricao, nivel, valor_capturado, status, chave_jira, dt_hora, fk_componente_node)
                VALUES (?, ?, ?, ?, 'aberto', ?, ?, ?)
                """;
        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titulo);
            ps.setString(2, descricao);
            ps.setString(3, nivel);
            ps.setBigDecimal(4, BigDecimal.valueOf(valor));
            ps.setString(5, chaveJira);
            ps.setTimestamp(6, Timestamp.valueOf(horario));
            ps.setInt(7, componenteNodeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Issue " + chaveJira
                    + " criada no Jira, mas não gravada em alerta. Verifique o banco antes de reprocessar: "
                    + "o reprocessamento pode duplicar a issue.", e);
        }
    }
}
