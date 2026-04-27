package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ChatManager {
    
    private final PartnershipRewards plugin;
    private final Set<UUID> chatToggled = ConcurrentHashMap.newKeySet();
    
    public boolean toggleChat(UUID player) {
        if (chatToggled.contains(player)) {
            chatToggled.remove(player);
            return false;
        } else {
            chatToggled.add(player);
            return true;
        }
    }
    
    public boolean isChatToggled(UUID player) {
        return chatToggled.contains(player);
    }
    
    public void unload(UUID player) {
        chatToggled.remove(player);
    }
}
