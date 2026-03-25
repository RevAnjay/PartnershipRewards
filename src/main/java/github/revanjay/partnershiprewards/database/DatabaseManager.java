package github.revanjay.partnershiprewards.database;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.ActiveQuest;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.model.QuestType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import java.sql.*;
import java.time.Instant;
import java.util.*;

@Getter
public class DatabaseManager {
    
    private final PartnershipRewards plugin;
    private HikariDataSource dataSource;
    
    public DatabaseManager(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    public void initialize() {
        setupDataSource();
        createTables();
    }
    
    private void setupDataSource() {
        HikariConfig config = new HikariConfig();
        
        String dbType = plugin.getConfig().getString("database.type", "SQLITE");
        
        if (dbType.equalsIgnoreCase("MYSQL")) {
            String host = plugin.getConfig().getString("database.mysql.host");
            int port = plugin.getConfig().getInt("database.mysql.port");
            String database = plugin.getConfig().getString("database.mysql.database");
            String username = plugin.getConfig().getString("database.mysql.username");
            String password = plugin.getConfig().getString("database.mysql.password");
            
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            String fileName = plugin.getConfig().getString("database.sqlite.file", "partnerships.db");
            config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + "/" + fileName);
            config.setDriverClassName("org.sqlite.JDBC");
        }
        
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool.maximum-pool-size", 10));
        config.setMinimumIdle(plugin.getConfig().getInt("database.pool.minimum-idle", 2));
        config.setConnectionTimeout(plugin.getConfig().getInt("database.pool.connection-timeout", 30000));
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        this.dataSource = new HikariDataSource(config);
    }
    
