package github.revanjay.partnershiprewards.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Partnership {
    private int id;
    private UUID player1;
    private UUID player2;
    private long startedAt;
    private long lastRewardCheck;
    @Builder.Default
    private int level = 1;
    @Builder.Default
    private int xp = 0;
    @Builder.Default
    private long lastQuestComplete = 0;
    @Builder.Default
    private boolean pvpEnabled = false;
    
    public UUID getPartner(UUID player) {
        return player.equals(player1) ? player2 : player1;
    }
    
    public long getDurationInSeconds() {
        return Instant.now().getEpochSecond() - startedAt;
    }
    
    public long getDurationInDays() {
        return getDurationInSeconds() / 86400;
    }
    
    public boolean hasPlayer(UUID player) {
        return player.equals(player1) || player.equals(player2);
    }
}
