package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.Partnership;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class PartnershipManager {
    
    private final PartnershipRewards plugin;
    private final Map<UUID, Partnership> partnershipCache = new ConcurrentHashMap<>();
    
    public void createPartnership(UUID player1, UUID player2) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().createPartnership(player1, player2);
            Partnership partnership = plugin.getDatabaseManager().getPartnership(player1);
            if (partnership != null) {
                partnershipCache.put(player1, partnership);
                partnershipCache.put(player2, partnership);
            }
        });
    }
    
    public void breakPartnership(UUID player) {
        Partnership partnership = getPartnership(player);
        if (partnership != null) {
            UUID partner = partnership.getPartner(player);
            partnershipCache.remove(player);
            partnershipCache.remove(partner);
            plugin.getQuestManager().unloadPlayer(player);
            plugin.getQuestManager().unloadPlayer(partner);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getDatabaseManager().deletePartnership(player)
            );
        }
    }
    
        public void breakPartnershipDB(UUID player) {
        // Try cache first
        Partnership cached = getPartnership(player);
        if (cached != null) {
            breakPartnership(player);
            return;
        }
        
        // Not cached â€” query DB async and delete
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Partnership partnership = plugin.getDatabaseManager().getPartnership(player);
            if (partnership != null) {
                UUID partner = partnership.getPartner(player);
                partnershipCache.remove(player);
                partnershipCache.remove(partner);
                plugin.getDatabaseManager().deletePartnership(player);
            }
        });
    }
    
        public Partnership getPartnership(UUID player) {
        return partnershipCache.get(player);
    }
    
    public boolean hasPartner(UUID player) {
        return getPartnership(player) != null;
    }
    
        public void hasPartnerDB(UUID player, java.util.function.Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Partnership partnership = plugin.getDatabaseManager().getPartnership(player);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(partnership != null));
        });
    }
    
    public UUID getPartnerUUID(UUID player) {
        Partnership partnership = getPartnership(player);
        return partnership != null ? partnership.getPartner(player) : null;
    }
    
        public void loadPartnership(UUID player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Partnership partnership = plugin.getDatabaseManager().getPartnership(player);
            if (partnership == null) return;
            partnershipCache.put(player, partnership);
            partnershipCache.put(partnership.getPartner(player), partnership);
            plugin.getQuestManager().loadPlayerQuestAsync(player, partnership);
        });
    }
    
        public void loadPartnershipSync(UUID player) {
        Partnership partnership = plugin.getDatabaseManager().getPartnership(player);
        if (partnership != null) {
            partnershipCache.put(player, partnership);
            partnershipCache.put(partnership.getPartner(player), partnership);
        }
    }
    
        public void unloadPartnership(UUID player) {
        Partnership partnership = partnershipCache.remove(player);
        if (partnership != null) {
            UUID partner = partnership.getPartner(player);
            if (partner != null && Bukkit.getPlayer(partner) == null) {
                partnershipCache.remove(partner);
            }
        }
    }
    
        public List<Partnership> getAllPartnerships() {
        return plugin.getDatabaseManager().getAllPartnerships();
    }
}
