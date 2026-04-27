package github.revanjay.partnershiprewards.model;

public enum QuestType {
    GIVE_ITEM("Give Item", "Give {target} to your partner", "PlayerInteractEntityEvent"),
    SEND_MESSAGE("Send Messages", "Send {amount} messages in chat", "AsyncPlayerChatEvent"),
    USE_COMMAND("Use Commands", "Use command {target} {amount} times", "PlayerCommandPreprocessEvent"),
    EAT_FOOD("Eat Together", "Eat {amount} food items", "PlayerItemConsumeEvent"),
    SLEEP_TOGETHER("Sleep Together", "Sleep with your partner {amount} times", "PlayerBedEnterEvent"),
    FISH_CATCH("Go Fishing", "Catch {amount} fish", "PlayerFishEvent"),
    TRADE_VILLAGER("Trade Villager", "Trade with villagers {amount} times", "InventoryClickEvent"),
    ENCHANT_ITEM("Enchant Items", "Enchant {amount} items", "EnchantItemEvent"),
    ANVIL_USE("Use Anvil", "Use an anvil {amount} times", "PrepareAnvilEvent"),
    BREW_POTION("Brew Potions", "Brew {amount} potions", "BrewEvent"),
    THROW_SNOWBALL_AT_PARTNER("Throw Snowballs", "Throw {amount} snowballs at your partner", "ProjectileHitEvent"),
    THROW_EGG("Throw Eggs", "Throw {amount} eggs", "ProjectileLaunchEvent"),
    EAT_CAKE("Eat Cake", "Eat cake {amount} times with your partner", "PlayerInteractEvent"),
    DRINK_MILK("Drink Milk", "Drink {amount} milk buckets", "PlayerItemConsumeEvent"),
    LAUNCH_FIREWORK("Launch Fireworks", "Launch {amount} fireworks", "PlayerInteractEvent"),
    KILL_MOBS("Kill Mobs", "Kill {amount} {target} with your partner", "EntityDeathEvent"),
    CRAFT_ITEM("Craft Items", "Craft {amount} {target}", "CraftItemEvent"),
    PLACE_BLOCKS("Place Blocks", "Place {amount} blocks", "BlockPlaceEvent"),
    HARVEST_CROPS("Harvest Crops", "Harvest {amount} {target}", "BlockBreakEvent"),
    TAME_ANIMAL("Tame Animals", "Tame {amount} animals", "EntityTameEvent"),
    BREED_ANIMAL("Breed Animals", "Breed {amount} animals", "EntityBreedEvent"),
    SMELT_ITEMS("Smelt Items", "Smelt {amount} items in a furnace", "FurnaceExtractEvent"),
    SHOOT_ARROWS("Shoot Arrows", "Shoot {amount} arrows", "ProjectileLaunchEvent"),
    SHEAR_SHEEP("Shear Sheep", "Shear {amount} sheep", "PlayerShearEntityEvent"),
    USE_ENDER_PEARL("Ender Pearl", "Use {amount} ender pearls", "PlayerTeleportEvent"),
    KILL_WITH_BOW("Kill with Bow", "Kill {amount} mobs with a bow", "EntityDeathEvent"),
    DAMAGE_EACH_OTHER("Partner Sparring", "Hit your partner {amount} times", "EntityDamageByEntityEvent"),
    VISIT_NETHER("Visit Nether", "Go to the Nether with your partner", "PlayerChangedWorldEvent"),
    RIDE_TOGETHER("Ride Together", "Ride boat/minecart {amount} times with your partner", "VehicleEnterEvent"),
    BREAK_BLOCKS("Break Blocks", "Break {amount} blocks with your partner", "BlockBreakEvent"),
    PLAY_TOGETHER("Play Together", "Play together with your partner for {amount} minutes", "Scheduler"),
    KILL_BOSS("Kill Boss", "Kill {amount} {target}", "EntityDeathEvent"),
    MINE_ANCIENT_DEBRIS("Mine Ancient Debris", "Mine {amount} ancient debris with your partner", "BlockBreakEvent"),
    COMPLETE_RAID("Complete Raids", "Complete {amount} raids with your partner", "RaidFinishEvent"),
    EARN_XP_LEVELS("Earn XP Levels", "Earn {amount} XP levels", "PlayerLevelChangeEvent"),
    MINE_DIAMOND_ORE("Mine Diamond Ore", "Mine {amount} diamond ore", "BlockBreakEvent"),
    MINE_DEEPSLATE_ORES("Mine Deepslate Ore", "Mine {amount} deepslate ore", "BlockBreakEvent"),
    KILL_WITHER_SKELETONS("Kill Wither Skeletons", "Kill {amount} wither skeletons with your partner", "EntityDeathEvent"),
    SMELT_NETHERITE("Smelt Netherite", "Smelt {amount} netherite scrap in a furnace", "FurnaceExtractEvent");
    
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

