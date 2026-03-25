package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.List;
import java.util.Set;

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
    
    private void checkAndGiveRewards() {
        if (!plugin.getConfig().getBoolean("rewards.enabled", true)) {
            return;
        }
        
        List<Partnership> partnerships = plugin.getPartnershipManager().getAllPartnerships();
        
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
                giveReward(partnership, key);
                plugin.getDatabaseManager().updateLastRewardCheck(partnership.getId(), Instant.now().getEpochSecond());
            }
        }
    }
    
    private void giveReward(Partnership partnership, String milestoneKey) {
        String player1Name = Bukkit.getOfflinePlayer(partnership.getPlayer1()).getName();
        String player2Name = Bukkit.getOfflinePlayer(partnership.getPlayer2()).getName();
        
        List<String> commands = plugin.getConfig().getStringList("rewards.milestones." + milestoneKey + ".commands");
        String broadcast = plugin.getConfig().getString("rewards.milestones." + milestoneKey + ".broadcast");
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String command : commands) {
                String processedCmd = command
                    .replace("{player}", player1Name)
                    .replace("{partner}", player2Name);
                
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd);
            }
            
            if (broadcast != null && !broadcast.isEmpty()) {
                String message = broadcast
                    .replace("{player}", player1Name)
                    .replace("{partner}", player2Name)
                    .replace("&", "Â§");
                
                Bukkit.broadcastMessage(message);
            }
        });
    }
}
