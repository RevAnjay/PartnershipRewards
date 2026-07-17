package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;
import static github.revanjay.partnershiprewards.PartnershipRewards.sendActionBar;

import java.time.LocalDate;
import java.time.ZoneId;

public class StreakManager {
    
    private final PartnershipRewards plugin;
    
    public StreakManager(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    public void processLogin(Player player) {
        if (!plugin.getConfig().getBoolean("login-streak.enabled", true)) return;
        
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) return;
        
        long today = LocalDate.now(ZoneId.systemDefault()).toEpochDay();
        boolean isPlayer1 = partnership.isPlayer1(player.getUniqueId());
        
        long myLastLogin = isPlayer1 ? partnership.getPlayer1LastLogin() : partnership.getPlayer2LastLogin();
        
        
        if (myLastLogin == today) return;
        
        
        if (isPlayer1) {
            partnership.setPlayer1LastLogin(today);
        } else {
            partnership.setPlayer2LastLogin(today);
        }
        
        SchedulerUtil.runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().updatePlayerLogin(partnership.getId(), isPlayer1, today);
            
            
            long partnerLastLogin = isPlayer1 ? partnership.getPlayer2LastLogin() : partnership.getPlayer1LastLogin();
            
            if (partnerLastLogin == today) {
                processStreak(partnership, player, today);
            } else {
                SchedulerUtil.runTask(plugin, () -> {
                    String msg = plugin.getLanguageManager().getMessage("waiting-partner", true);
                    player.sendMessage(msg);
                    sendActionBar(player, plugin.getLanguageManager().getMessage("waiting-partner-action"));
                });
            }
        });
    }
    
    private void processStreak(Partnership partnership, Player player, long today) {
        long lastStreakDate = partnership.getLastStreakDate();
        long yesterday = today - 1;
        int maxStreak = plugin.getConfig().getInt("login-streak.max-streak", 7);
        
        int newStreak;
        if (lastStreakDate == today) {
            return;
        } else if (lastStreakDate == yesterday) {
            newStreak = Math.min(partnership.getLoginStreak() + 1, maxStreak);
        } else {
            newStreak = 1;
        }
        
        partnership.setLoginStreak(newStreak);
        partnership.setLastStreakDate(today);
        
        plugin.getDatabaseManager().updateLoginStreak(partnership.getId(), newStreak, today);
        
        int baseBonusXp = plugin.getConfig().getInt("login-streak.base-bonus-xp", 25);
        int bonusXp = baseBonusXp * newStreak;
        
        
        SchedulerUtil.runTask(plugin, () -> {
            plugin.getQuestManager().addBonusXp(partnership, bonusXp);
            
            String streakMsg = plugin.getLanguageManager().getMessage("streak-continue", true)
                .replace("{streak}", String.valueOf(newStreak))
                .replace("{xp}", String.valueOf(bonusXp));
            
            player.sendMessage(streakMsg);
            sendActionBar(player, plugin.getLanguageManager().getMessage("streak-action")
                .replace("{streak}", String.valueOf(newStreak))
                .replace("{xp}", String.valueOf(bonusXp)));
            
            Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
            if (partner != null) {
                partner.sendMessage(streakMsg);
                sendActionBar(partner, plugin.getLanguageManager().getMessage("streak-action")
                    .replace("{streak}", String.valueOf(newStreak))
                    .replace("{xp}", String.valueOf(bonusXp)));
            }
        });
    }
    

}
