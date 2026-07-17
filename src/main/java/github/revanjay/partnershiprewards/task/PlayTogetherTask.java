package github.revanjay.partnershiprewards.task;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.ActiveQuest;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.model.QuestType;
import github.revanjay.partnershiprewards.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static github.revanjay.partnershiprewards.PartnershipRewards.sendActionBar;

public class PlayTogetherTask {

    private final PartnershipRewards plugin;
    private SchedulerUtil.TaskHandle taskHandle;

    public PlayTogetherTask(PartnershipRewards plugin) {
        this.plugin = plugin;
    }

    public void run() {
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                processPlayer(player);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in PlayTogetherTask: " + e.getMessage());
        }
    }

    private void processPlayer(Player player) {
        try {
            if (!plugin.getQuestManager().hasActiveQuest(player.getUniqueId(), QuestType.PLAY_TOGETHER)) {
                return;
            }

            ActiveQuest quest = plugin.getQuestManager().getActiveQuest(player.getUniqueId());
            if (quest == null) return;
            Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
            if (partnership == null) return;

            Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
            if (partner == null) return;
            if (player.getUniqueId().compareTo(partner.getUniqueId()) > 0) return;
            boolean completed = plugin.getQuestManager().updateQuestProgress(
                player.getUniqueId(),
                QuestType.PLAY_TOGETHER,
                1
            );
            if (!completed && quest.getProgress() % 5 == 0 && quest.getProgress() > 0) {
                String actionBarMsg = plugin.getLanguageManager().getMessage("quest-action-play")
                    .replace("{current}", String.valueOf(quest.getProgress()))
                    .replace("{required}", String.valueOf(quest.getRequiredAmount()));

                sendActionBar(player, actionBarMsg);
                sendActionBar(partner, actionBarMsg);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing player " + player.getName() + ": " + e.getMessage());
        }
    }

    public void start() {
        taskHandle = SchedulerUtil.runTaskTimer(plugin, this::run, 1200L, 1200L);
    }

    public void cancel() {
        if (taskHandle != null) {
            taskHandle.cancel();
        }
    }
}
