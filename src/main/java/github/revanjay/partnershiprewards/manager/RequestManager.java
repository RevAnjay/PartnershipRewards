package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.PartnerRequest;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class RequestManager {
    
    private final PartnershipRewards plugin;
    private final Map<UUID, PartnerRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    
    public void createRequest(UUID sender, UUID target) {
        PartnerRequest request = new PartnerRequest(sender, target, System.currentTimeMillis());
        pendingRequests.put(target, request);
        cooldowns.put(sender, System.currentTimeMillis());
    }
    
    public PartnerRequest getRequest(UUID target) {
        PartnerRequest request = pendingRequests.get(target);
        if (request != null && request.isExpired()) {
            pendingRequests.remove(target);
            return null;
        }
        return request;
    }
    
    public void removeRequest(UUID target) {
        pendingRequests.remove(target);
    }
    
    public boolean hasPendingRequest(UUID target) {
        return getRequest(target) != null;
    }
    
    public boolean isOnCooldown(UUID player) {
        Long lastUse = cooldowns.get(player);
        if (lastUse == null) return false;
        
        int cooldownSeconds = plugin.getConfig().getInt("partnership.cooldown-seconds", 60);
        return System.currentTimeMillis() - lastUse < cooldownSeconds * 1000L;
    }
    
    public long getRemainingCooldown(UUID player) {
        Long lastUse = cooldowns.get(player);
        if (lastUse == null) return 0;
        
        int cooldownSeconds = plugin.getConfig().getInt("partnership.cooldown-seconds", 60);
        long elapsed = System.currentTimeMillis() - lastUse;
        long remaining = (cooldownSeconds * 1000L) - elapsed;
        
        return Math.max(0, remaining / 1000);
    }
}
