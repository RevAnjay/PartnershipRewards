package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.ActiveQuest;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.model.QuestType;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class QuestManager {
    
    private final PartnershipRewards plugin;
    private final Map<Integer, ActiveQuest> questCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerPartnershipCache = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> pendingProgressUpdates = new ConcurrentHashMap<>();
    
    public QuestManager(PartnershipRewards plugin) {
        this.plugin = plugin;
        startBatchSaveTask();
        startDailyResetTask();
    }
    
        private void startBatchSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (pendingProgressUpdates.isEmpty()) return;
            
            for (Map.Entry<Integer, Integer> entry : pendingProgressUpdates.entrySet()) {
                Integer currentProgress = entry.getValue();
                plugin.getDatabaseManager().updateQuestProgress(entry.getKey(), currentProgress);
                pendingProgressUpdates.remove(entry.getKey(), currentProgress);
            }
        }, 20L * 30, 20L * 30);
    }
    
        private void startDailyResetTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long resetHours = plugin.getConfig().getLong("quest.reset-hours", 24);
            long resetSeconds = resetHours * 3600;
            long now = Instant.now().getEpochSecond();
            
            
            Iterator<Map.Entry<Integer, ActiveQuest>> it = questCache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, ActiveQuest> entry = it.next();
                ActiveQuest quest = entry.getValue();
                if (now - quest.getCreatedAt() >= resetSeconds) {
                    Partnership partnership = getPartnershipById(entry.getKey());
                    if (partnership != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            resetExpiredQuest(partnership, quest);
                        });
                    } else {
                        it.remove();
                    }
                }
            }
        }, 20L * 60 * 5, 20L * 60 * 5);
    }
    
        private boolean isQuestExpired(ActiveQuest quest) {
        long resetHours = plugin.getConfig().getLong("quest.reset-hours", 24);
        long resetSeconds = resetHours * 3600;
        long now = Instant.now().getEpochSecond();
        return (now - quest.getCreatedAt()) >= resetSeconds;
    }
    
        private void resetExpiredQuest(Partnership partnership, ActiveQuest oldQuest) {
        plugin.getLogger().info("Resetting expired quest for partnership #" + partnership.getId());
        Player p1 = Bukkit.getPlayer(partnership.getPlayer1());
        Player p2 = Bukkit.getPlayer(partnership.getPlayer2());
        
        String expiredMsg = colorize("&cQuest expired! A new quest is being generated...");
        
        if (p1 != null) p1.sendMessage(expiredMsg);
        if (p2 != null) p2.sendMessage(expiredMsg);
        ActiveQuest newQuest = generateRandomQuest(partnership);
        if (newQuest != null) {
            notifyNewQuest(partnership);
        }
    }
    
        public void loadPlayerQuest(UUID playerUuid) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null) return;
        
        playerPartnershipCache.put(playerUuid, partnership.getId());
        playerPartnershipCache.put(partnership.getPartner(playerUuid), partnership.getId());
        
        if (!questCache.containsKey(partnership.getId())) {
            ActiveQuest quest = plugin.getDatabaseManager().getActiveQuest(partnership.getId());
            if (quest != null) {
                if (isQuestExpired(quest)) {
                    resetExpiredQuest(partnership, quest);
                } else {
                    questCache.put(partnership.getId(), quest);
                }
            }
        } else {
            ActiveQuest cachedQuest = questCache.get(partnership.getId());
            if (cachedQuest != null && isQuestExpired(cachedQuest)) {
                resetExpiredQuest(partnership, cachedQuest);
            }
        }
    }
    
        public void loadPlayerQuestAsync(UUID playerUuid, Partnership partnership) {
        playerPartnershipCache.put(playerUuid, partnership.getId());
        playerPartnershipCache.put(partnership.getPartner(playerUuid), partnership.getId());
        
        if (!questCache.containsKey(partnership.getId())) {
            ActiveQuest quest = plugin.getDatabaseManager().getActiveQuest(partnership.getId());
            if (quest != null) {
                if (isQuestExpired(quest)) {
                    Bukkit.getScheduler().runTask(plugin, () -> resetExpiredQuest(partnership, quest));
                } else {
                    questCache.put(partnership.getId(), quest);
                }
            }
        } else {
            ActiveQuest cachedQuest = questCache.get(partnership.getId());
            if (cachedQuest != null && isQuestExpired(cachedQuest)) {
                Bukkit.getScheduler().runTask(plugin, () -> resetExpiredQuest(partnership, cachedQuest));
            }
        }
    }
    
        public void unloadPlayer(UUID playerUuid) {
        Integer partnershipId = playerPartnershipCache.remove(playerUuid);
        if (partnershipId != null) {
            Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
            if (partnership != null) {
                UUID partnerUuid = partnership.getPartner(playerUuid);
                if (partnerUuid != null && Bukkit.getPlayer(partnerUuid) == null) {
                    playerPartnershipCache.remove(partnerUuid);
                    questCache.remove(partnershipId);
                }
            }
        }
    }
    
        public boolean hasActiveQuest(UUID playerUuid, QuestType questType) {
        Integer partnershipId = playerPartnershipCache.get(playerUuid);
        if (partnershipId == null) return false;
        
        ActiveQuest quest = questCache.get(partnershipId);
        return quest != null && quest.getQuestType() == questType;
    }
    
        public ActiveQuest getActiveQuest(UUID playerUuid) {
        Integer partnershipId = playerPartnershipCache.get(playerUuid);
        if (partnershipId == null) return null;
        return questCache.get(partnershipId);
    }
    
        public ActiveQuest getActiveQuest(Partnership partnership) {
        return questCache.get(partnership.getId());
    }
    
        public ActiveQuest generateRandomQuest(Partnership partnership) {
        ActiveQuest existingQuest = getActiveQuest(partnership);
        if (existingQuest != null) {
            plugin.getLogger().info("Partnership #" + partnership.getId() + " already has an active quest, returning existing");
            return existingQuest;
        }
        if (isOnQuestCooldown(partnership)) {
            return null;
        }
        List<QuestType> enabledTypes = getEnabledQuestTypes();
        if (enabledTypes.isEmpty()) {
            plugin.getLogger().warning("No quest types enabled in config!");
            return null;
        }
        
        
        QuestType questType = enabledTypes.get(ThreadLocalRandom.current().nextInt(enabledTypes.size()));
        String target = null;
        int amount = 0;
        
        ConfigurationSection typeConfig = plugin.getConfig().getConfigurationSection("quest.types." + questType.name());
        if (typeConfig == null) {
            plugin.getLogger().warning("No config for quest type: " + questType.name());
            return null;
        }
        
        switch (questType) {
            case GIVE_ITEM, CRAFT_ITEM -> {
                List<String> items = typeConfig.getStringList("items");
                if (!items.isEmpty()) {
                    String[] parts = items.get(ThreadLocalRandom.current().nextInt(items.size())).split(":");
                    target = parts[0];
                    amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                }
            }
            case KILL_MOBS -> {
                List<String> mobs = typeConfig.getStringList("mobs");
                if (!mobs.isEmpty()) {
                    String[] parts = mobs.get(ThreadLocalRandom.current().nextInt(mobs.size())).split(":");
                    target = parts[0];
                    amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                }
            }
            case HARVEST_CROPS -> {
                List<String> crops = typeConfig.getStringList("crops");
                if (!crops.isEmpty()) {
                    String[] parts = crops.get(ThreadLocalRandom.current().nextInt(crops.size())).split(":");
                    target = parts[0];
                    amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 32;
                }
            }
            case USE_COMMAND -> {
                List<String> commands = typeConfig.getStringList("commands");
                if (!commands.isEmpty()) {
                    String[] parts = commands.get(ThreadLocalRandom.current().nextInt(commands.size())).split(":");
                    target = parts[0];
                    amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 5;
                }
            }
            case SEND_MESSAGE, EAT_FOOD, FISH_CATCH, TRADE_VILLAGER, ENCHANT_ITEM, ANVIL_USE, BREW_POTION,
                 PLACE_BLOCKS, TAME_ANIMAL, BREED_ANIMAL, SMELT_ITEMS, BREAK_BLOCKS, MINE_ANCIENT_DEBRIS,
                 SHOOT_ARROWS, SHEAR_SHEEP, USE_ENDER_PEARL, KILL_WITH_BOW, DAMAGE_EACH_OTHER, RIDE_TOGETHER,
                 THROW_SNOWBALL_AT_PARTNER, THROW_EGG, EAT_CAKE, DRINK_MILK, LAUNCH_FIREWORK,
                 EARN_XP_LEVELS, MINE_DIAMOND_ORE, MINE_DEEPSLATE_ORES, KILL_WITHER_SKELETONS, SMELT_NETHERITE -> {
                List<Integer> amounts = typeConfig.getIntegerList("amount");
                if (amounts.isEmpty()) amounts = typeConfig.getIntegerList("times");
                if (amounts.isEmpty()) amounts = typeConfig.getIntegerList("trades");
                if (!amounts.isEmpty()) {
                    amount = amounts.get(ThreadLocalRandom.current().nextInt(amounts.size()));
                } else {
                    amount = 10;
                }
            }
            case VISIT_NETHER -> {
                amount = 1;
            }
            case SLEEP_TOGETHER -> {
                List<Integer> times = typeConfig.getIntegerList("times");
                amount = times.isEmpty() ? 1 : times.get(ThreadLocalRandom.current().nextInt(times.size()));
            }
            case PLAY_TOGETHER -> {
                List<Integer> minutes = typeConfig.getIntegerList("minutes");
                amount = minutes.isEmpty() ? 30 : minutes.get(ThreadLocalRandom.current().nextInt(minutes.size()));
            }
            case KILL_BOSS -> {
                List<String> bosses = typeConfig.getStringList("bosses");
                if (!bosses.isEmpty()) {
                    String[] parts = bosses.get(ThreadLocalRandom.current().nextInt(bosses.size())).split(":");
                    target = parts[0];
                    amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                }
            }
            case COMPLETE_RAID -> {
                List<Integer> raids = typeConfig.getIntegerList("amount");
                amount = raids.isEmpty() ? 1 : raids.get(ThreadLocalRandom.current().nextInt(raids.size()));
            }
        }
        ActiveQuest quest = ActiveQuest.builder()
            .partnershipId(partnership.getId())
            .questType(questType)
            .target(target)
            .requiredAmount(amount)
            .progress(0)
            .createdAt(Instant.now().getEpochSecond())
            .build();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().saveActiveQuest(quest));
        questCache.put(partnership.getId(), quest);
        
        return quest;
    }
    
        private List<QuestType> getEnabledQuestTypes() {
        List<QuestType> enabled = new ArrayList<>();
        ConfigurationSection typesSection = plugin.getConfig().getConfigurationSection("quest.types");
        
        if (typesSection == null) return enabled;
        
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (String key : typesSection.getKeys(false)) {
            if (typesSection.getBoolean(key + ".enabled", true)) {
                try {
                    QuestType questType = QuestType.valueOf(key);
                    int chance = typesSection.getInt(key + ".chance", 100);
                    if (random.nextInt(100) < chance) {
                        enabled.add(questType);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown quest type in config: " + key);
                }
            }
        }
        
        return enabled;
    }
    
        public boolean updateQuestProgress(UUID playerUuid, QuestType questType, int amount) {
        Integer partnershipId = playerPartnershipCache.get(playerUuid);
        if (partnershipId == null) return false;
        
        ActiveQuest quest = questCache.get(partnershipId);
        if (quest == null || quest.getQuestType() != questType) return false;
        boolean completed = quest.addProgress(amount);
        pendingProgressUpdates.put(quest.getId(), quest.getProgress());
        
        if (completed) {
            completeQuest(partnershipId);
        }
        
        return completed;
    }
    
        private void completeQuest(int partnershipId) {
        ActiveQuest quest = questCache.get(partnershipId);
        if (quest == null) return;
        
        Partnership partnership = getPartnershipById(partnershipId);
        if (partnership == null) return;
        
        int xpReward = getXpRewardForQuest(quest.getQuestType());
        int newXp = partnership.getXp() + xpReward;
        int newLevel = partnership.getLevel();
        int requiredXp = getRequiredXpForLevel(newLevel + 1);
        while (newXp >= requiredXp && newLevel < plugin.getConfig().getInt("quest.max-level", 50)) {
            newXp -= requiredXp;
            newLevel++;
            giveLevelUpReward(partnership, newLevel);
            requiredXp = getRequiredXpForLevel(newLevel + 1);
        }
        partnership.setXp(newXp);
        partnership.setLevel(newLevel);
        final int finalXp = newXp;
        final int finalLevel = newLevel;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().updatePartnershipXpAndLevel(partnershipId, finalXp, finalLevel);
            plugin.getDatabaseManager().deleteActiveQuest(partnershipId);
        });
        questCache.remove(partnershipId);
        pendingProgressUpdates.remove(quest.getId());
        notifyQuestComplete(partnership, xpReward);
        long now = Instant.now().getEpochSecond();
        partnership.setLastQuestComplete(now);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
            plugin.getDatabaseManager().updateLastQuestComplete(partnershipId, now)
        );
        int cooldownMinutes = plugin.getConfig().getInt("quest.cooldown-minutes", 60);
        notifyCooldownStart(partnership, cooldownMinutes);
    }
    
        private Partnership getPartnershipById(int partnershipId) {
        for (Map.Entry<UUID, Integer> entry : playerPartnershipCache.entrySet()) {
            if (entry.getValue() == partnershipId) {
                return plugin.getPartnershipManager().getPartnership(entry.getKey());
            }
        }
        return null;
    }
    
        public int getRequiredXpForLevel(int level) {
        if (level <= 1) return 0;
        if (plugin.getConfig().contains("quest.xp-per-level." + level)) {
            return plugin.getConfig().getInt("quest.xp-per-level." + level);
        }
        
        
        int baseXp = plugin.getConfig().getInt("quest.base-xp-for-level", 200);
        double multiplier = plugin.getConfig().getDouble("quest.xp-multiplier-per-level", 1.2);
        
        return (int) (baseXp * Math.pow(multiplier, level - 2));
    }
    
        private int getXpRewardForQuest(QuestType questType) {
        int baseReward;
        
        ConfigurationSection typeConfig = plugin.getConfig()
            .getConfigurationSection("quest.types." + questType.name());
        
        if (typeConfig != null && typeConfig.contains("xp-reward")) {
            baseReward = typeConfig.getInt("xp-reward");
        } else {
            baseReward = plugin.getConfig().getInt("quest.xp-per-quest", 100);
        }
        
        
        return (int) (baseReward * PartnershipRewards.getGlobalXpMultiplier());
    }
    
        private void giveLevelUpReward(Partnership partnership, int newLevel) {
        ConfigurationSection rewardSection = plugin.getConfig().getConfigurationSection("level-rewards." + newLevel);
        if (rewardSection == null) return;
        
        List<String> commands = rewardSection.getStringList("commands");
        String broadcast = rewardSection.getString("broadcast");
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String player1Name = Bukkit.getOfflinePlayer(partnership.getPlayer1()).getName();
            String player2Name = Bukkit.getOfflinePlayer(partnership.getPlayer2()).getName();
            
            final String name1 = player1Name != null ? player1Name : "Unknown";
            final String name2 = player2Name != null ? player2Name : "Unknown";
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String command : commands) {
                    String processed = command
                        .replace("{player}", name1)
                        .replace("{partner}", name2);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
                }
                
                if (broadcast != null && !broadcast.isEmpty()) {
                    String message = colorize(broadcast
                        .replace("{player}", name1)
                        .replace("{partner}", name2));
                    Bukkit.broadcastMessage(message);
                }
            });
        });
    }
    
        private void notifyQuestComplete(Partnership partnership, int xpReward) {
        String message = colorize("&a&lQuest Complete! &7+" + xpReward + " XP");
        
        Player p1 = Bukkit.getPlayer(partnership.getPlayer1());
        Player p2 = Bukkit.getPlayer(partnership.getPlayer2());
        
        if (p1 != null) p1.sendMessage(message);
        if (p2 != null) p2.sendMessage(message);
    }
    
        private void notifyNewQuest(Partnership partnership) {
        ActiveQuest quest = questCache.get(partnership.getId());
        if (quest == null) return;
        
        String message = colorize("&e&lNew Quest! &7" + quest.getFormattedDescription());
        
        Player p1 = Bukkit.getPlayer(partnership.getPlayer1());
        Player p2 = Bukkit.getPlayer(partnership.getPlayer2());
        if (p1 != null) {
            p1.sendMessage(message);
            p1.sendTitle(colorize("&e&lNew Quest!"), colorize("&7" + quest.getQuestType().getDisplayName()), 10, 60, 20);
        }
        if (p2 != null) {
            p2.sendMessage(message);
            p2.sendTitle(colorize("&e&lNew Quest!"), colorize("&7" + quest.getQuestType().getDisplayName()), 10, 60, 20);
        }
    }
    
        public boolean isOnQuestCooldown(Partnership partnership) {
        long cooldownMinutes = plugin.getConfig().getLong("quest.cooldown-minutes", 60);
        long cooldownSeconds = cooldownMinutes * 60;
        long now = Instant.now().getEpochSecond();
        long elapsed = now - partnership.getLastQuestComplete();
        
        return elapsed < cooldownSeconds;
    }
    
        public long getQuestCooldownRemaining(Partnership partnership) {
        long cooldownMinutes = plugin.getConfig().getLong("quest.cooldown-minutes", 60);
        long cooldownSeconds = cooldownMinutes * 60;
        long now = Instant.now().getEpochSecond();
        long elapsed = now - partnership.getLastQuestComplete();
        long remaining = cooldownSeconds - elapsed;
        
        return remaining > 0 ? remaining / 60 : 0;
    }
    
        private void notifyCooldownStart(Partnership partnership, int cooldownMinutes) {
        String message = colorize("&7New quest in &e" + cooldownMinutes + " minutes&7. Use &e/partner level &7to check status.");
        
        Player p1 = Bukkit.getPlayer(partnership.getPlayer1());
        Player p2 = Bukkit.getPlayer(partnership.getPlayer2());
        
        if (p1 != null) p1.sendMessage(message);
        if (p2 != null) p2.sendMessage(message);
    }
    
        public void deleteActiveQuest(Partnership partnership) {
        questCache.remove(partnership.getId());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().deleteActiveQuest(partnership.getId()));
    }
    
        public void shutdown() {
        if (!pendingProgressUpdates.isEmpty()) {
            for (Map.Entry<Integer, Integer> entry : pendingProgressUpdates.entrySet()) {
                plugin.getDatabaseManager().updateQuestProgress(entry.getKey(), entry.getValue());
            }
            pendingProgressUpdates.clear();
        }
    }
    
    public void addBonusXp(Partnership partnership, int bonusXp) {
        int newXp = partnership.getXp() + bonusXp;
        int newLevel = partnership.getLevel();
        int requiredXp = getRequiredXpForLevel(newLevel + 1);
        while (newXp >= requiredXp && newLevel < plugin.getConfig().getInt("quest.max-level", 50)) {
            newXp -= requiredXp;
            newLevel++;
            giveLevelUpReward(partnership, newLevel);
            requiredXp = getRequiredXpForLevel(newLevel + 1);
        }
        partnership.setXp(newXp);
        partnership.setLevel(newLevel);
        final int finalXp = newXp;
        final int finalLevel = newLevel;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().updatePartnershipXpAndLevel(partnership.getId(), finalXp, finalLevel);
        });
    }
}

