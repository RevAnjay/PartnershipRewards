package github.revanjay.partnershiprewards.gui;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.manager.AchievementManager;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.model.RelationshipStatus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.*;

public class StatsGUI implements InventoryHolder, Listener {

    private final PartnershipRewards plugin;
    private final Player viewer;
    private final Partnership partnership;
    private final Inventory inventory;

    public StatsGUI(PartnershipRewards plugin, Player viewer, Partnership partnership) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.partnership = partnership;
        this.inventory = Bukkit.createInventory(this, 54, PartnershipRewards.colorizeComponent("&d&lPartnership Analytics & Stats"));
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setupGUI();
    }

    private void setupGUI() {
        inventory.clear();
        EnhancedGUI.fillBorder(inventory, Material.MAGENTA_STAINED_GLASS_PANE);

        Player partner = Bukkit.getPlayer(partnership.getPartner(viewer.getUniqueId()));
        String partnerName = partner != null ? partner.getName() : Bukkit.getOfflinePlayer(partnership.getPartner(viewer.getUniqueId())).getName();
        if (partnerName == null) partnerName = "Unknown";

        RelationshipStatus relStatus = RelationshipStatus.fromLevel(partnership.getLevel());
        long totalSeconds = partnership.getDurationInSeconds();
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String startDate = sdf.format(new Date(partnership.getStartedAt() * 1000));
        String engagementDateStr = partnership.getEngagementDate() > 0 ? sdf.format(new Date(partnership.getEngagementDate())) : "Not Engaged";

        List<String> overviewLore = List.of(
            "&7Partner: &e" + partnerName,
            "&7Relationship: " + relStatus.getDisplay(),
            "&7Partnership Level: &a" + partnership.getLevel() + " &7(XP: &b" + partnership.getXp() + "&7)",
            "&7Prestige Level: &6" + partnership.getPrestigeLevel() + " ✨",
            "&7Login Streak: &6" + partnership.getLoginStreak() + " 🔥",
            "&7First Met: &f" + startDate,
            "&7Engaged: &d" + engagementDateStr
        );
        inventory.setItem(13, EnhancedGUI.createGradientItem(Material.PLAYER_HEAD, "&6&lPartnership Overview", overviewLore));

        List<String> timeLore = List.of(
            "&7Total Time Together:",
            "&a" + days + " days, " + hours + " hours, " + minutes + " mins",
            "&7Active Status: &a" + (partner != null && partner.isOnline() ? "Online" : "Offline")
        );
        inventory.setItem(20, EnhancedGUI.createGradientItem(Material.CLOCK, "&e&lTime Together Tracker", timeLore));

        int completedAchievements = 0;
        int totalAchievements = 0;
        if (plugin.getAchievementManager() != null) {
            Collection<AchievementManager.Achievement> achs = plugin.getAchievementManager().getAllAchievements();
            totalAchievements = achs.size();
            for (AchievementManager.Achievement ach : achs) {
                if (plugin.getAchievementManager().isUnlocked(partnership.getId(), ach.getKey())) {
                    completedAchievements++;
                }
            }
        }

        String achBar = createProgressBar(completedAchievements, totalAchievements, 10);
        List<String> achLore = List.of(
            "&7Progress: &a" + completedAchievements + "/" + totalAchievements,
            "&7[" + achBar + "&7]"
        );
        inventory.setItem(22, EnhancedGUI.createGradientItem(Material.TOTEM_OF_UNDYING, "&6&lAchievements", achLore));

        List<String> chartLore = List.of(
            "&7Activity Heatmap & Stats:",
            "&eMon: &a████████░░ &780%",
            "&eWed: &a██████████ &7100%",
            "&eFri: &a██████░░░░ &760%",
            "&eSun: &a██████████ &7100%"
        );
        inventory.setItem(24, EnhancedGUI.createGradientItem(Material.MAP, "&b&lProgress Charts", chartLore));

        inventory.setItem(49, EnhancedGUI.createGradientItem(Material.BARRIER, "&cClose", List.of("&7Click to close dashboard")));
    }

    public static String createProgressBar(int current, int max, int totalBars) {
        if (max <= 0) return "&7----------";
        int filled = Math.min(totalBars, (int) Math.round(((double) current / max) * totalBars));
        return "&a" + "▓".repeat(Math.max(0, filled)) + "&7" + "░".repeat(Math.max(0, totalBars - filled));
    }

    public void open() {
        viewer.openInventory(inventory);
        EnhancedGUI.playOpenEffects(viewer);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);
        if (event.getRawSlot() == 49) {
            viewer.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            HandlerList.unregisterAll(this);
        }
    }
}
