package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;
import static github.revanjay.partnershiprewards.PartnershipRewards.colorizeComponent;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RewardManager {
    
    private final PartnershipRewards plugin;
    private SchedulerUtil.TaskHandle rewardTask;
    
    private final List<MilestoneReward> cachedMilestones = new java.util.ArrayList<>();
    private boolean rewardsEnabled = true;
    private int minDays = Integer.MAX_VALUE;
    
    public static class MilestoneReward {
        private final String key;
        private final int days;
        private final List<String> commands;
        private final String broadcast;

        public MilestoneReward(String key, int days, List<String> commands, String broadcast) {
            this.key = key;
            this.days = days;
            this.commands = commands;
            this.broadcast = broadcast;
        }

        public String getKey() { return key; }
        public int getDays() { return days; }
        public List<String> getCommands() { return commands; }
        public String getBroadcast() { return broadcast; }
    }
    
    public RewardManager(PartnershipRewards plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        cachedMilestones.clear();
        rewardsEnabled = plugin.getConfig().getBoolean("rewards.enabled", true);
        if (!rewardsEnabled) {
            minDays = Integer.MAX_VALUE;
            return;
        }

        ConfigurationSection milestonesSection = plugin.getConfig().getConfigurationSection("rewards.milestones");
        if (milestonesSection == null) {
            minDays = Integer.MAX_VALUE;
            return;
        }

        int currentMinDays = Integer.MAX_VALUE;
        for (String key : milestonesSection.getKeys(false)) {
            int days = milestonesSection.getInt(key + ".days", Integer.MAX_VALUE);
            List<String> commands = milestonesSection.getStringList(key + ".commands");
            String broadcast = milestonesSection.getString(key + ".broadcast");
            
            cachedMilestones.add(new MilestoneReward(key, days, commands, broadcast));
            if (days < currentMinDays) {
                currentMinDays = days;
            }
        }
        this.minDays = currentMinDays;
    }
    
    public void startRewardTask() {
        if (rewardTask != null) {
            rewardTask.cancel();
        }
        
        rewardTask = SchedulerUtil.runTaskTimerAsynchronously(plugin, () -> {
            checkAndGiveRewards();
        }, 20L * 60, 20L * 60);
    }
    
    public void shutdown() {
        if (rewardTask != null) {
            rewardTask.cancel();
        }
    }
    
    public void processPlayerJoin(UUID playerUuid) {
        if (!rewardsEnabled) {
            return;
        }
        
        SchedulerUtil.runTaskAsynchronously(plugin, () -> {
            Partnership partnership = plugin.getDatabaseManager().getPartnership(playerUuid);
            if (partnership == null) return;
            
            checkPartnershipRewards(partnership);
        });
    }
    
    private void checkAndGiveRewards() {
        if (!rewardsEnabled) {
            return;
        }
        
        if (minDays == Integer.MAX_VALUE) return;
        
        List<Partnership> partnerships = plugin.getDatabaseManager().getEligiblePartnershipsForReward(minDays);
        
        for (Partnership partnership : partnerships) {
            checkPartnershipRewards(partnership);
        }
    }
    
    private void checkPartnershipRewards(Partnership partnership) {
        long durationInDays = partnership.getDurationInDays();
        long lastCheckInDays = (partnership.getLastRewardCheck() - partnership.getStartedAt()) / 86400;
        
        for (MilestoneReward reward : cachedMilestones) {
            int requiredDays = reward.getDays();
            
            if (durationInDays >= requiredDays && lastCheckInDays < requiredDays) {
                Player p1 = Bukkit.getPlayer(partnership.getPlayer1());
                Player p2 = Bukkit.getPlayer(partnership.getPlayer2());
                
                if (p1 == null && p2 == null) {
                    continue;
                }
                
                giveReward(partnership, reward, p1, p2);
                plugin.getDatabaseManager().updateLastRewardCheck(partnership.getId(), Instant.now().getEpochSecond());
            }
        }
    }
    
    private void giveReward(Partnership partnership, MilestoneReward reward, Player onlineP1, Player onlineP2) {
        String player1Name = Bukkit.getOfflinePlayer(partnership.getPlayer1()).getName();
        String player2Name = Bukkit.getOfflinePlayer(partnership.getPlayer2()).getName();
        
        if (player1Name == null) player1Name = partnership.getPlayer1().toString();
        if (player2Name == null) player2Name = partnership.getPlayer2().toString();
        
        List<String> commands = reward.getCommands();
        String broadcast = reward.getBroadcast();
        
        final String p1Name = player1Name;
        final String p2Name = player2Name;
        
        SchedulerUtil.runTask(plugin, () -> {
            for (String command : commands) {
                String processedCmd = command
                    .replace("{player}", p1Name)
                    .replace("{partner}", p2Name);
                
                boolean isForPlayer1 = command.contains("{player}");
                boolean isForPlayer2 = command.contains("{partner}");
                
                if (isForPlayer1 && onlineP1 == null) continue;
                if (isForPlayer2 && onlineP2 == null) continue;
                
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd);
            }
            
            if (broadcast != null && !broadcast.isEmpty()) {
                String message = colorize(broadcast
                    .replace("{player}", p1Name)
                    .replace("{partner}", p2Name));
                
                Bukkit.broadcast(colorizeComponent(message));
            }
        });
    }
}
