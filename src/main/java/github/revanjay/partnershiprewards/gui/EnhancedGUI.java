package github.revanjay.partnershiprewards.gui;

import github.revanjay.partnershiprewards.PartnershipRewards;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;
import static github.revanjay.partnershiprewards.PartnershipRewards.colorizeComponent;

public class EnhancedGUI {

    public static Inventory createAnimatedGUI(Player player, String title, int size) {
        Inventory inv = Bukkit.createInventory(null, size, colorizeComponent(title));
        playOpenEffects(player);
        return inv;
    }

    public static void playOpenEffects(Player player) {
        if (player == null || !player.isOnline()) return;
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.2, 0), 3, 0.3, 0.3, 0.3, 0.05);
    }

    public static ItemStack createGradientItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(colorizeComponent(name));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream().map(PartnershipRewards::colorizeComponent).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(colorizeComponent(name == null ? " " : name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void fillBorder(Inventory inv, Material material) {
        ItemStack glass = createGlassPane(material, " ");
        int size = inv.getSize();
        int rows = size / 9;
        for (int col = 0; col < 9; col++) {
            inv.setItem(col, glass);
            inv.setItem(size - 9 + col, glass);
        }
        for (int row = 1; row < rows - 1; row++) {
            inv.setItem(row * 9, glass);
            inv.setItem(row * 9 + 8, glass);
        }
    }
}
