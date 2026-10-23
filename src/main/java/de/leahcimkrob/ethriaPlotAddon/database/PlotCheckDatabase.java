package de.leahcimkrob.ethriaPlotAddon.database;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Network-wide last-online and current-online data for PlotCheck. */
public final class PlotCheckDatabase implements AutoCloseable {
    private final JavaPlugin plugin;
    private final File settingsFile;
    private final File importedFile;
    private FileConfiguration settings;
    private FileConfiguration imported;
    private Connection connection;
    private boolean mysql;
    private String serverName;

    public PlotCheckDatabase(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        this.settingsFile = new File(plugin.getDataFolder(), "database/plotcheck_db.yml");
        this.importedFile = new File(plugin.getDataFolder(), "database/plotcheck_imported.yml");
    }

    public synchronized void initialize() throws Exception {
        if (!settingsFile.exists()) plugin.saveResource("database/plotcheck_db.yml", false);
        if (!importedFile.exists()) plugin.saveResource("database/plotcheck_imported.yml", false);
        settings = YamlConfiguration.loadConfiguration(settingsFile);
        imported = YamlConfiguration.loadConfiguration(importedFile);
        String type = settings.getString("type", "sqlite").toLowerCase(Locale.ROOT);
        mysql = "mysql".equals(type);
        if (!mysql && !"sqlite".equals(type)) throw new IllegalArgumentException("database/plotcheck_db.yml: type muss sqlite oder mysql sein");
        serverName = resolveServerName();
        connection = mysql ? openMysql() : openSqlite();
        createTables(connection, mysql);
        importPaperHistory();
        for (var player : Bukkit.getOnlinePlayers()) markJoin(player.getUniqueId(), player.getName());
        plugin.getLogger().info("PlotCheck-Datenbank bereit (Typ: " + (mysql ? "MySQL" : "SQLite")
                + ", Server: " + serverName + ", Config: " + settingsFile.getAbsolutePath() + ").");
        if (!mysql) {
            plugin.getLogger().warning("PlotCheck läuft mit SQLite. Online-Status ist nur auf dieser Serverinstallation sichtbar; für Event und Citybuild muss in der Plugin-Konfiguration MySQL aktiviert sein und dieselbe Datenbank verwendet werden.");
        }
    }

