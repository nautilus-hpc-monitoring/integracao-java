package school.sptech.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexao {

    private Conexao() {}

    /** Abre uma conexão nova. Quem chama deve fechar (use try-with-resources). */
    public static Connection abrir() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.get("DB_URL"),       // ex.: jdbc:mysql://localhost:3306/nautilus
                AppConfig.get("DB_USER"),
                AppConfig.get("DB_PASSWORD"));
    }
}
