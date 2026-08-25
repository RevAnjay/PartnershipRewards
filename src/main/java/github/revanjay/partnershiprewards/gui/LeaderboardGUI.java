package github.revanjay.partnershiprewards.gui;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import lombok.Getter;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardGUI implements InventoryHolder, Listener {

    public enum Tab {
        LEVEL("Top Partnerships (Level)", Material.NETHER_STAR),
        STREAK("Longest Streak", Material.BLAZE_POWDER),
        PRESTIGE("Prestige Leaders", Material.DRAGON_EGG);

        @Getter private final String title;
        @Getter private final Material icon;

        Tab(String title, Material icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    public static class LeaderboardEntry {
        public final String player1Name;
        public final String player2Name;
        public final int score;
        public final int rank;

        public LeaderboardEntry(int rank, String p1, String p2, int score) {
            this.rank = rank;
            this.player1Name = p1;
            this.player2Name = p2;
            this.score = score;
        }
    }

    private static final int[] SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private final PartnershipRewards plugin;
    private final Player viewer;
    private final Inventory inventory;
    private Tab currentTab = Tab.LEVEL;
    private GUIPaginator<LeaderboardEntry> paginator;

    public LeaderboardGUI(PartnershipRewards plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 54, PartnershipRewards.colorizeComponent("&6&lPartnership Leaderboard"));
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadDataAndRender();
    }
    private void loadDataAndRender() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> entries = fetchEntries(currentTab);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                this.paginator = new GUIPaginator<>(entries, 28);
                render();
            });
        });
    }

    private List<LeaderboardEntry> fetchEntries(Tab tab) {
        List<LeaderboardEntry> list = new ArrayList<>();
        String orderBy = switch (tab) {
            case LEVEL -> "level DESC, xp DESC";
            case STREAK -> "login_streak DESC";
            case PRESTIGE -> "prestige_level DESC, level DESC";
        };

        String sql = "SELECT player1_uuid, player2_uuid, level, login_streak, prestige_level FROM partnerships ORDER BY " + orderBy + " LIMIT 50";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int rank = 1;
            while (rs.next()) {
                String p1Uuid = rs.getString("player1_uuid");
                String p2Uuid = rs.getString("player2_uuid");
                String p1Name = Bukkit.getOfflinePlayer(java.util.UUID.fromString(p1Uuid)).getName();
                String p2Name = Bukkit.getOfflinePlayer(java.util.UUID.fromString(p2Uuid)).getName();
                if (p1Name == null) p1Name = "Unknown";
                if (p2Name == null) p2Name = "Unknown";

                int score = switch (tab) {
                    case LEVEL -> rs.getInt("level");
                    case STREAK -> rs.getInt("login_streak");
                    case PRESTIGE -> rs.getInt("prestige_level");
                };

                list.add(new LeaderboardEntry(rank++, p1Name, p2Name, score));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error querying leaderboard: " + e.getMessage());
        }
        return list;
    }

    private void render() {
        inventory.clear();
        EnhancedGUI.fillBorder(inventory, Material.BLACK_STAINED_GLASS_PANE);

        inventory.setItem(2, EnhancedGUI.createGradientItem(Tab.LEVEL.getIcon(), (currentTab == Tab.LEVEL ? "&a&l" : "&7") + Tab.LEVEL.getTitle(), List.of("&eClick to view Level leaderboard")));
        inventory.setItem(4, EnhancedGUI.createGradientItem(Tab.STREAK.getIcon(), (currentTab == Tab.STREAK ? "&a&l" : "&7") + Tab.STREAK.getTitle(), List.of("&eClick to view Streak leaderboard")));
        inventory.setItem(6, EnhancedGUI.createGradientItem(Tab.PRESTIGE.getIcon(), (currentTab == Tab.PRESTIGE ? "&a&l" : "&7") + Tab.PRESTIGE.getTitle(), List.of("&eClick to view Prestige leaderboard")));

        if (paginator != null) {
            List<LeaderboardEntry> pageItems = paginator.getCurrentPageItems();
            for (int i = 0; i < pageItems.size() && i < SLOTS.length; i++) {
                LeaderboardEntry entry = pageItems.get(i);
                Material icon = entry.rank == 1 ? Material.GOLD_BLOCK : (entry.rank == 2 ? Material.IRON_BLOCK : (entry.rank == 3 ? Material.COPPER_BLOCK : Material.PAPER));
                String name = "&e#" + entry.rank + " &6" + entry.player1Name + " &7& " + "&6" + entry.player2Name;
                List<String> lore = List.of(
                    "&7Score / Value: &a" + entry.score,
                    "&8Tab: " + currentTab.getTitle()
                );
                inventory.setItem(SLOTS[i], EnhancedGUI.createGradientItem(icon, name, lore));
            }
            paginator.applyNavigationButtons(inventory, 48, 50, 49);
        }

        inventory.setItem(45, EnhancedGUI.createGradientItem(Material.BARRIER, "&cClose", List.of("&7Click to close leaderboard")));
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
        int slot = event.getRawSlot();

        if (slot == 45) {
            viewer.closeInventory();
            return;
        }

        if (slot == 2 && currentTab != Tab.LEVEL) {
            currentTab = Tab.LEVEL;
            loadDataAndRender();
            return;
        }
        if (slot == 4 && currentTab != Tab.STREAK) {
            currentTab = Tab.STREAK;
            loadDataAndRender();
            return;
        }
        if (slot == 6 && currentTab != Tab.PRESTIGE) {
            currentTab = Tab.PRESTIGE;
            loadDataAndRender();
            return;
        }

        if (paginator != null) {
            if (slot == 48 && paginator.hasPreviousPage()) {
                paginator.previousPage();
                render();
            } else if (slot == 50 && paginator.hasNextPage()) {
                paginator.nextPage();
                render();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            HandlerList.unregisterAll(this);
        }
    }
}
