package github.revanjay.partnershiprewards.listener;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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
        if (attacker.equals(victim)) return; // Self-damage
        Partnership partnership = plugin.getPartnershipManager().getPartnership(attacker.getUniqueId());
        if (partnership == null) return;
        UUID partnerUuid = partnership.getPartner(attacker.getUniqueId());
        if (!partnerUuid.equals(victim.getUniqueId())) return;
        if (!partnership.isPvpEnabled()) {
            event.setCancelled(true);
            if (ThreadLocalRandom.current().nextInt(10) == 0) { // 10% chance to show message
                attacker.sendMessage("Â§dÂ§lPartner Â§8Â» Â§7PvP dengan partner dinonaktifkan. Gunakan Â§e/partner toggle pvp Â§7untuk mengaktifkan.");
            }
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
        String spyFormat = "Â§8[Â§cSPYÂ§8] Â§d[Partner] Â§f" + sender.getName() + " Â§7â†’ Â§f" + receiver.getName() + "Â§7: " + message;
        
        for (UUID adminUuid : spyingAdmins) {
            Player admin = org.bukkit.Bukkit.getPlayer(adminUuid);
            if (admin != null && admin.hasPermission("partner.admin.spy")) {
                admin.sendMessage(spyFormat);
            }
        }
    }
}

