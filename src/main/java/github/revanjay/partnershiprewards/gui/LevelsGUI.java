package github.revanjay.partnershiprewards.gui;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.ActiveQuest;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class LevelsGUI implements InventoryHolder, Listener {
    
    private final PartnershipRewards plugin;
    private final Player viewer;
    private final Partnership partnership;
    private final Inventory inventory;
    
    private static final String GUI_TITLE = "Â§6Â§lâš” Â§ePartnership Level";
    
    public LevelsGUI(PartnershipRewards plugin, Player viewer, Partnership partnership) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.partnership = partnership;
        this.inventory = Bukkit.createInventory(this, 27, GUI_TITLE);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        setupItems();
    }
    
    private void setupItems() {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }
        UUID partnerUuid = partnership.getPartner(viewer.getUniqueId());
        String partnerName = Bukkit.getOfflinePlayer(partnerUuid).getName();
        ItemStack partnerHead = createPlayerHead(partnerUuid, partnerName);
        inventory.setItem(4, partnerHead);
        inventory.setItem(10, createLevelItem());
        createXpProgressBar();
        inventory.setItem(19, createQuestItem());
        inventory.setItem(20, createQuestProgressItem());
        inventory.setItem(21, createTimeItem());
        inventory.setItem(22, createNextRewardItem());
        inventory.setItem(26, createItem(Material.BARRIER, "Â§cÂ§lTutup", Arrays.asList("Â§7Klik untuk menutup")));
    }
    
    private ItemStack createPlayerHead(UUID uuid, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            meta.setDisplayName("Â§eÂ§lPartner: Â§f" + name);
            
            long durationDays = partnership.getDurationInDays();
            String duration = formatDuration(partnership.getDurationInSeconds());
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String startDate = sdf.format(new Date(partnership.getStartedAt() * 1000));
            
            meta.setLore(Arrays.asList(
                "",
                "Â§7Durasi: Â§a" + duration,
                "Â§7Sejak: Â§b" + startDate,
                "Â§7Hari: Â§e" + durationDays + " hari"
            ));
            head.setItemMeta(meta);
        }
        return head;
    }
    
    private ItemStack createLevelItem() {
        int level = partnership.getLevel();
        int xp = partnership.getXp();
        int requiredXp = plugin.getQuestManager().getRequiredXpForLevel(level + 1);
        int maxLevel = plugin.getConfig().getInt("quest.max-level", 50);
        
        Material material = level >= 25 ? Material.NETHER_STAR : 
                           level >= 10 ? Material.DIAMOND : 
                           level >= 5 ? Material.GOLD_INGOT : Material.IRON_INGOT;
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("Â§7XP Saat Ini: Â§b" + xp);
        lore.add("Â§7XP Dibutuhkan: Â§e" + requiredXp);
        lore.add("");
        
        if (level >= maxLevel) {
            lore.add("Â§aÂ§lâœ“ MAX LEVEL!");
        } else {
            int percentage = requiredXp > 0 ? (xp * 100) / requiredXp : 100;
            lore.add("Â§7Progress: Â§a" + percentage + "%");
        }
        
        return createItem(material, "Â§6Â§lLevel " + level, lore);
    }
    
    private void createXpProgressBar() {
        int xp = partnership.getXp();
        int requiredXp = plugin.getQuestManager().getRequiredXpForLevel(partnership.getLevel() + 1);
        int filledSlots = requiredXp > 0 ? Math.min(6, (xp * 6) / requiredXp) : 6;
        
        for (int i = 0; i < 6; i++) {
            Material pane = i < filledSlots ? Material.LIME_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;
            String name = i < filledSlots ? "Â§aÂ§lâ–ˆ" : "Â§7Â§lâ–‘";
            inventory.setItem(11 + i, createItem(pane, name, null));
        }
    }
    
    private ItemStack createQuestItem() {
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(viewer.getUniqueId());
        
        if (quest == null) {
            if (plugin.getQuestManager().isOnQuestCooldown(partnership)) {
                long remaining = plugin.getQuestManager().getQuestCooldownRemaining(partnership);
                return createItem(Material.CLOCK, "Â§cÂ§lCooldown Aktif", Arrays.asList(
                    "",
                    "Â§7Quest baru tersedia dalam:",
                    "Â§e" + remaining + " menit",
                    "",
                    "Â§8Selesaikan quest untuk",
                    "Â§8mendapatkan XP!"
                ));
            }
            return createItem(Material.PAPER, "Â§aÂ§lQuest Tersedia!", Arrays.asList(
                "",
                "Â§7Kamu bisa ambil quest baru!",
                "Â§7Gunakan Â§e/partner quest Â§7untuk",
                "Â§7mendapatkan quest baru."
            ));
        }
        Material material = quest.getQuestType().isBonusQuest() ? Material.GOLDEN_APPLE : Material.PAPER;
        String prefix = quest.getQuestType().isBonusQuest() ? "Â§6Â§lâ˜… BONUS: " : "Â§eÂ§l";
        ConfigurationSection typeConfig = plugin.getConfig()
            .getConfigurationSection("quest.types." + quest.getQuestType().name());
        int xpReward = typeConfig != null && typeConfig.contains("xp-reward") 
            ? typeConfig.getInt("xp-reward") 
            : plugin.getConfig().getInt("quest.xp-per-quest", 100);
        int progress = quest.getProgress();
        int required = quest.getRequiredAmount();
        int percentage = quest.getCompletionPercentage();
        
        return createItem(material, prefix + quest.getQuestType().getDisplayName(), Arrays.asList(
            "",
            "Â§7" + quest.getFormattedDescription(),
            "",
            "Â§7Progress: " + quest.getProgressBar(),
            "Â§7Selesai: Â§e" + progress + "Â§7/Â§e" + required + " Â§8(Â§a" + percentage + "%Â§8)",
            "",
            "Â§7Reward: Â§b+" + xpReward + " XP"
        ));
    }
    
    private ItemStack createQuestProgressItem() {
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(viewer.getUniqueId());
        
        if (quest == null) {
            return createItem(Material.GRAY_DYE, "Â§7Progress", Arrays.asList("Â§cTidak ada quest"));
        }
        
        int progress = quest.getProgress();
        int required = quest.getRequiredAmount();
        int percentage = quest.getCompletionPercentage();
        
        Material material = percentage >= 100 ? Material.LIME_DYE : 
                           percentage >= 50 ? Material.YELLOW_DYE : Material.RED_DYE;
        
        return createItem(material, "Â§aÂ§lProgress: " + percentage + "%", Arrays.asList(
            "",
            quest.getProgressBar(),
            "",
            "Â§7Selesai: Â§e" + progress + "Â§7/Â§e" + required
        ));
    }
    
    private ItemStack createTimeItem() {
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(viewer.getUniqueId());
        
        if (quest == null) {
            return createItem(Material.CLOCK, "Â§7Waktu", Arrays.asList("Â§cTidak ada quest"));
        }
        
        long resetHours = plugin.getConfig().getLong("quest.reset-hours", 24);
        long resetSeconds = resetHours * 3600;
        long now = Instant.now().getEpochSecond();
        long elapsed = now - quest.getCreatedAt();
        long remaining = resetSeconds - elapsed;
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        if (remaining > 0) {
            long hoursRemaining = remaining / 3600;
            long minutesRemaining = (remaining % 3600) / 60;
            lore.add("Â§7Sisa: Â§e" + hoursRemaining + " jam " + minutesRemaining + " menit");
            lore.add("");
            lore.add("Â§8Quest akan reset jika tidak");
            lore.add("Â§8diselesaikan tepat waktu.");
        } else {
            lore.add("Â§câš  KADALUARSA!");
            lore.add("Â§7Quest akan di-reset otomatis.");
        }
        
        String title = remaining > 0 ? "Â§eÂ§lWaktu Tersisa" : "Â§cÂ§lâš  Kadaluarsa!";
        return createItem(Material.CLOCK, title, lore);
    }
    
    private ItemStack createNextRewardItem() {
        int nextLevel = partnership.getLevel() + 1;
        int maxLevel = plugin.getConfig().getInt("quest.max-level", 50);
        
        if (partnership.getLevel() >= maxLevel) {
            return createItem(Material.NETHER_STAR, "Â§dÂ§lâœ“ MAX LEVEL", Arrays.asList(
                "",
                "Â§7Kamu sudah mencapai level maksimum!",
                "Â§7Selamat! ðŸŽ‰"
            ));
        }
        
        ConfigurationSection rewardSection = plugin.getConfig().getConfigurationSection("level-rewards." + nextLevel);
        
        if (rewardSection == null) {
            return createItem(Material.GOLD_INGOT, "Â§eÂ§lLevel " + nextLevel, Arrays.asList(
                "",
                "Â§7Tidak ada reward khusus",
                "Â§7untuk level ini."
            ));
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("Â§7Rewards untuk Â§eLevel " + nextLevel + "Â§7:");
        lore.add("");
        
        List<String> commands = rewardSection.getStringList("commands");
        for (String cmd : commands) {
            String readable = cmd.replace("give {player} ", "")
                                 .replace("give {partner} ", "")
                                 .replace("_", " ");
            lore.add("Â§8â€¢ Â§a" + readable);
        }
        
        return createItem(Material.CHEST, "Â§6Â§lReward Level " + nextLevel, lore);
    }
    
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private String formatDuration(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        
        if (days > 0) {
            return days + " hari " + hours + " jam";
        } else {
            return hours + " jam";
        }
    }
    
    public void open() {
        viewer.openInventory(inventory);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        
        event.setCancelled(true);
        if (event.getRawSlot() == 26) {
            event.getWhoClicked().closeInventory();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) return;
        HandlerList.unregisterAll(this);
    }
}

