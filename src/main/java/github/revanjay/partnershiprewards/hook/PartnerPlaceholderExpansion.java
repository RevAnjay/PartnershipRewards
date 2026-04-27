package github.revanjay.partnershiprewards.hook;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.ActiveQuest;
import github.revanjay.partnershiprewards.model.Partnership;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public class PartnerPlaceholderExpansion extends PlaceholderExpansion {
    
    private final PartnershipRewards plugin;
    
    public PartnerPlaceholderExpansion(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "partner";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) return "";
        
        UUID uuid = offlinePlayer.getUniqueId();
        Partnership partnership = plugin.getPartnershipManager().getPartnership(uuid);
        
        return switch (params.toLowerCase()) {
            case "name" -> {
                if (partnership == null) yield "None";
                UUID partnerUuid = partnership.getPartner(uuid);
                String name = Bukkit.getOfflinePlayer(partnerUuid).getName();
                yield name != null ? name : "Unknown";
            }
            case "level" -> {
                if (partnership == null) yield "0";
                yield String.valueOf(partnership.getLevel());
            }
            case "xp" -> {
                if (partnership == null) yield "0";
                yield String.valueOf(partnership.getXp());
            }
            case "duration" -> {
                if (partnership == null) yield "None";
                yield formatDuration(partnership.getDurationInSeconds());
            }
            case "online" -> {
                if (partnership == null) yield "N/A";
                UUID partnerUuid = partnership.getPartner(uuid);
                yield Bukkit.getPlayer(partnerUuid) != null ? "Online" : "Offline";
            }
            case "title" -> {
                if (partnership == null) yield "";
                yield getTitle(partnership.getLevel());
            }
            case "quest" -> {
                if (partnership == null) yield "None";
                ActiveQuest quest = plugin.getQuestManager().getActiveQuest(uuid);
                yield quest != null ? quest.getFormattedDescription() : "None";
            }
            case "streak" -> {
                if (partnership == null) yield "0";
                yield String.valueOf(partnership.getLoginStreak());
            }
            case "has_partner" -> {
                yield partnership != null ? "true" : "false";
            }
            case "days" -> {
                if (partnership == null) yield "0";
                yield String.valueOf(partnership.getDurationInDays());
            }
            default -> null;
        };
    }
    
    private String getTitle(int level) {
        if (!plugin.getConfig().getBoolean("partner-titles.enabled", true)) return "";
        
        ConfigurationSection titles = plugin.getConfig().getConfigurationSection("partner-titles.titles");
        if (titles == null) return "";
        
        String bestTitle = "";
        int bestLevel = 0;
        
        for (String key : titles.getKeys(false)) {
            try {
                int titleLevel = Integer.parseInt(key);
                if (titleLevel <= level && titleLevel > bestLevel) {
                    bestLevel = titleLevel;
                    bestTitle = titles.getString(key, "");
                }
            } catch (NumberFormatException ignored) {}
        }
        
        if (bestTitle.isEmpty()) return "";
        
        String format = plugin.getConfig().getString("partner-titles.format", "&d[{title}] ");
        return PartnershipRewards.colorize(format.replace("{title}", bestTitle));
    }
    
    private String formatDuration(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        
        if (days > 0) return days + "d " + hours + "h";
        else if (hours > 0) return hours + "h " + minutes + "m";
        else return minutes + "m";
    }
}
