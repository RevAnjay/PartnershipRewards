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

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class LevelsGUI implements InventoryHolder, Listener {
    
    private final PartnershipRewards plugin;
    private final Player viewer;
    private final Partnership partnership;
    private final Inventory inventory;
    
    private static final String GUI_TITLE = colorize("&6&lPartnership Level");
    
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
        inventory.setItem(26, createItem(Material.BARRIER, colorize("&c&lClose"), Arrays.asList(colorize("&7Click to close"))));
    }
    
    private ItemStack createPlayerHead(UUID uuid, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            meta.setDisplayName(colorize("&e&lPartner: &f" + name));
            
            long durationDays = partnership.getDurationInDays();
            String duration = formatDuration(partnership.getDurationInSeconds());
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String startDate = sdf.format(new Date(partnership.getStartedAt() * 1000));
            
            meta.setLore(Arrays.asList(
                "",
                colorize("&7Duration: &a" + duration),
                colorize("&7Since: &b" + startDate),
                colorize("&7Days: &e" + durationDays + " days")
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
        lore.add(colorize("&7Current XP: &b" + xp));
        lore.add(colorize("&7Required XP: &e" + requiredXp));
        lore.add("");
        
        if (level >= maxLevel) {
            lore.add(colorize("&a&lMAX LEVEL"));
        } else {
            int percentage = requiredXp > 0 ? (xp * 100) / requiredXp : 100;
            lore.add(colorize("&7Progress: &a" + percentage + "%"));
        }
        
        return createItem(material, colorize("&6&lLevel " + level), lore);
    }
    
    private void createXpProgressBar() {
        int xp = partnership.getXp();
        int requiredXp = plugin.getQuestManager().getRequiredXpForLevel(partnership.getLevel() + 1);
        int filledSlots = requiredXp > 0 ? Math.min(6, (xp * 6) / requiredXp) : 6;
        
        for (int i = 0; i < 6; i++) {
            Material pane = i < filledSlots ? Material.LIME_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;
            String name = i < filledSlots ? colorize("&a&l█") : colorize("&7&l▒");
            inventory.setItem(11 + i, createItem(pane, name, null));
        }
    }
    
    private ItemStack createQuestItem() {
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(viewer.getUniqueId());
        
        if (quest == null) {
            if (plugin.getQuestManager().isOnQuestCooldown(partnership)) {
                long remaining = plugin.getQuestManager().getQuestCooldownRemaining(partnership);
                return createItem(Material.CLOCK, colorize("&c&lCooldown Active"), Arrays.asList(
                    "",
                    colorize("&7New quest available in:"),
                    colorize("&e" + remaining + " minutes"),
                    "",
                    colorize("&8Complete quests to"),
                    colorize("&8earn XP!")
                ));
            }
            return createItem(Material.PAPER, colorize("&a&lQuest Available!"), Arrays.asList(
                "",
                colorize("&7You can get a new quest!"),
                colorize("&7Use &e/partner quest &7to"),
                colorize("&7generate a new quest.")
            ));
        }
        Material material = quest.getQuestType().isBonusQuest() ? Material.GOLDEN_APPLE : Material.PAPER;
        String prefix = quest.getQuestType().isBonusQuest() ? colorize("&6&lBONUS: ") : colorize("&e&l");
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
            colorize("&7" + quest.getFormattedDescription()),
            "",
            colorize("&7Progress: ") + quest.getProgressBar(),
            colorize("&7Done: &e" + progress + "&7/&e" + required + " &8(&a" + percentage + "%&8)"),
            "",
            colorize("&7Reward: &b+" + xpReward + " XP")
        ));
    }
    
    private ItemStack createQuestProgressItem() {
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(viewer.getUniqueId());
        
        if (quest == null) {
            return createItem(Material.GRAY_DYE, colorize("&7Progress"), Arrays.asList(colorize("&cNo quest")));
        }
        
        int progress = quest.getProgress();
        int required = quest.getRequiredAmount();
        int percentage = quest.getCompletionPercentage();
        
        Material material = percentage >= 100 ? Material.LIME_DYE : 
                           percentage >= 50 ? Material.YELLOW_DYE : Material.RED_DYE;
        
        return createItem(material, colorize("&a&lProgress: " + percentage + "%"), Arrays.asList(
            "",
            quest.getProgressBar(),
            "",
            colorize("&7Done: &e" + progress + "&7/&e" + required)
        ));
    }
    
    private ItemStack createTimeItem() {
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(viewer.getUniqueId());
        
        if (quest == null) {
            return createItem(Material.CLOCK, colorize("&7Time"), Arrays.asList(colorize("&cNo quest")));
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
            lore.add(colorize("&7Remaining: &e" + hoursRemaining + "h " + minutesRemaining + "m"));
            lore.add("");
            lore.add(colorize("&8Quest will reset if not"));
            lore.add(colorize("&8completed in time."));
        } else {
            lore.add(colorize("&cEXPIRED!"));
            lore.add(colorize("&7Quest will be auto-reset."));
        }
        
        String title = remaining > 0 ? colorize("&e&lTime Remaining") : colorize("&c&lExpired!");
        return createItem(Material.CLOCK, title, lore);
    }
    
    private ItemStack createNextRewardItem() {
        int nextLevel = partnership.getLevel() + 1;
        int maxLevel = plugin.getConfig().getInt("quest.max-level", 50);
        
        if (partnership.getLevel() >= maxLevel) {
            return createItem(Material.NETHER_STAR, colorize("&d&lMAX LEVEL"), Arrays.asList(
                "",
                colorize("&7You've reached the maximum level!"),
                colorize("&7Congratulations!")
            ));
        }
        
        ConfigurationSection rewardSection = plugin.getConfig().getConfigurationSection("level-rewards." + nextLevel);
        
        if (rewardSection == null) {
            return createItem(Material.GOLD_INGOT, colorize("&e&lLevel " + nextLevel), Arrays.asList(
                "",
                colorize("&7No special reward"),
                colorize("&7for this level.")
            ));
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(colorize("&7Rewards for &eLevel " + nextLevel + "&7:"));
        lore.add("");
        
        List<String> commands = rewardSection.getStringList("commands");
        for (String cmd : commands) {
            String readable = cmd.replace("give {player} ", "")
                                 .replace("give {partner} ", "")
                                 .replace("_", " ");
            lore.add(colorize("&8• &a" + readable));
        }
        
        return createItem(Material.CHEST, colorize("&6&lReward Level " + nextLevel), lore);
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
            return days + "d " + hours + "h";
        } else {
            return hours + "h";
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