    private void createTables() {
        String createPartnerships = """
            CREATE TABLE IF NOT EXISTS partnerships (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                player1_uuid VARCHAR(36) NOT NULL,
                player2_uuid VARCHAR(36) NOT NULL,
                started_at BIGINT NOT NULL,
                last_reward_check BIGINT NOT NULL,
                level INTEGER DEFAULT 1,
                xp INTEGER DEFAULT 0,
                last_quest_complete BIGINT DEFAULT 0,
                UNIQUE KEY unique_partnership (player1_uuid, player2_uuid)
            )
        """;
        
        String createActiveQuests = """
            CREATE TABLE IF NOT EXISTS active_quests (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                partnership_id INTEGER NOT NULL,
                quest_type VARCHAR(50) NOT NULL,
                target VARCHAR(100),
                required_amount INTEGER NOT NULL,
                progress INTEGER DEFAULT 0,
                created_at BIGINT NOT NULL,
                FOREIGN KEY (partnership_id) REFERENCES partnerships(id) ON DELETE CASCADE
            )
        """;
        
        boolean isSqlite = plugin.getConfig().getString("database.type", "SQLITE").equalsIgnoreCase("SQLITE");
        
        if (isSqlite) {
            createPartnerships = createPartnerships.replace("AUTO_INCREMENT", "AUTOINCREMENT");
            createPartnerships = createPartnerships.replace("UNIQUE KEY unique_partnership (player1_uuid, player2_uuid)", "UNIQUE (player1_uuid, player2_uuid)");
            createActiveQuests = createActiveQuests.replace("AUTO_INCREMENT", "AUTOINCREMENT");
            createActiveQuests = createActiveQuests.replace(",\n                FOREIGN KEY (partnership_id) REFERENCES partnerships(id) ON DELETE CASCADE", "");
        }
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createPartnerships);
            stmt.execute(createActiveQuests);
            addColumnIfNotExists(conn, "partnerships", "level", "INTEGER DEFAULT 1");
            addColumnIfNotExists(conn, "partnerships", "xp", "INTEGER DEFAULT 0");
            addColumnIfNotExists(conn, "partnerships", "last_quest_complete", "BIGINT DEFAULT 0");
            addColumnIfNotExists(conn, "partnerships", "pvp_enabled", "INTEGER DEFAULT 0");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void addColumnIfNotExists(Connection conn, String table, String column, String definition) {
        boolean isSqlite = plugin.getConfig().getString("database.type", "SQLITE").equalsIgnoreCase("SQLITE");
        boolean columnExists = false;
        
        try {
            if (isSqlite) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
                    while (rs.next()) {
                        if (rs.getString("name").equalsIgnoreCase(column)) {
                            columnExists = true;
                            break;
                        }
                    }
                }
            } else {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeQuery("SELECT " + column + " FROM " + table + " LIMIT 1");
                    columnExists = true;
                } catch (SQLException e) {
                    columnExists = false;
                }
            }
            
            if (!columnExists) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                    plugin.getLogger().info("Successfully added column '" + column + "' to table '" + table + "'");
                } catch (SQLException e2) {
                    plugin.getLogger().severe("Failed to add column '" + column + "' to table '" + table + "': " + e2.getMessage());
                    e2.printStackTrace();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking column existence for '" + column + "': " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void createPartnership(UUID player1, UUID player2) {
        String sql = "INSERT INTO partnerships (player1_uuid, player2_uuid, started_at, last_reward_check) VALUES (?, ?, ?, ?)";
        
        long now = Instant.now().getEpochSecond();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, player1.toString());
            stmt.setString(2, player2.toString());
            stmt.setLong(3, now);
            stmt.setLong(4, now);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating partnership: " + e.getMessage());
        }
    }
    
    public void deletePartnership(UUID player) {
        String sql = "DELETE FROM partnerships WHERE player1_uuid = ? OR player2_uuid = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            stmt.setString(2, player.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error deleting partnership: " + e.getMessage());
        }
    }
    
    public Partnership getPartnership(UUID player) {
        String sql = "SELECT * FROM partnerships WHERE player1_uuid = ? OR player2_uuid = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            stmt.setString(2, player.toString());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Partnership.builder()
                    .id(rs.getInt("id"))
                    .player1(UUID.fromString(rs.getString("player1_uuid")))
                    .player2(UUID.fromString(rs.getString("player2_uuid")))
                    .startedAt(rs.getLong("started_at"))
                    .lastRewardCheck(rs.getLong("last_reward_check"))
                    .level(rs.getInt("level"))
                    .xp(rs.getInt("xp"))
                    .lastQuestComplete(rs.getLong("last_quest_complete"))
                    .pvpEnabled(rs.getInt("pvp_enabled") == 1)
                    .build();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting partnership: " + e.getMessage());
        }
        
        return null;
    }
    
    public List<Partnership> getAllPartnerships() {
        List<Partnership> partnerships = new ArrayList<>();
        String sql = "SELECT * FROM partnerships";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                partnerships.add(Partnership.builder()
                    .id(rs.getInt("id"))
                    .player1(UUID.fromString(rs.getString("player1_uuid")))
                    .player2(UUID.fromString(rs.getString("player2_uuid")))
                    .startedAt(rs.getLong("started_at"))
                    .lastRewardCheck(rs.getLong("last_reward_check"))
                    .level(rs.getInt("level"))
                    .xp(rs.getInt("xp"))
                    .pvpEnabled(rs.getInt("pvp_enabled") == 1)
                    .build());
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting all partnerships: " + e.getMessage());
        }
        
        return partnerships;
    }
    
    public void updateLastRewardCheck(int partnershipId, long timestamp) {
        String sql = "UPDATE partnerships SET last_reward_check = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, timestamp);
            stmt.setInt(2, partnershipId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating last reward check: " + e.getMessage());
        }
    }
    
    public void updatePartnershipXpAndLevel(int partnershipId, int xp, int level) {
        String sql = "UPDATE partnerships SET xp = ?, level = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, xp);
            stmt.setInt(2, level);
            stmt.setInt(3, partnershipId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating partnership xp/level: " + e.getMessage());
        }
    }
    
    public void updateLastQuestComplete(int partnershipId, long timestamp) {
        String sql = "UPDATE partnerships SET last_quest_complete = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, timestamp);
            stmt.setInt(2, partnershipId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating last_quest_complete: " + e.getMessage());
        }
    }
    
    public void updatePvpEnabled(int partnershipId, boolean enabled) {
        String sql = "UPDATE partnerships SET pvp_enabled = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enabled ? 1 : 0);
            stmt.setInt(2, partnershipId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating pvp_enabled: " + e.getMessage());
        }
    }
    
    public ActiveQuest getActiveQuest(int partnershipId) {
        String sql = "SELECT * FROM active_quests WHERE partnership_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, partnershipId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                QuestType questType;
                try {
                    questType = QuestType.valueOf(rs.getString("quest_type"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid quest_type in database: " + rs.getString("quest_type") + " for partnership " + partnershipId);
                    return null;
                }
                
                return ActiveQuest.builder()
                    .id(rs.getInt("id"))
                    .partnershipId(rs.getInt("partnership_id"))
                    .questType(questType)
                    .target(rs.getString("target"))
                    .requiredAmount(rs.getInt("required_amount"))
                    .progress(rs.getInt("progress"))
                    .createdAt(rs.getLong("created_at"))
                    .build();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting active quest: " + e.getMessage());
        }
        
        return null;
    }
    
    public void saveActiveQuest(ActiveQuest quest) {
        String sql = "INSERT INTO active_quests (partnership_id, quest_type, target, required_amount, progress, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, quest.getPartnershipId());
            stmt.setString(2, quest.getQuestType().name());
            stmt.setString(3, quest.getTarget());
            stmt.setInt(4, quest.getRequiredAmount());
            stmt.setInt(5, quest.getProgress());
            stmt.setLong(6, quest.getCreatedAt());
            stmt.executeUpdate();
            
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                quest.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving active quest: " + e.getMessage());
        }
    }
    
    public void updateQuestProgress(int questId, int progress) {
        String sql = "UPDATE active_quests SET progress = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, progress);
            stmt.setInt(2, questId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating quest progress: " + e.getMessage());
        }
    }
    
    public void deleteActiveQuest(int partnershipId) {
        String sql = "DELETE FROM active_quests WHERE partnership_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, partnershipId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error deleting active quest: " + e.getMessage());
        }
    }
    
    public List<Partnership> getTopPartnerships(int limit) {
        List<Partnership> partnerships = new ArrayList<>();
        String sql = "SELECT * FROM partnerships ORDER BY level DESC, xp DESC LIMIT ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                partnerships.add(Partnership.builder()
                    .id(rs.getInt("id"))
                    .player1(UUID.fromString(rs.getString("player1_uuid")))
                    .player2(UUID.fromString(rs.getString("player2_uuid")))
                    .startedAt(rs.getLong("started_at"))
                    .lastRewardCheck(rs.getLong("last_reward_check"))
                    .level(rs.getInt("level"))
                    .xp(rs.getInt("xp"))
                    .build());
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting top partnerships: " + e.getMessage());
        }
        
        return partnerships;
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
