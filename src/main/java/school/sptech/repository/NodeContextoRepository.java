package school.sptech.repository;

import school.sptech.config.Conexao;
import school.sptech.dto.NodeContextoDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class NodeContextoRepository {

    // >>> ÚNICO PONTO A AJUSTAR NO BANCO <<<
    // O CSV da silver traz o HOSTNAME da máquina; é por ele que achamos o node.
    // Troque nomes de tabelas e colunas pelos do seu schema do nautilus.
    // Os apelidos (AS ...) NÃO mudam: o Java lê por eles.
    // LEFT JOIN em usuario: node sem dono ainda devolve a hierarquia.
    private static final String SQL = """
        SELECT e.razao_social AS empresa,
               h.nome AS hpc,
               c.nome AS cluster,
               n.hostname AS node,
               u.nome AS proprietario_nome,
               u.email_institucional AS proprietario_email
        FROM node n
        JOIN cluster c
            ON n.fk_cluster = c.id_cluster
        JOIN ambiente_hpc h
            ON c.fk_ambiente_hpc = h.id_ambiente_hpc
        JOIN empresa e
            ON h.fk_empresa = e.id_empresa
        LEFT JOIN usuario u
            ON u.fk_empresa = e.id_empresa
        WHERE n.hostname = ?
        LIMIT 1
        """;

    public Optional<NodeContextoDto> buscarPorHostname(String hostname) {
        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setString(1, hostname);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String nome = rs.getString("proprietario_nome");
                String email = rs.getString("proprietario_email");

                return Optional.of(new NodeContextoDto(
                        rs.getString("empresa"),
                        rs.getString("hpc"),
                        rs.getString("cluster"),
                        rs.getString("node"),
                        nome != null ? nome : "Sem proprietário",
                        email != null ? email : "-"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro consultando o hostname " + hostname, e);
        }
    }
}
