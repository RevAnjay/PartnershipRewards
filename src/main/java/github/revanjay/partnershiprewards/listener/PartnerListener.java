package github.revanjay.partnershiprewards.listener;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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
                attacker.sendMessage(colorize("&d&lPartner &8» &7PvP with partner is disabled. Use &e/partner toggle pvp &7to enable."));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getChatManager().isChatToggled(player.getUniqueId())) return;
        
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            plugin.getChatManager().toggleChat(player.getUniqueId());
            return;
        }
        
        event.setCancelled(true);
        
        String message = event.getMessage();
        Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
        
        String chatFormat = colorize("&d[Partner] &f" + player.getName() + "&7: &f" + message);
        player.sendMessage(chatFormat);
        
        if (partner != null) {
            partner.sendMessage(chatFormat);
            notifySpyingAdmins(player, partner, message);
        } else {
            player.sendMessage(colorize("&7(Partner is offline, message not delivered)"));
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
        String spyFormat = colorize("&8[&cSPY&8] &d[Partner] &f" + sender.getName() + " &7→ &f" + receiver.getName() + "&7: " + message);
        
        for (UUID adminUuid : spyingAdmins) {
            Player admin = Bukkit.getPlayer(adminUuid);
            if (admin != null && admin.hasPermission("partner.admin.spy")) {
                admin.sendMessage(spyFormat);
            }
        }
    }
}

