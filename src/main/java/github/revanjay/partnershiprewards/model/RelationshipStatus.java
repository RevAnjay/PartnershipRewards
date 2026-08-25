package github.revanjay.partnershiprewards.model;

import github.revanjay.partnershiprewards.PartnershipRewards;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.TreeMap;

@Getter
public class RelationshipStatus {

    private static final TreeMap<Integer, RelationshipStatus> LOADED_STATUSES = new TreeMap<>();
    private static final RelationshipStatus DEFAULT_STATUS = new RelationshipStatus(1, "&a💚 Buddy", "&a[Buddy] ");

    private final int minLevel;
    private final String display;
    private final String chatPrefix;

    public RelationshipStatus(int minLevel, String display, String chatPrefix) {
        this.minLevel = minLevel;
        this.display = display;
        this.chatPrefix = chatPrefix;
    }

    public static void loadFromConfig(PartnershipRewards plugin) {
        LOADED_STATUSES.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("relationship.levels");
        if (section == null) {
            LOADED_STATUSES.put(1, new RelationshipStatus(1, "&a💚 Buddy", "&a[Buddy] "));
            LOADED_STATUSES.put(5, new RelationshipStatus(5, "&9💙 Duo", "&9[Duo] "));
            LOADED_STATUSES.put(10, new RelationshipStatus(10, "&5💜 Bonded", "&5[Bonded] "));
            LOADED_STATUSES.put(20, new RelationshipStatus(20, "&d💗 Partners", "&d[Partners] "));
            LOADED_STATUSES.put(30, new RelationshipStatus(30, "&c❤ Soulmates", "&c[Soulmates] "));
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                String name = section.getString(key + ".name", "&a💚 Level " + level);
                String prefix = section.getString(key + ".prefix", "&a[Level " + level + "] ");
                LOADED_STATUSES.put(level, new RelationshipStatus(level, name, prefix));
            } catch (NumberFormatException ignored) {
            }
        }

        if (LOADED_STATUSES.isEmpty()) {
            LOADED_STATUSES.put(1, DEFAULT_STATUS);
        }
    }

    public static RelationshipStatus fromLevel(int level) {
        if (LOADED_STATUSES.isEmpty()) {
            return DEFAULT_STATUS;
        }
        Map.Entry<Integer, RelationshipStatus> entry = LOADED_STATUSES.floorEntry(level);
        return entry != null ? entry.getValue() : LOADED_STATUSES.firstEntry().getValue();
    }
}
