package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PrestigeManager {

    private final PartnershipRewards plugin;

    public PrestigeManager(PartnershipRewards plugin) {
        this.plugin = plugin;
    }

    public boolean canPrestige(Partnership partnership) {
        if (partnership == null) return false;
        int minLevel = plugin.getConfig().getInt("prestige.min-partnership-level", 50);
        int minDays = plugin.getConfig().getInt("prestige.min-days-active", 30);
        return partnership.getLevel() >= minLevel && partnership.getDurationInDays() >= minDays;
    }

    public boolean performPrestige(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("prestige-not-in-partnership", true));
            return false;
        }

        int minLevel = plugin.getConfig().getInt("prestige.min-partnership-level", 50);
        if (!canPrestige(partnership)) {
            int minDays = plugin.getConfig().getInt("prestige.min-days-active", 30);
            player.sendMessage(plugin.getLanguageManager().getMessage("prestige-requirements", true)
                    .replace("{level}", String.valueOf(minLevel))
                    .replace("{days}", String.valueOf(minDays)));
            return false;
        }

        int newPrestige = partnership.getPrestigeLevel() + 1;
        partnership.setPrestigeLevel(newPrestige);
        partnership.setTotalPrestigePoints(partnership.getTotalPrestigePoints() + 1);
        partnership.setLevel(1);
        partnership.setXp(0);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().updatePrestige(partnership.getId(), newPrestige, partnership.getTotalPrestigePoints());
            plugin.getDatabaseManager().updatePartnershipXpAndLevel(partnership.getId(), 0, 1);
        });

        Player p1 = Bukkit.getPlayer(partnership.getPlayer1());
        Player p2 = Bukkit.getPlayer(partnership.getPlayer2());

        String badge = getPrestigeBadge(newPrestige);
        String broadcast = plugin.getLanguageManager().getMessage("prestige-broadcast")
                .replace("{player1}", p1 != null ? p1.getName() : "Partner 1")
                .replace("{player2}", p2 != null ? p2.getName() : "Partner 2")
                .replace("{level}", String.valueOf(newPrestige))
                .replace("{badge}", badge);

        Bukkit.broadcast(PartnershipRewards.colorizeComponent(broadcast));
        if (p1 != null && p1.isOnline()) {
            PartnershipRewards.playLevelUpSound(p1);
            p1.playSound(p1.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        if (p2 != null && p2.isOnline()) {
            PartnershipRewards.playLevelUpSound(p2);
            p2.playSound(p2.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().checkAndProgress(partnership, "FIRST_PRESTIGE", 1);
        }

        return true;
    }

    public String getPrestigeBadge(int prestige) {
        if (prestige <= 0) return "";
        if (prestige <= 2) return "✨";
        if (prestige <= 5) return "🌟";
        if (prestige <= 10) return "💫";
        return "👑";
    }

    public double getXpMultiplier(int prestige) {
        double bonusPerPrestige = plugin.getConfig().getDouble("prestige.xp-bonus-per-prestige", 5.0) / 100.0;
        return 1.0 + (prestige * bonusPerPrestige);
    }
}