    private String resolveServerName() {
        String configured = settings.getString("server_name", "auto");
        if (configured != null && !configured.isBlank() && !configured.equalsIgnoreCase("auto")) return configured.trim();
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Object value = papi.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class)
                        .invoke(null, (OfflinePlayer) null, "%server_name%");
                if (value instanceof String name && !name.isBlank() && !name.equals("%server_name%")) return name;
            } catch (ReflectiveOperationException ignored) { }
        }
        return Bukkit.getServer().getName();
    }

    private Connection openSqlite() throws SQLException {
        String configured = settings.getString("sqlite.file", "database/plotcheck-data.db");
        File file = new File(configured);
        if (!file.isAbsolute()) file = new File(plugin.getDataFolder(), configured);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new SQLException("SQLite-Verzeichnis konnte nicht erstellt werden: " + parent);
        return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    private Connection openMysql() throws SQLException {
        return MySqlDatabase.open(settings.getString("mysql.host", "localhost"),
                settings.getInt("mysql.port", 3306), settings.getString("mysql.database", "global_plotcheck"),
                settings.getString("mysql.username", "minecraft"), settings.getString("mysql.password", ""),
                settings.getBoolean("mysql.useSSL", false));
    }

    private void createTables(Connection database, boolean mysqlDatabase) throws SQLException {
        try (Statement statement = database.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS plotcheck_players (id "
                    + (mysqlDatabase ? "BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT")
                    + ", uuid " + (mysqlDatabase ? "CHAR(36)" : "TEXT") + " NOT NULL UNIQUE, server VARCHAR(255) NOT NULL, last_online BIGINT NOT NULL DEFAULT 0, online BOOLEAN NOT NULL DEFAULT FALSE)");
            statement.execute("CREATE TABLE IF NOT EXISTS plotcheck_names (user_id "
                    + (mysqlDatabase ? "BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT")
                    + ", uuid " + (mysqlDatabase ? "CHAR(36)" : "TEXT") + " NOT NULL UNIQUE, player "
                    + (mysqlDatabase ? "VARCHAR(255)" : "TEXT") + " NOT NULL)");
        }
        if (mysqlDatabase) ensureMysqlPlayerNameCapacity(database);
    }

    private void ensureMysqlPlayerNameCapacity(Connection database) throws SQLException {
        int columnSize = 0;
        try (ResultSet columns = database.getMetaData().getColumns(database.getCatalog(), null, "plotcheck_names", "player")) {
            if (columns.next()) columnSize = columns.getInt("COLUMN_SIZE");
        }
        if (columnSize > 0 && columnSize < 255) {
            try (Statement statement = database.createStatement()) {
                statement.execute("ALTER TABLE plotcheck_names MODIFY COLUMN player VARCHAR(255) NOT NULL");
            }
        }
    }

    /** Merges PlotCheck rows, replacing a UUID only when the source login is newer. */
    public synchronized TransferResult importBetween(String from, String to) throws SQLException {
        String sourceType = normalizeType(from);
        String targetType = normalizeType(to);
        if (sourceType.equals(targetType)) throw new IllegalArgumentException("Source and target databases must differ.");
        try (Connection source = openDatabase(sourceType); Connection target = openDatabase(targetType)) {
            boolean sourceMysql = "mysql".equals(sourceType);
            boolean targetMysql = "mysql".equals(targetType);
            createTables(source, sourceMysql);
            createTables(target, targetMysql);
            Map<String, String> names = new HashMap<>();
            try (Statement query = source.createStatement(); ResultSet rows = query.executeQuery("SELECT uuid, player FROM plotcheck_names")) {
                while (rows.next()) names.put(rows.getString("uuid"), rows.getString("player"));
            }
            List<PlotCheckRow> users = new ArrayList<>();
            try (Statement query = source.createStatement(); ResultSet rows = query.executeQuery(
                    "SELECT uuid, server, last_online, online FROM plotcheck_players")) {
                while (rows.next()) users.add(new PlotCheckRow(rows.getString("uuid"), rows.getString("server"),
                        rows.getLong("last_online"), rows.getBoolean("online"), names.get(rows.getString("uuid"))));
            }

            boolean oldAutoCommit = target.getAutoCommit();
            target.setAutoCommit(false);
            int inserted = 0;
            int updated = 0;
            int skipped = 0;
            try {
                for (PlotCheckRow user : users) {
                    Long destinationTime = null;
                    try (PreparedStatement query = target.prepareStatement(
                            "SELECT last_online FROM plotcheck_players WHERE uuid = ?")) {
                        query.setString(1, user.uuid());
                        try (ResultSet row = query.executeQuery()) { if (row.next()) destinationTime = row.getLong(1); }
                    }
                    if (destinationTime == null) {
                        try (PreparedStatement insert = target.prepareStatement(
                                "INSERT INTO plotcheck_players(uuid,server,last_online,online) VALUES(?,?,?,?)")) {
                            insert.setString(1, user.uuid()); insert.setString(2, user.server());
                            insert.setLong(3, user.lastOnline()); insert.setBoolean(4, user.online()); insert.executeUpdate();
                        }
                        if (user.name() != null) upsertImportedName(target, targetMysql, user.uuid(), user.name());
                        inserted++;
                    } else if (user.lastOnline() > destinationTime) {
                        try (PreparedStatement update = target.prepareStatement(
                                "UPDATE plotcheck_players SET server = ?, last_online = ?, online = ? WHERE uuid = ?")) {
                            update.setString(1, user.server()); update.setLong(2, user.lastOnline());
                            update.setBoolean(3, user.online()); update.setString(4, user.uuid()); update.executeUpdate();
                        }
                        if (user.name() != null) upsertImportedName(target, targetMysql, user.uuid(), user.name());
                        updated++;
                    } else {
                        skipped++;
                    }
                }
                target.commit();
            } catch (SQLException exception) {
                target.rollback();
                throw exception;
            } finally {
                target.setAutoCommit(oldAutoCommit);
            }
            return new TransferResult(inserted, updated, skipped);
        }
    }

    private void upsertImportedName(Connection target, boolean targetMysql, String uuid, String name) throws SQLException {
        String sql = targetMysql
                ? "INSERT INTO plotcheck_names(uuid,player) VALUES(?,?) ON DUPLICATE KEY UPDATE player=VALUES(player)"
                : "INSERT INTO plotcheck_names(uuid,player) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET player=excluded.player";
        try (PreparedStatement statement = target.prepareStatement(sql)) {
            statement.setString(1, uuid); statement.setString(2, name); statement.executeUpdate();
        }
    }

    private Connection openDatabase(String type) throws SQLException {
        if ("sqlite".equals(type)) return openSqlite();
        return openMysql();
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!List.of("sqlite", "mysql").contains(normalized)) {
            throw new IllegalArgumentException("Database type must be sqlite or mysql.");
        }
        return normalized;
    }

    private record PlotCheckRow(String uuid, String server, long lastOnline, boolean online, String name) { }
    public record TransferResult(int inserted, int updated, int skipped) { }

    private void importPaperHistory() throws Exception {
        if (imported.getBoolean("servers." + serverName + ".done", false)) return;
        // The display name can change (for example, Bukkit's default "Paper"
        // name can later be replaced with the proxy name "Event"). Keep the
        // successful import marker stable across that rename on this server.
        if (hasPreviousSuccessfulImport()) {
            plugin.getLogger().info("Paper-Loginzeiten wurden bereits importiert; erneuter Import unter dem neuen Servernamen '"
                    + serverName + "' wird übersprungen.");
            return;
        }
        long cutoff = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000;
        int count = 0;
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            long lastSeen = player.getLastSeen();
            if (lastSeen <= 0 || lastSeen < cutoff || player.getName() == null) continue;
            upsert(player.getUniqueId(), player.getName(), serverName, lastSeen, false);
            count++;
        }
        imported.set("servers." + serverName + ".done", true);
        imported.set("servers." + serverName + ".imported_at",
                DateTimeFormatter.ofPattern("yyyy-dd-MM '|' hh:mm:ss a", Locale.ENGLISH)
                        .withZone(ZoneId.systemDefault()).format(Instant.now()));
        imported.set("servers." + serverName + ".players", count);
        imported.save(importedFile);
        plugin.getLogger().info("Paper-Loginzeiten importiert: " + count + " Spieler für " + serverName + ".");
    }

    private boolean hasPreviousSuccessfulImport() {
        var servers = imported.getConfigurationSection("servers");
        if (servers == null) return false;
        for (String previousName : servers.getKeys(false)) {
            if (imported.getBoolean("servers." + previousName + ".done", false)) return true;
        }
        return false;
    }

    public synchronized void markJoin(UUID uuid, String name) {
        try { upsert(uuid, name, serverName, System.currentTimeMillis(), true); }
        catch (SQLException e) { plugin.getLogger().warning("PlotCheck-Login konnte nicht gespeichert werden: " + e.getMessage()); }
    }

    public synchronized void markQuit(UUID uuid) {
        // A delayed quit from a previous backend must not overwrite a newer login
        // recorded by the server the player has just switched to.
        long quitAt = System.currentTimeMillis();
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE plotcheck_players SET online = FALSE, last_online = ? WHERE uuid = ? AND server = ? AND last_online <= ?")) {
            statement.setLong(1, quitAt);
            statement.setString(2, uuid.toString());
            statement.setString(3, serverName);
            statement.setLong(4, quitAt);
            statement.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().warning("PlotCheck-Logout konnte nicht gespeichert werden: " + e.getMessage()); }
    }

    public synchronized PlayerStatus getStatus(UUID uuid, String currentName) throws SQLException {
        updateName(uuid, currentName);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT server, last_online, online FROM plotcheck_players WHERE uuid = ? ORDER BY last_online DESC")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return new PlayerStatus(false, 0L, null);
                long newestLastOnline = rows.getLong("last_online");
                String newestServer = rows.getString("server");
                boolean online = false;
                String onlineServer = null;
                do {
                    if (rows.getBoolean("online")) {
                        online = true;
                        if (onlineServer == null) onlineServer = rows.getString("server");
                    }
                } while (rows.next());
                if (online) {
                    return new PlayerStatus(true, newestLastOnline, onlineServer);
                }
                return new PlayerStatus(false, newestLastOnline, newestServer);
            }
        }
    }

    private void upsert(UUID uuid, String name, String server, long lastOnline, boolean online) throws SQLException {
        updateName(uuid, name);
        String sql = mysql
                ? "INSERT INTO plotcheck_players(uuid,server,last_online,online) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE server=VALUES(server),last_online=VALUES(last_online),online=VALUES(online)"
                : "INSERT INTO plotcheck_players(uuid,server,last_online,online) VALUES(?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET server=excluded.server,last_online=MAX(plotcheck_players.last_online,excluded.last_online),online=excluded.online";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString()); statement.setString(2, server); statement.setLong(3, lastOnline); statement.setBoolean(4, online); statement.executeUpdate();
        }
    }

    private void updateName(UUID uuid, String name) throws SQLException {
        if (name == null || name.isBlank()) return;
        String existing = null;
        try (PreparedStatement query = connection.prepareStatement("SELECT player FROM plotcheck_names WHERE uuid = ?")) {
            query.setString(1, uuid.toString()); try (ResultSet row = query.executeQuery()) { if (row.next()) existing = row.getString(1); }
        }
        if (name.equals(existing)) return;
        String sql = mysql ? "INSERT INTO plotcheck_names(uuid,player) VALUES(?,?) ON DUPLICATE KEY UPDATE player=VALUES(player)"
                : "INSERT INTO plotcheck_names(uuid,player) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET player=excluded.player";
        try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.setString(1, uuid.toString()); statement.setString(2, name); statement.executeUpdate(); }
    }

    public String getServerName() { return serverName; }

    @Override public synchronized void close() {
        if (connection != null) try { connection.close(); } catch (SQLException e) { plugin.getLogger().warning("PlotCheck-DB schließen fehlgeschlagen: " + e.getMessage()); }
        connection = null;
    }

    public record PlayerStatus(boolean online, long lastOnline, String server) { }
}
