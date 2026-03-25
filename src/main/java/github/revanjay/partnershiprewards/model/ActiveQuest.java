package github.revanjay.partnershiprewards.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveQuest {
    private int id;
    private int partnershipId;
    private QuestType questType;
    private String target;
    private int requiredAmount;
    private int progress;
    private long createdAt;
    @Builder.Default
    private volatile boolean rewardClaimed = false;
    
        public boolean isCompleted() {
        return progress >= requiredAmount;
    }
    
        public int getCompletionPercentage() {
        if (requiredAmount <= 0) return 100;
        return Math.min(100, (progress * 100) / requiredAmount);
    }
    
        public synchronized boolean addProgress(int amount) {
        // If reward already claimed, don't allow double completion
        if (rewardClaimed) return false;
        
        if (amount < 0) amount = 0; // Prevent negative progress
        this.progress += amount;
        if (this.progress > requiredAmount) {
            this.progress = requiredAmount; // Cap at max
        }
        if (isCompleted() && !rewardClaimed) {
            rewardClaimed = true;
            return true;
        }
        return false;
    }
    
        public String getFormattedDescription() {
        return questType.formatDescription(target, requiredAmount);
    }
    
        public String getProgressBar() {
        int percentage = getCompletionPercentage();
        int filled = percentage / 10;
        int empty = 10 - filled;
        
        StringBuilder bar = new StringBuilder("Â§a");
        for (int i = 0; i < filled; i++) bar.append("â–ˆ");
        bar.append("Â§7");
        for (int i = 0; i < empty; i++) bar.append("â–ˆ");
        
        return bar.toString();
    }
}

