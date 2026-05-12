package github.revanjay.partnershiprewards.listener;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PartnerListener implements Listener {
    
    private final PartnershipRewards plugin;
    private final Set<UUID> spyingAdmins = new HashSet<>();
    
    public PartnerListener(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPartnerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        }
        else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player p) {
                attacker = p;
            }
        }
        
        if (attacker == null) return;
        if (attacker.equals(victim)) return;
        Partnership partnership = plugin.getPartnershipManager().getPartnership(attacker.getUniqueId());
        if (partnership == null) return;
        UUID partnerUuid = partnership.getPartner(attacker.getUniqueId());
        if (!partnerUuid.equals(victim.getUniqueId())) return;
        if (!partnership.isPvpEnabled()) {
            event.setCancelled(true);
            if (ThreadLocalRandom.current().nextInt(10) == 0) {
                attacker.sendMessage(plugin.getLanguageManager().getMessage("pvp-denied", true));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getChatManager().isChatToggled(player.getUniqueId())) return;
        
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            plugin.getChatManager().toggleChat(player.getUniqueId());
            return;
        }
        
        event.setCancelled(true);
        
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
        
        String chatFormat = plugin.getLanguageManager().getMessage("chat-format")
            .replace("{player}", player.getName())
            .replace("{message}", message);
        player.sendMessage(chatFormat);
        
        if (partner != null) {
            partner.sendMessage(chatFormat);
            notifySpyingAdmins(player, partner, message);
        } else {
            player.sendMessage(plugin.getLanguageManager().getMessage("chat-partner-offline", true));
        }
    }
    
    public boolean isSpying(UUID adminUuid) {
        return spyingAdmins.contains(adminUuid);
    }
    
    public void toggleSpy(UUID adminUuid) {
        if (spyingAdmins.contains(adminUuid)) {
            spyingAdmins.remove(adminUuid);
        } else {
            spyingAdmins.add(adminUuid);
        }
    }
    
    public Set<UUID> getSpyingAdmins() {
        return spyingAdmins;
    }
    
    public void notifySpyingAdmins(Player sender, Player receiver, String message) {
        String spyFormat = plugin.getLanguageManager().getMessage("chat-spy-format")
            .replace("{sender}", sender.getName())
            .replace("{receiver}", receiver.getName())
            .replace("{message}", message);
        
        for (UUID adminUuid : spyingAdmins) {
            Player admin = Bukkit.getPlayer(adminUuid);
            if (admin != null && admin.hasPermission("partner.admin.spy")) {
                admin.sendMessage(spyFormat);
            }
        }
    }
}

