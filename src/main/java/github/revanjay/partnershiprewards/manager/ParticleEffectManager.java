package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ParticleEffectManager {

    private final PartnershipRewards plugin;
    private SchedulerUtil.TaskHandle particleTask;

    public ParticleEffectManager(PartnershipRewards plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("effects.particles.enabled", true)
                && !plugin.getConfig().getBoolean("partner-effects.enabled", true)) {
            return;
        }

        int interval = plugin.getConfig().getInt("effects.heart.interval-ticks",
                plugin.getConfig().getInt("partner-effects.interval-ticks", 60));
        double maxDistance = plugin.getConfig().getDouble("partner-effects.max-distance", 10.0);

        particleTask = SchedulerUtil.runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                processParticles(player, maxDistance);
            }
        }, 60L, interval);
    }

    public void shutdown() {
        if (particleTask != null) {
            particleTask.cancel();
        }
    }

    private void processParticles(Player player, double maxDistance) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null || !partnership.isEffectsEnabled()) return;
        if (!partnership.isPlayer1(player.getUniqueId())) return;

        Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
        if (partner == null || !partner.isOnline()) return;
        if (!player.getWorld().equals(partner.getWorld())) return;

        double distance = player.getLocation().distance(partner.getLocation());
        if (distance > maxDistance) return;

        spawnHeartConnection(player, partner);
    }

    public void spawnHeartConnection(Player p1, Player p2) {
        p1.getWorld().spawnParticle(Particle.HEART, p1.getLocation().add(0, 2.2, 0), 2, 0.25, 0.1, 0.25, 0);
        p2.getWorld().spawnParticle(Particle.HEART, p2.getLocation().add(0, 2.2, 0), 2, 0.25, 0.1, 0.25, 0);
    }

    public void playLevelUpEffect(Player player) {
        if (player == null || !player.isOnline()) return;
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.TOTEM, loc.add(0, 1.0, 0), 35, 0.5, 0.8, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 25, 0.4, 0.6, 0.4, 0.08);
        player.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1.0, 0), 8, 0.4, 0.4, 0.4, 0);
    }

    public void playAchievementEffect(Player player) {
        if (player == null || !player.isOnline()) return;
        Location loc = player.getLocation().add(0, 1.0, 0);
        player.getWorld().spawnParticle(Particle.REDSTONE, loc, 30, 0.6, 0.6, 0.6,
                new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f));
        player.getWorld().spawnParticle(Particle.END_ROD, loc, 15, 0.3, 0.5, 0.3, 0.05);
    }

    public void playAnniversaryEffect(Player player) {
        if (player == null || !player.isOnline()) return;
        Location loc = player.getLocation().add(0, 1.5, 0);
        player.getWorld().spawnParticle(Particle.HEART, loc, 20, 0.8, 0.8, 0.8, 0.1);
        player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 25, 0.8, 0.8, 0.8, 0.05);
    }
}
