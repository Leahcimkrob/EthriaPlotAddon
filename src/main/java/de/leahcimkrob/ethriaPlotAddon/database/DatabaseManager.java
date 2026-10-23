package de.leahcimkrob.ethriaPlotAddon.database;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** JDBC persistence for plot-related records. */
public final class DatabaseManager implements AutoCloseable {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private Connection connection;
    private DatabaseType databaseType;
    private String serverName;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** Opens the configured database and creates the table when needed. */
    public synchronized void initialize() throws SQLException {
        close();
        File settingsFile = new File(plugin.getDataFolder(), "database/plotinfo_db.yml");
        if (!settingsFile.exists()) plugin.saveResource("database/plotinfo_db.yml", false);
        config = YamlConfiguration.loadConfiguration(settingsFile);
        String configuredType = config.getString("type", "sqlite").toLowerCase(Locale.ROOT);
        try {
            databaseType = DatabaseType.valueOf(configuredType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("database/plotinfo_db.yml: type must be sqlite or mysql", exception);
        }

        serverName = resolveServerName();

        connection = databaseType == DatabaseType.SQLITE
                ? openSqlite(config) : openMysql(config);
        createTables();
        plugin.getLogger().info("Plotinfo database connected (" + configuredType + ", server: " + serverName + ").");
    }

    private String resolveServerName() {
        String configured = config.getString("server_name", "auto");
        if (configured != null && !configured.isBlank() && !configured.equalsIgnoreCase("auto")) return configured.trim();
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Object value = papi.getMethod("setPlaceholders", OfflinePlayer.class, String.class)
                        .invoke(null, (OfflinePlayer) null, "%server_name%");
                if (value instanceof String name && !name.isBlank() && !name.equals("%server_name%")) return name;
            } catch (ReflectiveOperationException ignored) { }
        }
        return Bukkit.getServer().getName();
    }

    public String getServerName() { return serverName; }

    private Connection openSqlite(FileConfiguration config) throws SQLException {
        String configuredFile = config.getString("sqlite.file", "database/plotinfo-data.db");
        File file = new File(configuredFile);
        if (!file.isAbsolute()) file = new File(plugin.getDataFolder(), configuredFile);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new SQLException("Could not create SQLite database directory: " + parent);
        }
        Connection sqlite = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement statement = sqlite.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return sqlite;
    }

    private Connection openMysql(FileConfiguration config) throws SQLException {
        return MySqlDatabase.open(config.getString("mysql.host", "localhost"),
                config.getInt("mysql.port", 3306), config.getString("mysql.database", "global_plotinfo"),
                config.getString("mysql.username", "minecraft"), config.getString("mysql.password", ""),
                config.getBoolean("mysql.useSSL", false));
    }

    private void createTables() throws SQLException {
        renameLegacyTableIfNeeded(connection);
        String idType = databaseType == DatabaseType.SQLITE
                ? "INTEGER PRIMARY KEY AUTOINCREMENT" : "BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY";
        String textType = databaseType == DatabaseType.SQLITE ? "TEXT" : "VARCHAR(255)";
        String coordinateType = databaseType == DatabaseType.SQLITE ? "INTEGER" : "INT";
        String uuidType = databaseType == DatabaseType.SQLITE ? "TEXT" : "CHAR(36)";
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS plot_users (user_uuid " + uuidType
                    + " PRIMARY KEY, user_name " + textType + " NOT NULL)"
                    + (databaseType == DatabaseType.MYSQL ? " ENGINE=InnoDB" : ""));
        }
        String table = "CREATE TABLE IF NOT EXISTS plot_info ("
                + "id " + idType + ","
                + "server " + textType + " NOT NULL,"
                + "world " + textType + " NOT NULL,"
                + "plot_x " + coordinateType + " NOT NULL,"
                + "plot_y " + coordinateType + " NOT NULL,"
                + "owner_id " + uuidType + ","
                + "teamler_id " + uuidType + " NOT NULL,"
                + "x " + coordinateType + " NOT NULL,"
                + "y " + coordinateType + " NOT NULL,"
                + "z " + coordinateType + " NOT NULL,"
                + "recorded_at " + (databaseType == DatabaseType.SQLITE ? "TEXT" : "TIMESTAMP") + " NOT NULL,"
                + "note TEXT,"
                + "seen BOOLEAN NOT NULL DEFAULT TRUE,"
                + "FOREIGN KEY(owner_id) REFERENCES plot_users(user_uuid),"
                + "FOREIGN KEY(teamler_id) REFERENCES plot_users(user_uuid)"
                + ")" + (databaseType == DatabaseType.MYSQL ? " ENGINE=InnoDB" : "");
        try (Statement statement = connection.createStatement()) {
            statement.execute(table);
            ensureColumn("plot_info", "owner_id", uuidType);
            ensureColumn("plot_info", "teamler_id", uuidType);
            ensureColumn("plot_info", "seen", "BOOLEAN NOT NULL DEFAULT TRUE");
            migrateLegacyUsers();
            if (databaseType == DatabaseType.SQLITE) {
                statement.execute("CREATE INDEX IF NOT EXISTS idx_plot_info_lookup "
                        + "ON plot_info(server, world, plot_x, plot_y, recorded_at)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_plot_info_unseen "
                        + "ON plot_info(server, owner_id, seen)");
            } else {
                try {
                    statement.execute("CREATE INDEX idx_plot_info_lookup "
                            + "ON plot_info(server, world, plot_x, plot_y, recorded_at)");
                } catch (SQLException exception) {
                    if (!indexAlreadyExists(exception)) throw exception;
                }
                try {
                    statement.execute("CREATE INDEX idx_plot_info_unseen "
                            + "ON plot_info(server, owner_id, seen)");
                } catch (SQLException exception) {
                    if (!indexAlreadyExists(exception)) throw exception;
                }
            }
        }
    }

    private void renameLegacyTableIfNeeded(Connection database) throws SQLException {
        if (!tableExists(database, "plot_records") || tableExists(database, "plot_info")) return;
        String renameSql = databaseType == DatabaseType.SQLITE
                ? "ALTER TABLE plot_records RENAME TO plot_info"
                : "RENAME TABLE plot_records TO plot_info";
        try (Statement statement = database.createStatement()) {
            statement.execute(renameSql);
        }
    }

    private boolean tableExists(Connection database, String table) throws SQLException {
        try (ResultSet tables = database.getMetaData().getTables(database.getCatalog(), null, table, new String[]{"TABLE"})) {
            if (tables.next()) return true;
        }
        try (ResultSet tables = database.getMetaData().getTables(database.getCatalog(), null,
                table.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private void ensureColumn(String table, String column, String type) throws SQLException {
        if (hasColumn(table, column)) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }

    private boolean hasColumn(String table, String column) throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, table, column)) {
            if (columns.next()) return true;
        }
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null,
                table.toUpperCase(Locale.ROOT), column.toUpperCase(Locale.ROOT))) {
            return columns.next();
        }
    }

    private void migrateLegacyUsers() throws SQLException {
        boolean hasExecutor = hasColumn("plot_info", "executor");
        boolean hasOwner = hasColumn("plot_info", "plot_owner");
        if (!hasExecutor && !hasOwner) return;
        String legacySql = "SELECT id" + (hasOwner ? ", plot_owner" : ", NULL AS plot_owner")
                + (hasExecutor ? ", executor" : ", NULL AS executor")
                + " FROM plot_info WHERE teamler_id IS NULL OR teamler_id = '' OR owner_id IS NULL";
        java.util.List<LegacyUserRow> legacyRows = new java.util.ArrayList<>();
        try (Statement query = connection.createStatement(); ResultSet rows = query.executeQuery(legacySql)) {
            while (rows.next()) {
                legacyRows.add(new LegacyUserRow(rows.getLong("id"), rows.getString("plot_owner"),
                        rows.getString("executor")));
            }
        }
        for (LegacyUserRow row : legacyRows) {
            UUID ownerId = parseUuid(row.ownerValue());
            UUID teamlerId = parseUuid(row.teamlerValue());
            if (ownerId != null) upsertUser(ownerId, row.ownerValue());
            if (teamlerId != null) upsertUser(teamlerId, row.teamlerValue());
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE plot_info SET owner_id = ?, teamler_id = ? WHERE id = ?")) {
                if (ownerId == null) update.setNull(1, Types.VARCHAR); else update.setString(1, ownerId.toString());
                if (teamlerId == null) update.setNull(2, Types.VARCHAR); else update.setString(2, teamlerId.toString());
                update.setLong(3, row.id());
                update.executeUpdate();
            }
        }
    }

    private record LegacyUserRow(long id, String ownerValue, String teamlerValue) { }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private void upsertUser(UUID id, String name) throws SQLException {
        if (id == null) return;
        String sql = databaseType == DatabaseType.SQLITE
                ? "INSERT INTO plot_users (user_uuid, user_name) VALUES (?, ?) "
                + "ON CONFLICT(user_uuid) DO UPDATE SET user_name = excluded.user_name"
                : "INSERT INTO plot_users (user_uuid, user_name) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE user_name = VALUES(user_name)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.setString(2, name == null || name.isBlank() ? id.toString() : name);
            statement.executeUpdate();
        }
    }

    private boolean indexAlreadyExists(SQLException exception) {
        return "23000".equals(exception.getSQLState()) || exception.getErrorCode() == 1061;
    }

    public synchronized long insert(PlotRecord record) throws SQLException {
        ensureOpen();
        Objects.requireNonNull(record.teamlerId(), "teamlerId");
        upsertUser(record.teamlerId(), record.teamlerName());
        if (record.ownerId() != null) upsertUser(record.ownerId(), record.ownerName());
        boolean legacyOwner = hasColumn("plot_info", "plot_owner");
        boolean legacyExecutor = hasColumn("plot_info", "executor");
        String columns = "server, world, plot_x, plot_y, owner_id, teamler_id, x, y, z, recorded_at, note, seen"
                + (legacyOwner ? ", plot_owner" : "") + (legacyExecutor ? ", executor" : "");
        int parameterCount = 12 + (legacyOwner ? 1 : 0) + (legacyExecutor ? 1 : 0);
        String sql = "INSERT INTO plot_info (" + columns + ") VALUES ("
                + String.join(", ", java.util.Collections.nCopies(parameterCount, "?")) + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, record.server());
            statement.setString(2, record.world());
            statement.setInt(3, record.plotX());
            statement.setInt(4, record.plotY());
            if (record.ownerId() == null) statement.setNull(5, Types.VARCHAR);
            else statement.setString(5, record.ownerId().toString());
            statement.setString(6, record.teamlerId().toString());
            statement.setInt(7, record.x());
            statement.setInt(8, record.y());
            statement.setInt(9, record.z());
            if (databaseType == DatabaseType.SQLITE) statement.setString(10, record.recordedAt().toString());
            else statement.setTimestamp(10, Timestamp.from(record.recordedAt()));
            if (record.note() == null) statement.setNull(11, Types.VARCHAR);
            else statement.setString(11, record.note());
            statement.setBoolean(12, record.ownerId() == null || record.ownerId().equals(record.teamlerId()));
            int next = 13;
            if (legacyOwner) statement.setString(next++, record.ownerName());
            if (legacyExecutor) statement.setString(next, record.teamlerName());
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        }
    }

    public synchronized void updateNote(long id, String note, UUID editorId) throws SQLException {
        ensureOpen();
        if (note != null && note.length() > 256) throw new IllegalArgumentException("Note cannot exceed 256 characters.");
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE plot_info SET note = ?, seen = CASE WHEN owner_id = ? THEN TRUE ELSE FALSE END WHERE id = ?")) {
            if (note == null) statement.setNull(1, Types.VARCHAR);
            else statement.setString(1, note);
            if (editorId == null) statement.setNull(2, Types.VARCHAR);
            else statement.setString(2, editorId.toString());
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    public synchronized boolean deletePlotRecord(long id) throws SQLException {
        ensureOpen();
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM plot_info WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public synchronized int deletePlotRecords(String server, String world, int plotX, int plotY) throws SQLException {
        ensureOpen();
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM plot_info WHERE server = ? AND world = ? AND plot_x = ? AND plot_y = ?")) {
            statement.setString(1, server);
            statement.setString(2, world);
            statement.setInt(3, plotX);
            statement.setInt(4, plotY);
            return statement.executeUpdate();
        }
    }

    public synchronized java.util.List<PlotRecordEntry> getPlotRecordEntries(String server, String world,
                                                                              int plotX, int plotY)
            throws SQLException {
        ensureOpen();
        java.util.List<PlotRecordEntry> entries = new java.util.ArrayList<>();
        String sql = recordSelectSql()
                + "FROM plot_info LEFT JOIN plot_users owner_user ON owner_user.user_uuid = plot_info.owner_id "
                + "LEFT JOIN plot_users teamler_user ON teamler_user.user_uuid = plot_info.teamler_id "
                + "WHERE server = ? AND world = ? AND plot_x = ? AND plot_y = ? "
                + "ORDER BY recorded_at ASC, id ASC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, server);
            statement.setString(2, world);
            statement.setInt(3, plotX);
            statement.setInt(4, plotY);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    entries.add(readRecord(rows));
                }
            }
        }
        return entries;
    }

    public synchronized java.util.List<PlotRecordEntry> getPlotRecordEntriesByOwner(
            String server, String world, String ownerName) throws SQLException {
        ensureOpen();
        java.util.List<PlotRecordEntry> entries = new java.util.ArrayList<>();
        String ownerNameColumn = hasColumn("plot_info", "plot_owner")
                ? "COALESCE(owner_user.user_name, plot_info.plot_owner)" : "owner_user.user_name";
        String sql = recordSelectSql()
                + "FROM plot_info LEFT JOIN plot_users owner_user ON owner_user.user_uuid = plot_info.owner_id "
                + "LEFT JOIN plot_users teamler_user ON teamler_user.user_uuid = plot_info.teamler_id "
                + "WHERE server = ? AND world = ? AND note IS NOT NULL AND note <> '' "
                + "AND LOWER(" + ownerNameColumn + ") = LOWER(?) "
                + "ORDER BY plot_x ASC, plot_y ASC, recorded_at ASC, id ASC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, server);
            statement.setString(2, world);
            statement.setString(3, ownerName);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) entries.add(readRecord(rows));
            }
        }
        return entries;
    }

    /** Returns changed, not-yet-seen note records for an owner on this server, across all worlds. */
    public synchronized java.util.List<PlotRecordEntry> getUnseenPlotInfoEntries(String server, UUID ownerId)
            throws SQLException {
        ensureOpen();
        java.util.List<PlotRecordEntry> entries = new java.util.ArrayList<>();
        String sql = recordSelectSql()
                + "FROM plot_info LEFT JOIN plot_users owner_user ON owner_user.user_uuid = plot_info.owner_id "
                + "LEFT JOIN plot_users teamler_user ON teamler_user.user_uuid = plot_info.teamler_id "
                + "WHERE server = ? AND owner_id = ? AND seen = FALSE AND note IS NOT NULL AND note <> '' "
                + "ORDER BY world ASC, plot_x ASC, plot_y ASC, recorded_at ASC, id ASC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, server);
            statement.setString(2, ownerId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) entries.add(readRecord(rows));
            }
        }
        return entries;
    }

    /** Marks only the delivered records belonging to the player as seen. */
    public synchronized void markPlotInfoEntriesSeen(UUID ownerId, List<Long> entryIds) throws SQLException {
        ensureOpen();
        if (entryIds.isEmpty()) return;
        String placeholders = String.join(", ", java.util.Collections.nCopies(entryIds.size(), "?"));
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE plot_info SET seen = TRUE WHERE owner_id = ? AND id IN (" + placeholders + ")")) {
            statement.setString(1, ownerId.toString());
            for (int i = 0; i < entryIds.size(); i++) statement.setLong(i + 2, entryIds.get(i));
            statement.executeUpdate();
        }
    }

    public synchronized PlotRecordEntry getPlotRecordEntry(long id) throws SQLException {
        ensureOpen();
        String sql = recordSelectSql()
                + "FROM plot_info LEFT JOIN plot_users owner_user ON owner_user.user_uuid = plot_info.owner_id "
                + "LEFT JOIN plot_users teamler_user ON teamler_user.user_uuid = plot_info.teamler_id WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return null;
                return readRecord(rows);
            }
        }
    }

    private String recordSelectSql() throws SQLException {
        String ownerName = hasColumn("plot_info", "plot_owner")
                ? "COALESCE(owner_user.user_name, plot_info.plot_owner)" : "owner_user.user_name";
        String teamlerName = hasColumn("plot_info", "executor")
                ? "COALESCE(teamler_user.user_name, plot_info.executor)" : "teamler_user.user_name";
        return "SELECT plot_info.id, server, world, plot_x, plot_y, owner_id, teamler_id, x, y, z, recorded_at, note, "
                + ownerName + " AS owner_name, " + teamlerName + " AS teamler_name ";
    }

    private PlotRecordEntry readRecord(ResultSet rows) throws SQLException {
        String timestamp = rows.getString("recorded_at");
        Instant recordedAt;
        try { recordedAt = Instant.parse(timestamp); }
        catch (RuntimeException ignored) { recordedAt = rows.getTimestamp("recorded_at").toInstant(); }
        PlotRecord record = new PlotRecord(rows.getString("server"), rows.getString("world"),
                rows.getInt("plot_x"), rows.getInt("plot_y"), parseUuid(rows.getString("owner_id")),
                parseUuid(rows.getString("teamler_id")), rows.getString("owner_name"), rows.getString("teamler_name"),
                rows.getInt("x"), rows.getInt("y"), rows.getInt("z"), recordedAt, rows.getString("note"));
        return new PlotRecordEntry(rows.getLong("id"), record);
    }

    /** Copies all plot records between the configured SQLite and MySQL databases. */
    public synchronized void convert(String from, String to) throws SQLException {
        String sourceType = normalizeType(from);
        String targetType = normalizeType(to);
        if (sourceType.equals(targetType)) {
            throw new IllegalArgumentException("Source and target databases must differ.");
        }

        DatabaseType originalType = databaseType;
        close();
        try {
            try (Connection source = openConnection(sourceType);
                 Connection target = openConnection(targetType)) {
                databaseType = "sqlite".equals(sourceType) ? DatabaseType.SQLITE : DatabaseType.MYSQL;
                connection = source;
                createTables();
                boolean sourceLegacyOwner = hasColumn("plot_info", "plot_owner");
                boolean sourceLegacyExecutor = hasColumn("plot_info", "executor");
                databaseType = "sqlite".equals(targetType) ? DatabaseType.SQLITE : DatabaseType.MYSQL;
                connection = target;
                createTables();
                target.setAutoCommit(false);
                try {
                    try (Statement clear = target.createStatement()) {
                        clear.executeUpdate("DELETE FROM plot_info");
                        clear.executeUpdate("DELETE FROM plot_users");
                    }
                    try (Statement query = source.createStatement();
                         ResultSet users = query.executeQuery("SELECT user_uuid, user_name FROM plot_users")) {
                        while (users.next()) upsertUser(UUID.fromString(users.getString("user_uuid")),
                                users.getString("user_name"));
                    }
                    String sourceSql = "SELECT server, world, plot_x, plot_y, owner_id, teamler_id, x, y, z, recorded_at, note, seen"
                            + (sourceLegacyOwner ? ", plot_owner" : "")
                            + (sourceLegacyExecutor ? ", executor" : "") + " FROM plot_info";
                    try (Statement query = source.createStatement(); ResultSet rows = query.executeQuery(sourceSql)) {
                        boolean legacyOwner = hasColumn("plot_info", "plot_owner");
                        boolean legacyExecutor = hasColumn("plot_info", "executor");
                        String columns = "server, world, plot_x, plot_y, owner_id, teamler_id, x, y, z, recorded_at, note, seen"
                                + (legacyOwner ? ", plot_owner" : "") + (legacyExecutor ? ", executor" : "");
                        int count = 12 + (legacyOwner ? 1 : 0) + (legacyExecutor ? 1 : 0);
                        String insertSql = "INSERT INTO plot_info (" + columns + ") VALUES ("
                                + String.join(", ", java.util.Collections.nCopies(count, "?")) + ")";
                        try (PreparedStatement insert = target.prepareStatement(insertSql)) {
                            int batchCount = 0;
                            while (rows.next()) {
                                for (int i = 1; i <= 12; i++) insert.setObject(i, rows.getObject(i));
                                int next = 13;
                                String ownerName = null;
                                String teamlerName = null;
                                UUID ownerId = parseUuid(rows.getString("owner_id"));
                                UUID teamlerId = parseUuid(rows.getString("teamler_id"));
                                if (ownerId != null) ownerName = userName(source, ownerId);
                                else if (sourceLegacyOwner) ownerName = rows.getString("plot_owner");
                                if (teamlerId != null) teamlerName = userName(source, teamlerId);
                                else if (sourceLegacyExecutor) teamlerName = rows.getString("executor");
                                if (legacyOwner) insert.setString(next++, ownerName);
                                if (legacyExecutor) insert.setString(next, teamlerName);
                                insert.addBatch();
                                if (++batchCount % 500 == 0) insert.executeBatch();
                            }
                            insert.executeBatch();
                        }
                    }
                    target.commit();
                } catch (SQLException exception) {
                    target.rollback();
                    throw exception;
                } finally {
                    target.setAutoCommit(true);
                    connection = null;
                }
            }
        } finally {
            databaseType = originalType;
            if (originalType != null) {
                if (originalType == DatabaseType.SQLITE) connection = openConnection("sqlite");
                else connection = openConnection("mysql");
            }
        }
    }

    /** Merges plotinfo records without clearing the destination. User names are updated only from newer activity. */
    public synchronized TransferResult importBetween(String from, String to) throws SQLException {
        String sourceType = normalizeType(from);
        String targetType = normalizeType(to);
        if (sourceType.equals(targetType)) throw new IllegalArgumentException("Source and target databases must differ.");
        Connection activeConnection = connection;
        DatabaseType activeType = databaseType;
        List<TransferPlotInfoEntry> sourceEntries = new java.util.ArrayList<>();
        List<UserEntry> sourceUsers = new java.util.ArrayList<>();
        try (Connection source = openConnection(sourceType); Connection target = openConnection(targetType)) {
            databaseType = "sqlite".equals(sourceType) ? DatabaseType.SQLITE : DatabaseType.MYSQL;
            connection = source;
            createTables();
            try (Statement query = source.createStatement();
                 ResultSet rows = query.executeQuery("SELECT user_uuid, user_name FROM plot_users")) {
                while (rows.next()) sourceUsers.add(new UserEntry(UUID.fromString(rows.getString("user_uuid")), rows.getString("user_name")));
            }
            String sourceSql = recordSelectSql() + ", plot_info.seen AS seen FROM plot_info LEFT JOIN plot_users owner_user ON owner_user.user_uuid = plot_info.owner_id "
                    + "LEFT JOIN plot_users teamler_user ON teamler_user.user_uuid = plot_info.teamler_id";
            try (Statement query = source.createStatement(); ResultSet rows = query.executeQuery(sourceSql)) {
                while (rows.next()) sourceEntries.add(new TransferPlotInfoEntry(readRecord(rows), rows.getBoolean("seen")));
            }

            databaseType = "sqlite".equals(targetType) ? DatabaseType.SQLITE : DatabaseType.MYSQL;
            connection = target;
            createTables();
            boolean targetLegacyOwner = hasColumn("plot_info", "plot_owner");
            boolean targetLegacyExecutor = hasColumn("plot_info", "executor");
            boolean targetMysql = databaseType == DatabaseType.MYSQL;
            boolean oldAutoCommit = target.getAutoCommit();
            target.setAutoCommit(false);
            int inserted = 0;
            int skipped = 0;
            try {
                for (UserEntry user : sourceUsers) {
                    Instant sourceActivity = latestUserActivity(source, user.uuid());
                    Instant targetActivity = latestUserActivity(target, user.uuid());
                    boolean exists;
                    try (PreparedStatement query = target.prepareStatement("SELECT 1 FROM plot_users WHERE user_uuid = ?")) {
                        query.setString(1, user.uuid().toString());
                        try (ResultSet row = query.executeQuery()) { exists = row.next(); }
                    }
                    if (!exists || (sourceActivity != null && (targetActivity == null || sourceActivity.isAfter(targetActivity)))) {
                        upsertUser(user.uuid(), user.name());
                    }
                }
                for (TransferPlotInfoEntry entry : sourceEntries) {
                    if (containsRecord(target, targetMysql, entry.entry().record())) {
                        skipped++;
                        continue;
                    }
                    insertImportedRecord(target, targetMysql, entry, targetLegacyOwner, targetLegacyExecutor);
                    inserted++;
                }
                target.commit();
            } catch (SQLException exception) {
                target.rollback();
                throw exception;
            } finally {
                target.setAutoCommit(oldAutoCommit);
            }
            return new TransferResult(inserted, skipped);
        } finally {
            connection = activeConnection;
            databaseType = activeType;
        }
    }

    private Instant latestUserActivity(Connection database, UUID userId) throws SQLException {
        try (PreparedStatement statement = database.prepareStatement(
                "SELECT MAX(recorded_at) FROM plot_info WHERE owner_id = ? OR teamler_id = ?")) {
            statement.setString(1, userId.toString());
            statement.setString(2, userId.toString());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                String value = row.getString(1);
                if (value == null) return null;
                try { return Instant.parse(value); }
                catch (RuntimeException ignored) { return row.getTimestamp(1).toInstant(); }
            }
        }
    }

    private boolean containsRecord(Connection target, boolean targetMysql, PlotRecord record) throws SQLException {
        String nullSafe = targetMysql ? " <=> ?" : " IS ?";
        String sql = "SELECT 1 FROM plot_info WHERE server = ? AND world = ? AND plot_x = ? AND plot_y = ? "
                + "AND owner_id" + nullSafe + " AND teamler_id = ? AND x = ? AND y = ? AND z = ? AND recorded_at = ? AND note" + nullSafe;
        try (PreparedStatement statement = target.prepareStatement(sql)) {
            statement.setString(1, record.server());
            statement.setString(2, record.world());
            statement.setInt(3, record.plotX());
            statement.setInt(4, record.plotY());
            if (record.ownerId() == null) statement.setNull(5, Types.VARCHAR); else statement.setString(5, record.ownerId().toString());
            statement.setString(6, record.teamlerId().toString());
            statement.setInt(7, record.x()); statement.setInt(8, record.y()); statement.setInt(9, record.z());
            if (targetMysql) statement.setTimestamp(10, Timestamp.from(record.recordedAt()));
            else statement.setString(10, record.recordedAt().toString());
            if (record.note() == null) statement.setNull(11, Types.VARCHAR); else statement.setString(11, record.note());
            try (ResultSet row = statement.executeQuery()) { return row.next(); }
        }
    }

    private void insertImportedRecord(Connection target, boolean targetMysql, TransferPlotInfoEntry entry,
                                      boolean legacyOwner, boolean legacyExecutor) throws SQLException {
        PlotRecord record = entry.entry().record();
        String columns = "server, world, plot_x, plot_y, owner_id, teamler_id, x, y, z, recorded_at, note, seen"
                + (legacyOwner ? ", plot_owner" : "") + (legacyExecutor ? ", executor" : "");
        int count = 12 + (legacyOwner ? 1 : 0) + (legacyExecutor ? 1 : 0);
        String sql = "INSERT INTO plot_info (" + columns + ") VALUES ("
                + String.join(", ", java.util.Collections.nCopies(count, "?")) + ")";
        try (PreparedStatement statement = target.prepareStatement(sql)) {
            statement.setString(1, record.server()); statement.setString(2, record.world());
            statement.setInt(3, record.plotX()); statement.setInt(4, record.plotY());
            if (record.ownerId() == null) statement.setNull(5, Types.VARCHAR); else statement.setString(5, record.ownerId().toString());
            statement.setString(6, record.teamlerId().toString());
            statement.setInt(7, record.x()); statement.setInt(8, record.y()); statement.setInt(9, record.z());
            if (targetMysql) statement.setTimestamp(10, Timestamp.from(record.recordedAt()));
            else statement.setString(10, record.recordedAt().toString());
            if (record.note() == null) statement.setNull(11, Types.VARCHAR); else statement.setString(11, record.note());
            statement.setBoolean(12, entry.seen());
            int next = 13;
            if (legacyOwner) statement.setString(next++, record.ownerName());
            if (legacyExecutor) statement.setString(next, record.teamlerName());
            statement.executeUpdate();
        }
    }

    private record UserEntry(UUID uuid, String name) { }
    private record TransferPlotInfoEntry(PlotRecordEntry entry, boolean seen) { }
    public record TransferResult(int inserted, int skipped) { }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!List.of("sqlite", "mysql").contains(normalized)) {
            throw new IllegalArgumentException("Database type must be sqlite or mysql.");
        }
        return normalized;
    }

    private Connection openConnection(String type) throws SQLException {
        if ("sqlite".equals(type)) {
            String configuredFile = config.getString("sqlite.file", "database/plotinfo-data.db");
            File file = new File(configuredFile);
            if (!file.isAbsolute()) file = new File(plugin.getDataFolder(), configuredFile);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new SQLException("Could not create SQLite database directory: " + parent);
            }
            Connection sqlite = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement statement = sqlite.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return sqlite;
        }
        return MySqlDatabase.open(config.getString("mysql.host", "localhost"),
                config.getInt("mysql.port", 3306), config.getString("mysql.database", "global_plotinfo"),
                config.getString("mysql.username", "minecraft"), config.getString("mysql.password", ""),
                config.getBoolean("mysql.useSSL", false));
    }

    private String userName(Connection database, UUID id) throws SQLException {
        try (PreparedStatement statement = database.prepareStatement(
                "SELECT user_name FROM plot_users WHERE user_uuid = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : id.toString();
            }
        }
    }

    private void ensureOpen() throws SQLException {
        if (connection == null || connection.isClosed()) throw new SQLException("Database is not initialized.");
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not close database connection: " + exception.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    private enum DatabaseType { SQLITE, MYSQL }

    public record PlotRecord(String server, String world, int plotX, int plotY,
                             UUID ownerId, UUID teamlerId, String ownerName, String teamlerName,
                             int x, int y, int z, Instant recordedAt, String note) {
        public PlotRecord {
            Objects.requireNonNull(server, "server");
            Objects.requireNonNull(world, "world");
            Objects.requireNonNull(recordedAt, "recordedAt");
        }

        public static PlotRecord now(String server, String world, int plotX, int plotY,
                                     UUID ownerId, String ownerName, UUID teamlerId, String teamlerName,
                                     int x, int y, int z, String note) {
            return new PlotRecord(server, world, plotX, plotY, ownerId, teamlerId, ownerName, teamlerName,
                    x, y, z, Instant.now(), note);
        }
    }

    public record PlotRecordEntry(long id, PlotRecord record) { }
}
