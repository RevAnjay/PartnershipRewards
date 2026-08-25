package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AchievementManager {

    private final PartnershipRewards plugin;

    @Getter
    public static class Achievement {
        private final String key;
        private final String title;
        private final String description;
        private final String category;
        private final int requiredProgress;
        private final String rewardTitle;

        public Achievement(String key, String title, String description, String category, int requiredProgress, String rewardTitle) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.category = category;
            this.requiredProgress = requiredProgress;
            this.rewardTitle = rewardTitle;
        }
    }

    private final Map<String, Achievement> registeredAchievements = new LinkedHashMap<>();
    private final Map<Integer, Set<String>> unlockedCache = new ConcurrentHashMap<>();

    public AchievementManager(PartnershipRewards plugin) {
        this.plugin = plugin;
        registerDefaultAchievements();
    }

    private void registerDefaultAchievements() {
        register(new Achievement("FIRST_QUEST", "First Steps", "Complete your first partnership quest", "First Time", 1, "Novice Pair"));
        register(new Achievement("QUEST_10", "Quest Seekers", "Complete 10 partnership quests", "Milestone", 10, "Adventurous"));
        register(new Achievement("QUEST_100", "Quest Master", "Complete 100 partnership quests", "Milestone", 100, "Quest Master"));
        register(new Achievement("FIRST_GIFT", "Thoughtful Heart", "Send a gift to your partner", "First Time", 1, "Gift Giver"));
        register(new Achievement("GIFT_100", "True Generosity", "Send 100 gifts to your partner", "Milestone", 100, "Santa"));
        register(new Achievement("STREAK_7", "Weekly Bond", "Maintain a 7-day login streak", "Milestone", 7, "Dedicated"));
        register(new Achievement("STREAK_30", "Love Bird", "Maintain a 30-day login streak", "Milestone", 30, "Love Bird"));
        register(new Achievement("LEVEL_10", "Growing Strong", "Reach partnership level 10", "Milestone", 10, "Sweethearts"));
        register(new Achievement("LEVEL_50", "Unbreakable Bond", "Reach partnership level 50", "Milestone", 50, "Eternal"));
        register(new Achievement("ENGAGED", "She Said Yes!", "Get engaged with a proposal ring", "Special Event", 1, "Engaged"));
        register(new Achievement("FIRST_PRESTIGE", "Transcendent Love", "Reach your first Prestige level", "Milestone", 1, "Transcended"));
    }

    public void register(Achievement achievement) {
        registeredAchievements.put(achievement.getKey(), achievement);
    }

    public Collection<Achievement> getAllAchievements() {
        return registeredAchievements.values();
    }

    public Achievement getAchievement(String key) {
        return registeredAchievements.get(key);
    }

    public void initialize() {
        createTable();
    }

    private void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS achievements (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                partnership_id INTEGER NOT NULL,
                achievement_key VARCHAR(64) NOT NULL,
                progress INTEGER DEFAULT 0,
                unlocked BOOLEAN DEFAULT FALSE,
                unlocked_at BIGINT DEFAULT 0,
                UNIQUE KEY unique_p_ach (partnership_id, achievement_key)
            )
        """;

        boolean isSqlite = plugin.getConfig().getString("database.type", "SQLITE").equalsIgnoreCase("SQLITE");
        if (isSqlite) {
            sql = sql.replace("AUTO_INCREMENT", "AUTOINCREMENT")
                     .replace("UNIQUE KEY unique_p_ach (partnership_id, achievement_key)", "UNIQUE (partnership_id, achievement_key)");
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to create achievements table: " + e.getMessage());
        }
    }

    public void checkAndProgress(Partnership partnership, String achievementKey, int progressDelta) {
        if (partnership == null) return;
        Achievement achievement = registeredAchievements.get(achievementKey);
        if (achievement == null) return;

        Set<String> unlocked = unlockedCache.computeIfAbsent(partnership.getId(), this::loadUnlockedFromDb);
        if (unlocked.contains(achievementKey)) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            int currentProgress = getProgress(partnership.getId(), achievementKey) + progressDelta;
            boolean isUnlocked = currentProgress >= achievement.getRequiredProgress();

            saveProgress(partnership.getId(), achievementKey, currentProgress, isUnlocked);

            if (isUnlocked) {
                unlocked.add(achievementKey);
                plugin.getServer().getScheduler().runTask(plugin, () -> broadcastUnlock(partnership, achievement));
            }
        });
    }

    private Set<String> loadUnlockedFromDb(int partnershipId) {
        Set<String> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
        String query = "SELECT achievement_key FROM achievements WHERE partnership_id = ? AND unlocked = TRUE";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, partnershipId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                set.add(rs.getString("achievement_key"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load achievements for partnership " + partnershipId + ": " + e.getMessage());
        }
        return set;
    }

    private int getProgress(int partnershipId, String key) {
        String query = "SELECT progress FROM achievements WHERE partnership_id = ? AND achievement_key = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, partnershipId);
            stmt.setString(2, key);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("progress");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error reading achievement progress: " + e.getMessage());
        }
        return 0;
    }

    private void saveProgress(int partnershipId, String key, int progress, boolean unlocked) {
        String sql = """
            INSERT INTO achievements (partnership_id, achievement_key, progress, unlocked, unlocked_at)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE progress = VALUES(progress), unlocked = VALUES(unlocked), unlocked_at = VALUES(unlocked_at)
        """;

        boolean isSqlite = plugin.getConfig().getString("database.type", "SQLITE").equalsIgnoreCase("SQLITE");
        if (isSqlite) {
            sql = """
                INSERT INTO achievements (partnership_id, achievement_key, progress, unlocked, unlocked_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(partnership_id, achievement_key) DO UPDATE SET progress=excluded.progress, unlocked=excluded.unlocked, unlocked_at=excluded.unlocked_at
            """;
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, partnershipId);
            stmt.setString(2, key);
            stmt.setInt(3, progress);
            stmt.setBoolean(4, unlocked);
            stmt.setLong(5, unlocked ? System.currentTimeMillis() : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving achievement progress: " + e.getMessage());
        }
    }

    private void broadcastUnlock(Partnership partnership, Achievement achievement) {
        Player p1 = Bukkit.getPlayer(partnership.getPlayer1());
        Player p2 = Bukkit.getPlayer(partnership.getPlayer2());

        String msg = "&6&l[ACHIEVEMENT UNLOCKED]&r &e" + achievement.getTitle() + " &7- " + achievement.getDescription();
        if (p1 != null && p1.isOnline()) {
            p1.sendMessage(PartnershipRewards.colorize(msg));
            PartnershipRewards.playLevelUpSound(p1);
            p1.playSound(p1.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        if (p2 != null && p2.isOnline()) {
            p2.sendMessage(PartnershipRewards.colorize(msg));
            PartnershipRewards.playLevelUpSound(p2);
            p2.playSound(p2.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    public boolean isUnlocked(int partnershipId, String key) {
        Set<String> unlocked = unlockedCache.computeIfAbsent(partnershipId, this::loadUnlockedFromDb);
        return unlocked.contains(key);
    }
}
