package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class EffectManager {
    
    private final PartnershipRewards plugin;
    private BukkitTask effectTask;
    
    public EffectManager(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    public void start() {
        if (!plugin.getConfig().getBoolean("partner-effects.enabled", true)) return;
        
        int interval = plugin.getConfig().getInt("partner-effects.interval-ticks", 60);
        double maxDistance = plugin.getConfig().getDouble("partner-effects.max-distance", 10.0);
        
        effectTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                processPlayer(player, maxDistance);
            }
        }, 60L, interval);
    }
    
    public void shutdown() {
        if (effectTask != null) {
            effectTask.cancel();
        }
    }
    
    private void processPlayer(Player player, double maxDistance) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) return;
        if (!partnership.isEffectsEnabled()) return;
        
        if (!partnership.isPlayer1(player.getUniqueId())) return;
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
        if (partner == null) return;
        if (!player.getWorld().equals(partner.getWorld())) return;
        
        double distance = player.getLocation().distance(partner.getLocation());
        if (distance > maxDistance) return;
        
        int level = partnership.getLevel();
        spawnEffects(player, partner, level);
    }
    
    private void spawnEffects(Player player, Player partner, int level) {
        if (level >= 1) {
            player.getWorld().spawnParticle(Particle.HEART,
                player.getLocation().add(0, 2.2, 0), 1, 0.3, 0.1, 0.3, 0);
            partner.getWorld().spawnParticle(Particle.HEART,
                partner.getLocation().add(0, 2.2, 0), 1, 0.3, 0.1, 0.3, 0);
        }
        
        
        if (level >= 5) {
            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
                player.getLocation().add(0, 1.0, 0), 2, 0.3, 0.3, 0.3, 0);
            partner.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
                partner.getLocation().add(0, 1.0, 0), 2, 0.3, 0.3, 0.3, 0);
        }
        
        
        if (level >= 15) {
            player.getWorld().spawnParticle(Particle.END_ROD,
                player.getLocation().add(0, 1.5, 0), 1, 0.2, 0.5, 0.2, 0.01);
            partner.getWorld().spawnParticle(Particle.END_ROD,
                partner.getLocation().add(0, 1.5, 0), 1, 0.2, 0.5, 0.2, 0.01);
        }
        
        
        if (level >= 25) {
            player.getWorld().spawnParticle(Particle.CHERRY_LEAVES,
                player.getLocation().add(0, 2.5, 0), 3, 0.5, 0.3, 0.5, 0.01);
            partner.getWorld().spawnParticle(Particle.CHERRY_LEAVES,
                partner.getLocation().add(0, 2.5, 0), 3, 0.5, 0.3, 0.5, 0.01);
        }
    }
}
