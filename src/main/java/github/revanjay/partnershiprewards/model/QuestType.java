package github.revanjay.partnershiprewards.model;

public enum QuestType {
    GIVE_ITEM("Berikan Item", "Berikan {target} ke partner kamu", "PlayerInteractEntityEvent"),
    SEND_MESSAGE("Kirim Pesan", "Kirim {amount} pesan di chat", "AsyncPlayerChatEvent"),
    USE_COMMAND("Gunakan Command", "Gunakan command {target} sebanyak {amount}x", "PlayerCommandPreprocessEvent"),
    EAT_FOOD("Makan Bersama", "Makan {amount} makanan", "PlayerItemConsumeEvent"),
    SLEEP_TOGETHER("Tidur Bersama", "Tidur bersama partner {amount}x", "PlayerBedEnterEvent"),
    FISH_CATCH("Memancing", "Tangkap {amount} ikan", "PlayerFishEvent"),
    TRADE_VILLAGER("Trading Villager", "Trading dengan villager {amount}x", "InventoryClickEvent"),
    ENCHANT_ITEM("Enchant Item", "Enchant {amount} item", "EnchantItemEvent"),
    ANVIL_USE("Gunakan Anvil", "Gunakan anvil {amount}x", "PrepareAnvilEvent"),
    BREW_POTION("Brewing Potion", "Brew {amount} potion", "BrewEvent"),
    THROW_SNOWBALL_AT_PARTNER("Lempar Snowball", "Lempar {amount} snowball ke partner", "ProjectileHitEvent"),
    THROW_EGG("Lempar Telur", "Lempar {amount} telur", "ProjectileLaunchEvent"),
    EAT_CAKE("Makan Kue", "Makan kue {amount}x bersama partner", "PlayerInteractEvent"),
    DRINK_MILK("Minum Susu", "Minum {amount} susu", "PlayerItemConsumeEvent"),
    LAUNCH_FIREWORK("Kembang Api", "Luncurkan {amount} kembang api", "PlayerInteractEvent"),
    KILL_MOBS("Bunuh Monster", "Bunuh {amount} {target} bersama partner", "EntityDeathEvent"),
    CRAFT_ITEM("Craft Item", "Craft {amount} {target}", "CraftItemEvent"),
    PLACE_BLOCKS("Taruh Blocks", "Taruh {amount} blocks", "BlockPlaceEvent"),
    HARVEST_CROPS("Panen Tanaman", "Panen {amount} {target}", "BlockBreakEvent"),
    TAME_ANIMAL("Tame Hewan", "Tame {amount} hewan", "EntityTameEvent"),
    BREED_ANIMAL("Breed Hewan", "Breed {amount} hewan", "EntityBreedEvent"),
    SMELT_ITEMS("Smelt Items", "Smelt {amount} item di furnace", "FurnaceExtractEvent"),
    SHOOT_ARROWS("Tembak Panah", "Tembak {amount} panah", "ProjectileLaunchEvent"),
    SHEAR_SHEEP("Cukur Domba", "Cukur {amount} domba", "PlayerShearEntityEvent"),
    USE_ENDER_PEARL("Teleport Pearl", "Gunakan {amount} ender pearl", "PlayerTeleportEvent"),
    KILL_WITH_BOW("Bunuh dengan Bow", "Bunuh {amount} monster dengan bow", "EntityDeathEvent"),
    DAMAGE_EACH_OTHER("Sparring Partner", "Saling serang partner {amount}x", "EntityDamageByEntityEvent"),
    VISIT_NETHER("Kunjungi Nether", "Pergi ke Nether bersama partner", "PlayerChangedWorldEvent"),
    RIDE_TOGETHER("Naik Kendaraan", "Naik boat/minecart {amount}x bersama partner", "VehicleEnterEvent"),
    BREAK_BLOCKS("Hancurkan Blocks", "Hancurkan {amount} blocks bersama partner", "BlockBreakEvent"),
    PLAY_TOGETHER("Main Bersama", "Main bersama partner selama {amount} menit", "Scheduler"),
    KILL_BOSS("Bunuh Boss", "Bunuh {amount} {target}", "EntityDeathEvent"),
    MINE_ANCIENT_DEBRIS("Mining Ancient Debris", "Tambang {amount} Ancient Debris bersama partner", "BlockBreakEvent"),
    COMPLETE_RAID("Selesaikan Raid", "Selesaikan {amount} raid bersama partner", "RaidFinishEvent"),
    EARN_XP_LEVELS("Naik Level", "Naik {amount} level XP", "PlayerLevelChangeEvent"),
    MINE_DIAMOND_ORE("Tambang Diamond Ore", "Tambang {amount} diamond ore", "BlockBreakEvent"),
    MINE_DEEPSLATE_ORES("Tambang Deepslate Ore", "Tambang {amount} deepslate ore", "BlockBreakEvent"),
    KILL_WITHER_SKELETONS("Bunuh Wither Skeleton", "Bunuh {amount} wither skeleton bersama partner", "EntityDeathEvent"),
    SMELT_NETHERITE("Smelt Netherite", "Smelt {amount} netherite scrap dari furnace", "FurnaceExtractEvent");
    
    private final String displayName;
    private final String descriptionTemplate;
    private final String eventType;
    
    QuestType(String displayName, String descriptionTemplate, String eventType) {
        this.displayName = displayName;
        this.descriptionTemplate = descriptionTemplate;
        this.eventType = eventType;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescriptionTemplate() {
        return descriptionTemplate;
    }
    
    public String getEventType() {
        return eventType;
    }
    
        public String formatDescription(String target, int amount) {
        return descriptionTemplate
            .replace("{target}", target != null ? target : "")
            .replace("{amount}", String.valueOf(amount));
    }
    
        public boolean isHeavy() {
        return this == BREAK_BLOCKS || this == PLAY_TOGETHER || this == MINE_ANCIENT_DEBRIS;
    }
    
        public boolean requiresProximity() {
        return this == KILL_MOBS || this == BREAK_BLOCKS || this == SLEEP_TOGETHER 
            || this == KILL_BOSS || this == MINE_ANCIENT_DEBRIS || this == COMPLETE_RAID;
    }
    
        public boolean isBonusQuest() {
        return this == KILL_BOSS || this == MINE_ANCIENT_DEBRIS || this == COMPLETE_RAID;
    }
}

