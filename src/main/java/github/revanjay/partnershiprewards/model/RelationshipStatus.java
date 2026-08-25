package github.revanjay.partnershiprewards.model;

import lombok.Getter;

@Getter
public enum RelationshipStatus {
    CRUSH(1, "&a💚 Crush", "&a[Crush] "),
    DATE(5, "&9💙 Dating", "&9[Dating] "),
    BOYFRIEND(10, "&5💜 Couple", "&5[Couple] "),
    SPOUSE(20, "&d💗 Married", "&d[Married] "),
    SOULMATE(30, "&c❤ Soulmate", "&c[Soulmate] ");

    private final int minLevel;
    private final String display;
    private final String chatPrefix;

    RelationshipStatus(int minLevel, String display, String chatPrefix) {
        this.minLevel = minLevel;
        this.display = display;
        this.chatPrefix = chatPrefix;
    }

    public static RelationshipStatus fromLevel(int level) {
        RelationshipStatus current = CRUSH;
        for (RelationshipStatus status : values()) {
            if (level >= status.getMinLevel()) {
                current = status;
            }
        }
        return current;
    }
}
