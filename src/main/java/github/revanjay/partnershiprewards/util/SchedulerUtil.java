package github.revanjay.partnershiprewards.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

/**
 * Folia-aware scheduler utility.
 * On Paper: delegates to Bukkit.getScheduler().
 * On Folia: delegates to GlobalRegionScheduler / AsyncScheduler.
 */
public final class SchedulerUtil {

    private static final boolean FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
        }
        FOLIA = folia;
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    // --- Sync (global region) ---

    public static void runTask(JavaPlugin plugin, Runnable task) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runTaskLater(JavaPlugin plugin, Runnable task, long delayTicks) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static TaskHandle runTaskTimer(JavaPlugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            var st = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), delayTicks, periodTicks);
            return st::cancel;
        } else {
            var bt = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
            return bt::cancel;
        }
    }

    // --- Async ---

    public static void runTaskAsynchronously(JavaPlugin plugin, Runnable task) {
        if (FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static TaskHandle runTaskTimerAsynchronously(JavaPlugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            var st = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> task.run(), delayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS);
            return st::cancel;
        } else {
            var bt = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
            return bt::cancel;
        }
    }

    @FunctionalInterface
    public interface TaskHandle {
        void cancel();
    }

    private SchedulerUtil() {}
}
