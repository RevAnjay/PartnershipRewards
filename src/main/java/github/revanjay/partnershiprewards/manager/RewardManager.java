package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RewardManager {
    
    private final PartnershipRewards plugin;
    private BukkitTask rewardTask;
    
    public RewardManager(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    public void startRewardTask() {
        if (rewardTask != null) {
            rewardTask.cancel();
        }
        
        rewardTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            checkAndGiveRewards();
        }, 20L * 60, 20L * 60);
    }
    
    public void shutdown() {
        if (rewardTask != null) {
            rewardTask.cancel();
        }
    }
    
    public void processPlayerJoin(UUID playerUuid) {
        if (!plugin.getConfig().getBoolean("rewards.enabled", true)) {
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Partnership partnership = plugin.getDatabaseManager().getPartnership(playerUuid);
            if (partnership == null) return;
            
            checkPartnershipRewards(partnership);
        });
    }
    
    private void checkAndGiveRewards() {
        if (!plugin.getConfig().getBoolean("rewards.enabled", true)) {
            return;
        }
        
        ConfigurationSection milestonesSection = plugin.getConfig().getConfigurationSection("rewards.milestones");
        if (milestonesSection == null) return;
        
        int minDays = Integer.MAX_VALUE;
        for (String key : milestonesSection.getKeys(false)) {
            int days = plugin.getConfig().getInt("rewards.milestones." + key + ".days", Integer.MAX_VALUE);
            if (days < minDays) minDays = days;
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
        
        ConfigurationSection milestonesSection = plugin.getConfig().getConfigurationSection("rewards.milestones");
        if (milestonesSection == null) return;
        
        Set<String> milestoneKeys = milestonesSection.getKeys(false);
        
        for (String key : milestoneKeys) {
            int requiredDays = plugin.getConfig().getInt("rewards.milestones." + key + ".days");
            
            if (durationInDays >= requiredDays && lastCheckInDays < requiredDays) {
                Player p1 = Bukkit.getPlayer(partnership.getPlayer1());
                Player p2 = Bukkit.getPlayer(partnership.getPlayer2());
                
                if (p1 == null && p2 == null) {
                    continue;
                }
                
                giveReward(partnership, key, p1, p2);
                plugin.getDatabaseManager().updateLastRewardCheck(partnership.getId(), Instant.now().getEpochSecond());
            }
        }
    }
    
    private void giveReward(Partnership partnership, String milestoneKey, Player onlineP1, Player onlineP2) {
        String player1Name = Bukkit.getOfflinePlayer(partnership.getPlayer1()).getName();
        String player2Name = Bukkit.getOfflinePlayer(partnership.getPlayer2()).getName();
        
        if (player1Name == null) player1Name = partnership.getPlayer1().toString();
        if (player2Name == null) player2Name = partnership.getPlayer2().toString();
        
        List<String> commands = plugin.getConfig().getStringList("rewards.milestones." + milestoneKey + ".commands");
        String broadcast = plugin.getConfig().getString("rewards.milestones." + milestoneKey + ".broadcast");
        
        final String p1Name = player1Name;
        final String p2Name = player2Name;
        
        Bukkit.getScheduler().runTask(plugin, () -> {
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
                
                Bukkit.broadcastMessage(message);
            }
        });
    }
}

