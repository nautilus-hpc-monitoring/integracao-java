package school.sptech.repository;

import school.sptech.config.Conexao;
import school.sptech.dto.Componente;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

public class AlertaRepository {

    public record LimitesComponente(int id, double minimo, double maximo, double atencao, double critico) {
        // CPU, RAM e disco continuam usando os picos mínimo e máximo.
        public LimitesComponente(int id, double minimo, double maximo) {
            this(id, minimo, maximo, 0, 0);
        }
    }

    // Retorna null quando este componente não foi selecionado para o node.
    public LimitesComponente buscarComponenteNode(String hostname, Componente componente) {
        String sql = """
                SELECT cn.id_componente_node, cn.pico_min, cn.pico_max,
                       cn.limite_atencao, cn.limite_critico
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
                    return null;
                }
                int id = rs.getInt("id_componente_node");
                BigDecimal minimo = rs.getBigDecimal("pico_min");
                BigDecimal maximo = rs.getBigDecimal("pico_max");
                BigDecimal atencao = rs.getBigDecimal("limite_atencao");
                BigDecimal critico = rs.getBigDecimal("limite_critico");
                if (rs.next()) {
                    throw new IllegalStateException("Mais de um componente do mesmo tipo para "
                            + hostname + " / " + componente + ". O CSV não identifica qual utilizar.");
                }

                if (componente == Componente.SWAP) {
                    if (atencao == null || critico == null || atencao.signum() < 0
                            || atencao.compareTo(critico) >= 0 || critico.compareTo(BigDecimal.valueOf(100)) > 0) {
                        throw new IllegalStateException("Limites inválidos para " + hostname + " / SWAP"
                                + ": configure 0 <= Atenção < Crítico <= 100 no cadastro do node.");
                    }
                    // Os picos não são utilizados para swap, inclusive se estiverem NULL.
                    return new LimitesComponente(id, 0, 0, atencao.doubleValue(), critico.doubleValue());
                }

                if (minimo == null || maximo == null || minimo.signum() < 0
                        || minimo.compareTo(maximo) >= 0 || maximo.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalStateException("Picos inválidos para " + hostname + " / " + componente
                            + ": informe 0 <= pico_min < pico_max <= 100, em porcentagem.");
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

    // A primeira leitura normal registra o fim dos alertas ainda abertos deste componente.
    public int encerrarNormalizados(int componenteNodeId, LocalDateTime horario) {
        String sql = """
                UPDATE alerta
                SET dt_fim = ?, status = 'resolvido'
                WHERE fk_componente_node = ?
                  AND dt_fim IS NULL
                  AND status IN ('aberto', 'em_andamento')
                  AND dt_hora <= ?
                """;
        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(horario));
            ps.setInt(2, componenteNodeId);
            ps.setTimestamp(3, Timestamp.valueOf(horario));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro encerrando alertas do componente " + componenteNodeId, e);
        }
    }
}
