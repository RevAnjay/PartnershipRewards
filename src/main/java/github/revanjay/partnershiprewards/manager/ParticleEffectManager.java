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

        Location l1 = player.getLocation();
        Location l2 = partner.getLocation();
        double maxDistSq = maxDistance * maxDistance;
        if (l1.distanceSquared(l2) > maxDistSq) return;

        spawnHeartConnection(player, partner);
    }

    public void spawnHeartConnection(Player p1, Player p2) {
        Location l1 = p1.getLocation();
        Location l2 = p2.getLocation();
        p1.getWorld().spawnParticle(Particle.HEART, l1.getX(), l1.getY() + 2.2, l1.getZ(), 2, 0.25, 0.1, 0.25, 0);
        p2.getWorld().spawnParticle(Particle.HEART, l2.getX(), l2.getY() + 2.2, l2.getZ(), 2, 0.25, 0.1, 0.25, 0);
    }

    public void playLevelUpEffect(Player player) {
        if (player == null || !player.isOnline()) return;
        Location loc = player.getLocation();
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        player.getWorld().spawnParticle(Particle.TOTEM, x, y + 1.0, z, 35, 0.5, 0.8, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, x, y, z, 25, 0.4, 0.6, 0.4, 0.08);
        player.getWorld().spawnParticle(Particle.HEART, x, y + 1.0, z, 8, 0.4, 0.4, 0.4, 0);
    }

    public void playAchievementEffect(Player player) {
        if (player == null || !player.isOnline()) return;
        Location loc = player.getLocation();
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        player.getWorld().spawnParticle(Particle.REDSTONE, x, y + 1.0, z, 30, 0.6, 0.6, 0.6,
                new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f));
        player.getWorld().spawnParticle(Particle.END_ROD, x, y, z, 15, 0.3, 0.5, 0.3, 0.05);
    }

    public void playAnniversaryEffect(Player player) {
        if (player == null || !player.isOnline()) return;
        Location loc = player.getLocation();
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        player.getWorld().spawnParticle(Particle.HEART, x, y + 1.5, z, 20, 0.8, 0.8, 0.8, 0.1);
        player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, x, y + 1.5, z, 25, 0.8, 0.8, 0.8, 0.05);
    }
}
