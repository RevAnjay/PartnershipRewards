package github.revanjay.partnershiprewards.task;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.ActiveQuest;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.model.QuestType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayTogetherTask extends BukkitRunnable {
    
    private final PartnershipRewards plugin;
    
    public PlayTogetherTask(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        try {
            // Iterate online players instead of querying DB
            for (Player player : Bukkit.getOnlinePlayers()) {
                processPlayer(player);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in PlayTogetherTask: " + e.getMessage());
        }
    }
    
    private void processPlayer(Player player) {
        try {
            if (!plugin.getQuestManager().hasActiveQuest(player.getUniqueId(), QuestType.PLAY_TOGETHER)) {
                return;
            }
            
            ActiveQuest quest = plugin.getQuestManager().getActiveQuest(player.getUniqueId());
            if (quest == null) return;
            Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
            if (partnership == null) return;
            
            Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
            if (partner == null) return; // Partner offline
            if (player.getUniqueId().compareTo(partner.getUniqueId()) > 0) return;
            boolean completed = plugin.getQuestManager().updateQuestProgress(
                player.getUniqueId(), 
                QuestType.PLAY_TOGETHER, 
                1 // 1 minute per tick
            );
            if (!completed && quest.getProgress() % 5 == 0 && quest.getProgress() > 0) {
                String progressMsg = plugin.getConfig().getString("messages.prefix", "Â§d[Partnership] ")
                    .replace("&", "Â§") + 
                    "Â§7Main Bersama: Â§e" + quest.getProgress() + "/" + quest.getRequiredAmount() + " menit";
                
                player.sendMessage(progressMsg);
                partner.sendMessage(progressMsg);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing player " + player.getName() + ": " + e.getMessage());
        }
    }
    
        public void start() {
        this.runTaskTimer(plugin, 1200L, 1200L);
    }
}


