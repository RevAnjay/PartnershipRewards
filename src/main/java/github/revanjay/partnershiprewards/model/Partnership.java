package github.revanjay.partnershiprewards.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

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
    
    
    @Builder.Default
    private boolean effectsEnabled = true;
    
    
    private String homeWorld;
    @Builder.Default
    private double homeX = 0;
    @Builder.Default
    private double homeY = 0;
    @Builder.Default
    private double homeZ = 0;
    @Builder.Default
    private float homeYaw = 0;
    @Builder.Default
    private float homePitch = 0;
    
    
    @Builder.Default
    private int loginStreak = 0;
    @Builder.Default
    private long lastStreakDate = 0;
    @Builder.Default
    private long player1LastLogin = 0;
    @Builder.Default
    private long player2LastLogin = 0;
    
    public UUID getPartner(UUID player) {
        return player.equals(player1) ? player2 : player1;
    }
    
    public boolean isPlayer1(UUID player) {
        return player.equals(player1);
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
    
    public boolean hasHome() {
        return homeWorld != null && !homeWorld.isEmpty();
    }
    
    public Location getHomeLocation() {
        if (!hasHome()) return null;
        var world = Bukkit.getWorld(homeWorld);
        if (world == null) return null;
        return new Location(world, homeX, homeY, homeZ, homeYaw, homePitch);
    }
    
    public void setHome(Location loc) {
        this.homeWorld = loc.getWorld().getName();
        this.homeX = loc.getX();
        this.homeY = loc.getY();
        this.homeZ = loc.getZ();
        this.homeYaw = loc.getYaw();
        this.homePitch = loc.getPitch();
    }
}
