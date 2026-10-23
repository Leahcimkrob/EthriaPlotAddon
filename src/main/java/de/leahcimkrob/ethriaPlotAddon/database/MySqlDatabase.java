package de.leahcimkrob.ethriaPlotAddon.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/** Opens a MySQL schema, creating it first when the configured account has permission. */
public final class MySqlDatabase {
    private MySqlDatabase() { }

    public static Connection open(String host, int port, String database, String username,
                                  String password, boolean useSsl) throws SQLException {
        Objects.requireNonNull(database, "database");
        if (!database.matches("[A-Za-z0-9_$-]+")) {
            throw new SQLException("Invalid MySQL database name in configuration: " + database);
        }
        String options = "?useSSL=" + useSsl + "&allowPublicKeyRetrieval=true";
        String serverUrl = "jdbc:mysql://" + host + ":" + port + "/" + options;
        try (Connection server = DriverManager.getConnection(serverUrl, username, password);
             Statement statement = server.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS `" + database + "`");
        } catch (SQLException exception) {
            throw new SQLException("Could not create or access MySQL database '" + database
                    + "'. The configured MySQL user needs CREATE DATABASE permission, or the database must be created by an administrator.", exception);
        }
        return DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + options,
                username, password);
    }
}
