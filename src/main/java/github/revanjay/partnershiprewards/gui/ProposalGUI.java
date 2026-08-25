package github.revanjay.partnershiprewards.gui;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.List;

public class ProposalGUI implements InventoryHolder, Listener {

    private final PartnershipRewards plugin;
    private final Player proposer;
    private final Player target;
    private final Inventory inventory;
    private boolean responded = false;

    public ProposalGUI(PartnershipRewards plugin, Player proposer, Player target) {
        this.plugin = plugin;
        this.proposer = proposer;
        this.target = target;
        this.inventory = Bukkit.createInventory(this, 27, PartnershipRewards.colorizeComponent("&d&lMarriage Proposal from " + proposer.getName()));
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setupGUI();
    }

    private void setupGUI() {
        EnhancedGUI.fillBorder(inventory, Material.PINK_STAINED_GLASS_PANE);

        inventory.setItem(11, EnhancedGUI.createGradientItem(
            Material.LIME_CONCRETE,
            "&a&lYES, I DO! 💍",
            List.of("&7Click to accept the proposal from &e" + proposer.getName())
        ));

        inventory.setItem(13, EnhancedGUI.createGradientItem(
            Material.DIAMOND,
            "&b&lEngagement Ring",
            List.of(
                "&d" + proposer.getName() + " &fhas proposed to you!",
                "&7Do you accept to spend your adventures together?"
            )
        ));

        inventory.setItem(15, EnhancedGUI.createGradientItem(
            Material.RED_CONCRETE,
            "&c&lNO, NOT YET",
            List.of("&7Click to decline the proposal politely")
        ));
    }

    public void open() {
        target.openInventory(inventory);
        EnhancedGUI.playOpenEffects(target);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);
        if (responded) return;

        int slot = event.getRawSlot();
        if (slot == 11) {
            responded = true;
            acceptProposal();
            target.closeInventory();
        } else if (slot == 15) {
            responded = true;
            declineProposal();
            target.closeInventory();
        }
    }

    private void acceptProposal() {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(proposer.getUniqueId());
        if (partnership != null) {
            partnership.setEngagementDate(System.currentTimeMillis());
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().updateEngagementDate(partnership.getId(), partnership.getEngagementDate());
            });
            if (plugin.getAchievementManager() != null) {
                plugin.getAchievementManager().checkAndProgress(partnership, "ENGAGED", 1);
            }
        }

        String broadcast = "&d&l💘 LOVE ALERT! &f" + proposer.getName() + " &dproposed to &f" + target.getName() + "&d and they said &a&lYES! 💍🎉";
        Bukkit.broadcast(PartnershipRewards.colorizeComponent(broadcast));

        proposer.playSound(proposer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        target.playSound(target.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        spawnFireworks(target);
        spawnFireworks(proposer);
    }

    private void declineProposal() {
        proposer.sendMessage(PartnershipRewards.colorize("&c" + target.getName() + " has politely declined your proposal for now."));
        target.sendMessage(PartnershipRewards.colorize("&7You declined " + proposer.getName() + "'s proposal."));
        proposer.playSound(proposer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    private void spawnFireworks(Player player) {
        if (player == null || !player.isOnline()) return;
        Firework fw = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
            .with(FireworkEffect.Type.BALL_LARGE)
            .withColor(Color.FUCHSIA, Color.RED, Color.YELLOW)
            .withFade(Color.WHITE)
            .trail(true)
            .flicker(true)
            .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            HandlerList.unregisterAll(this);
            if (!responded) {
                declineProposal();
            }
        }
    }
}
