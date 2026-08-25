package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class AnniversaryManager {

    private final PartnershipRewards plugin;
    private final Set<String> rewardedMilestones = new HashSet<>();
    private SchedulerUtil.TaskHandle taskHandle;

    public AnniversaryManager(PartnershipRewards plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("anniversary.enabled", true)) return;

        taskHandle = SchedulerUtil.runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkAnniversary(player);
            }
        }, 20L * 60, 20L * 60 * 30);
    }

    public void shutdown() {
        if (taskHandle != null) {
            taskHandle.cancel();
        }
    }

    public void checkAnniversary(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) return;
        if (!partnership.isPlayer1(player.getUniqueId())) return;

        long daysTogether = partnership.getDurationInDays();
        checkAndReward(partnership, daysTogether);
    }

    private void checkAndReward(Partnership partnership, long days) {
        if (days >= 365) {
            triggerMilestone(partnership, 365, "1 Year Anniversary!", Material.NETHERITE_CHESTPLATE);
        } else if (days >= 30) {
            triggerMilestone(partnership, 30, "1 Month Anniversary!", Material.CAKE);
        } else if (days >= 1) {
            triggerMilestone(partnership, 1, "First Day Together!", Material.DIAMOND_BLOCK);
        }
    }

    private void triggerMilestone(Partnership partnership, int milestoneDays, String title, Material reward) {
        String key = partnership.getId() + "_" + milestoneDays;
        if (rewardedMilestones.contains(key)) return;
        rewardedMilestones.add(key);

        Player p1 = Bukkit.getPlayer(partnership.getPlayer1());
        Player p2 = Bukkit.getPlayer(partnership.getPlayer2());
        String broadcastMsg = "&#00C6F9&l[ANNIVERSARY]&r &#00C6F9" + (p1 != null ? p1.getName() : "Partner 1") + " &7& &#00C6F9" +
                (p2 != null ? p2.getName() : "Partner 2") + " &7are celebrating their &#00C6F9" + title + "&7!";

        Bukkit.broadcast(PartnershipRewards.colorizeComponent(broadcastMsg));

        if (p1 != null && p1.isOnline()) {
            p1.getInventory().addItem(new ItemStack(reward));
            PartnershipRewards.playLevelUpSound(p1);
            if (plugin.getParticleEffectManager() != null) plugin.getParticleEffectManager().playAnniversaryEffect(p1);
        }
        if (p2 != null && p2.isOnline()) {
            p2.getInventory().addItem(new ItemStack(reward));
            PartnershipRewards.playLevelUpSound(p2);
            if (plugin.getParticleEffectManager() != null) plugin.getParticleEffectManager().playAnniversaryEffect(p2);
        }
    }
}
