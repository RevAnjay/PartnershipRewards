package github.revanjay.partnershiprewards.listener;

import github.revanjay.partnershiprewards.PartnershipRewards;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class PlayerListener implements Listener {
    
    private final PartnershipRewards plugin;
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPartnershipManager().loadPartnership(event.getPlayer().getUniqueId());
    }

    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getQuestManager().unloadPlayer(event.getPlayer().getUniqueId());
        plugin.getPartnershipManager().unloadPartnership(event.getPlayer().getUniqueId());
    }
}
