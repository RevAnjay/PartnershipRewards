package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;
import static github.revanjay.partnershiprewards.PartnershipRewards.sendActionBar;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

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
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().updatePlayerLogin(partnership.getId(), isPlayer1, today);
            
            
            long partnerLastLogin = isPlayer1 ? partnership.getPlayer2LastLogin() : partnership.getPlayer1LastLogin();
            
            if (partnerLastLogin == today) {
                processStreak(partnership, player, today);
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String msg = plugin.getConfig().getString("login-streak.messages.waiting-partner",
                        "&7Waiting for your partner to login today to continue the streak...");
                    player.sendMessage(colorize(msg));
                    sendActionBar(player, "&7Waiting for partner...");
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
        
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getQuestManager().addBonusXp(partnership, bonusXp);
            
            String streakMsg = plugin.getConfig().getString("login-streak.messages.streak-continue",
                "&a&lStreak &7Day &e{streak}&7! Bonus &b+{xp} XP");
            streakMsg = streakMsg.replace("{streak}", String.valueOf(newStreak))
                                 .replace("{xp}", String.valueOf(bonusXp));
            
            String formattedMsg = colorize(streakMsg);
            
            player.sendMessage(formattedMsg);
            sendActionBar(player, "&a&lStreak &7Day &e" + newStreak + " &7| &b+" + bonusXp + " XP");
            
            Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
            if (partner != null) {
                partner.sendMessage(formattedMsg);
                sendActionBar(partner, "&a&lStreak &7Day &e" + newStreak + " &7| &b+" + bonusXp + " XP");
            }
        });
    }
    
    private String getMsg(String key) {
        return colorize(plugin.getConfig().getString("messages." + key, ""));
    }
}
