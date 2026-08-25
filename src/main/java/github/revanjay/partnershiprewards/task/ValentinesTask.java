package github.revanjay.partnershiprewards.task;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.Month;

public class ValentinesTask {

    private final PartnershipRewards plugin;
    private SchedulerUtil.TaskHandle taskHandle;

    public ValentinesTask(PartnershipRewards plugin) {
        this.plugin = plugin;
    }

    public boolean isValentinesEventActive() {
        if (!plugin.getConfig().getBoolean("events.valentines.enabled", true)) return false;
        LocalDate now = LocalDate.now();
        return now.getMonth() == Month.FEBRUARY && now.getDayOfMonth() >= 10 && now.getDayOfMonth() <= 16;
    }

    public void start() {
        taskHandle = SchedulerUtil.runTaskTimer(plugin, () -> {
            if (!isValentinesEventActive()) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
                if (partnership != null) {
                    player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2.0, 0), 1, 0.2, 0.2, 0.2, 0);
                }
            }
        }, 20L * 30, 20L * 10);
    }

    public void shutdown() {
        if (taskHandle != null) {
            taskHandle.cancel();
        }
    }
}
