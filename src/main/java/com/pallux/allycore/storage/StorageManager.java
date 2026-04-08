package com.pallux.allycore.storage;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.CombatMode;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class StorageManager {

    private final AllyCore plugin;
    private Connection connection;
    private String dbType;

    public StorageManager(AllyCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        dbType = cfg.getString("database.type", "SQLITE").toUpperCase();

        try {
            if (dbType.equals("MYSQL")) {
                initMySQL(cfg);
            } else {
                initSQLite(cfg);
            }
            createTables();
            plugin.getLogger().info("Database initialized (" + dbType + ").");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database!", e);
        }
    }

    private void initSQLite(FileConfiguration cfg) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String fileName = cfg.getString("database.sqlite.file", "allycore_data.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        // WAL mode for performance
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL;");
        }
    }

    private void initMySQL(FileConfiguration cfg) throws Exception {
        String host = cfg.getString("database.mysql.host", "localhost");
        int port = cfg.getInt("database.mysql.port", 3306);
        String db = cfg.getString("database.mysql.database", "allycore");
        String user = cfg.getString("database.mysql.username", "root");
        String pass = cfg.getString("database.mysql.password", "");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection(url, user, pass);
    }

    private void createTables() throws SQLException {
        String autoIncrement = dbType.equals("MYSQL") ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        try (Statement st = connection.createStatement()) {
            // Ally data
            st.execute("""
                CREATE TABLE IF NOT EXISTS ally_data (
                    owner_uuid TEXT PRIMARY KEY,
                    owner_name TEXT,
                    custom_name TEXT,
                    level INTEGER DEFAULT 1,
                    xp REAL DEFAULT 0,
                    health_tier INTEGER DEFAULT 0,
                    attack_tier INTEGER DEFAULT 0,
                    defense_tier INTEGER DEFAULT 0,
                    speed_tier INTEGER DEFAULT 0,
                    regen_tier INTEGER DEFAULT 0,
                    armor_tier INTEGER DEFAULT 0,
                    weapon_tier INTEGER DEFAULT 0,
                    alive INTEGER DEFAULT 1,
                    summoned INTEGER DEFAULT 0,
                    following INTEGER DEFAULT 1,
                    mode TEXT DEFAULT 'DEFENSIVE',
                    current_health REAL DEFAULT -1
                )
            """);

            // Mementos
            st.execute("""
                CREATE TABLE IF NOT EXISTS mementos (
                    uuid TEXT PRIMARY KEY,
                    amount INTEGER DEFAULT 0
                )
            """);
        }
    }

    // ─── Ally Data ────────────────────────────────────────────────────────────

    public Map<UUID, AllyData> loadAllAllyData() {
        Map<UUID, AllyData> result = new HashMap<>();
        ensureConnection();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM ally_data")) {
            while (rs.next()) {
                AllyData d = mapRow(rs);
                result.put(d.getOwnerUUID(), d);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load ally data", e);
        }
        return result;
    }

    public void saveAllyData(AllyData data) {
        ensureConnection();
        String sql = dbType.equals("MYSQL")
                ? """
                  INSERT INTO ally_data
                  (owner_uuid,owner_name,custom_name,level,xp,health_tier,attack_tier,defense_tier,
                   speed_tier,regen_tier,armor_tier,weapon_tier,alive,summoned,following,mode,current_health)
                  VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                  ON DUPLICATE KEY UPDATE
                  owner_name=VALUES(owner_name),custom_name=VALUES(custom_name),level=VALUES(level),
                  xp=VALUES(xp),health_tier=VALUES(health_tier),attack_tier=VALUES(attack_tier),
                  defense_tier=VALUES(defense_tier),speed_tier=VALUES(speed_tier),regen_tier=VALUES(regen_tier),
                  armor_tier=VALUES(armor_tier),weapon_tier=VALUES(weapon_tier),alive=VALUES(alive),
                  summoned=VALUES(summoned),following=VALUES(following),mode=VALUES(mode),
                  current_health=VALUES(current_health)
                  """
                : """
                  INSERT OR REPLACE INTO ally_data
                  (owner_uuid,owner_name,custom_name,level,xp,health_tier,attack_tier,defense_tier,
                   speed_tier,regen_tier,armor_tier,weapon_tier,alive,summoned,following,mode,current_health)
                  VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                  """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, data.getOwnerUUID().toString());
            ps.setString(2, data.getOwnerName());
            ps.setString(3, data.getCustomName());
            ps.setInt(4, data.getLevel());
            ps.setDouble(5, data.getXp());
            ps.setInt(6, data.getHealthTier());
            ps.setInt(7, data.getAttackTier());
            ps.setInt(8, data.getDefenseTier());
            ps.setInt(9, data.getSpeedTier());
            ps.setInt(10, data.getRegenerationTier());
            ps.setInt(11, data.getArmorTier());
            ps.setInt(12, data.getWeaponTier());
            ps.setInt(13, data.isAlive() ? 1 : 0);
            ps.setInt(14, data.isSummoned() ? 1 : 0);
            ps.setInt(15, data.isFollowing() ? 1 : 0);
            ps.setString(16, data.getMode().name());
            ps.setDouble(17, data.getCurrentHealth());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save ally data for " + data.getOwnerUUID(), e);
        }
    }

    public void deleteAllyData(UUID ownerUUID) {
        ensureConnection();
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM ally_data WHERE owner_uuid=?")) {
            ps.setString(1, ownerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete ally data", e);
        }
    }

    private AllyData mapRow(ResultSet rs) throws SQLException {
        UUID ownerUUID = UUID.fromString(rs.getString("owner_uuid"));
        String ownerName = rs.getString("owner_name");
        AllyData d = new AllyData(ownerUUID, ownerName);
        d.setCustomName(rs.getString("custom_name"));
        d.setLevel(rs.getInt("level"));
        d.setXp(rs.getDouble("xp"));
        d.setHealthTier(rs.getInt("health_tier"));
        d.setAttackTier(rs.getInt("attack_tier"));
        d.setDefenseTier(rs.getInt("defense_tier"));
        d.setSpeedTier(rs.getInt("speed_tier"));
        d.setRegenerationTier(rs.getInt("regen_tier"));
        d.setArmorTier(rs.getInt("armor_tier"));
        d.setWeaponTier(rs.getInt("weapon_tier"));
        d.setAlive(rs.getInt("alive") == 1);
        d.setSummoned(rs.getInt("summoned") == 1);
        d.setFollowing(rs.getInt("following") == 1);
        d.setMode(CombatMode.fromString(rs.getString("mode")));
        d.setCurrentHealth(rs.getDouble("current_health"));
        return d;
    }

    // ─── Mementos ─────────────────────────────────────────────────────────────

    public long getMementos(UUID uuid) {
        ensureConnection();
        try (PreparedStatement ps = connection.prepareStatement("SELECT amount FROM mementos WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("amount");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get mementos", e);
        }
        return 0L;
    }

    public void setMementos(UUID uuid, long amount) {
        ensureConnection();
        String sql = dbType.equals("MYSQL")
                ? "INSERT INTO mementos (uuid, amount) VALUES(?,?) ON DUPLICATE KEY UPDATE amount=VALUES(amount)"
                : "INSERT OR REPLACE INTO mementos (uuid, amount) VALUES(?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set mementos", e);
        }
    }

    // ─── Connection ──────────────────────────────────────────────────────────

    private void ensureConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Lost database connection!", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }
}
