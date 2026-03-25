package github.revanjay.partnershiprewards.listener;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.ActiveQuest;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.model.QuestType;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@RequiredArgsConstructor
public class QuestListener implements Listener {
    
    private final PartnershipRewards plugin;
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;
        
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.GIVE_ITEM)) return;
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null || !target.getUniqueId().equals(partnership.getPartner(playerUuid))) return;
        
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(playerUuid);
        if (quest == null || quest.getTarget() == null) return;
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) return;
        
        if (itemInHand.getType().name().equalsIgnoreCase(quest.getTarget())) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.GIVE_ITEM, 1);
            player.sendMessage("Â§a+1 progress untuk memberikan " + quest.getTarget() + "!");
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(AsyncPlayerChatEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.SEND_MESSAGE)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.SEND_MESSAGE, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.USE_COMMAND)) return;
        
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(playerUuid);
        if (quest == null || quest.getTarget() == null) return;
        
        String command = event.getMessage().split(" ")[0].toLowerCase();
        if (command.equalsIgnoreCase(quest.getTarget())) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.USE_COMMAND, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.EAT_FOOD)) return;
        
        Material type = event.getItem().getType();
        if (type.isEdible()) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.EAT_FOOD, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;
        
        UUID playerUuid = event.getPlayer().getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.SLEEP_TOGETHER)) return;
        
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null) return;
        Player partner = Bukkit.getPlayer(partnership.getPartner(playerUuid));
        if (partner != null && partner.isSleeping()) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.SLEEP_TOGETHER, 1);
            plugin.getQuestManager().updateQuestProgress(partner.getUniqueId(), QuestType.SLEEP_TOGETHER, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        UUID playerUuid = event.getPlayer().getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.FISH_CATCH)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.FISH_CATCH, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (event.getSlot() != 2) return; // Result slot
        if (event.getCurrentItem() == null) return;
        
        UUID playerUuid = player.getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.TRADE_VILLAGER)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.TRADE_VILLAGER, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        UUID playerUuid = event.getEnchanter().getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.ENCHANT_ITEM)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.ENCHANT_ITEM, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (event.getResult() == null) return;
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        
        UUID playerUuid = player.getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.ANVIL_USE)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.ANVIL_USE, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        
        Player killer = event.getEntity().getKiller();
        UUID playerUuid = killer.getUniqueId();
        if (plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.KILL_BOSS)) {
            handleKillBoss(event, killer, playerUuid);
            return;
        }
        if (plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.KILL_WITH_BOW)) {
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            if (weapon.getType() == Material.BOW || weapon.getType() == Material.CROSSBOW) {
                plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.KILL_WITH_BOW, 1);
            }
            return;
        }
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.KILL_MOBS)) return;
        
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(playerUuid);
        if (quest == null || quest.getTarget() == null) return;
        String entityType = event.getEntity().getType().name();
        if (!entityType.equalsIgnoreCase(quest.getTarget())) return;
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null) return;
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(playerUuid));
        if (partner == null) return;
        if (!killer.getWorld().equals(partner.getWorld())) return;
        
        double maxDistance = plugin.getConfig().getDouble("quest.types.KILL_MOBS.max-distance", 30);
        if (killer.getLocation().distance(partner.getLocation()) <= maxDistance) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.KILL_MOBS, 1);
        }
    }
    
        private void handleKillBoss(EntityDeathEvent event, Player killer, UUID playerUuid) {
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(playerUuid);
        if (quest == null || quest.getTarget() == null) return;
        String entityType = event.getEntity().getType().name();
        if (!entityType.equalsIgnoreCase(quest.getTarget())) return;
        EntityType type = event.getEntity().getType();
        if (type != EntityType.ENDER_DRAGON && type != EntityType.WITHER && type != EntityType.ELDER_GUARDIAN) {
            return;
        }
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null) return;
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(playerUuid));
        if (partner == null) return;
        
        if (!killer.getWorld().equals(partner.getWorld())) return;
        
        double maxDistance = 100; // Boss fights have larger area
        if (killer.getLocation().distance(partner.getLocation()) <= maxDistance) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.KILL_BOSS, 1);
            plugin.getQuestManager().updateQuestProgress(partner.getUniqueId(), QuestType.KILL_BOSS, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        UUID playerUuid = player.getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.CRAFT_ITEM)) return;
        
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(playerUuid);
        if (quest == null || quest.getTarget() == null) return;
        
        ItemStack result = event.getRecipe().getResult();
        if (result.getType().name().equalsIgnoreCase(quest.getTarget())) {
            int amount = result.getAmount();
            if (event.isShiftClick()) {
                amount = Math.min(64, amount * 9); // Estimate max craft amount
            }
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.CRAFT_ITEM, amount);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.PLACE_BLOCKS)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.PLACE_BLOCKS, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) return;
        
        UUID playerUuid = player.getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.TAME_ANIMAL)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.TAME_ANIMAL, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;
        
        UUID playerUuid = player.getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.BREED_ANIMAL)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.BREED_ANIMAL, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.SMELT_ITEMS)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.SMELT_ITEMS, event.getItemAmount());
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvestCrop(BlockBreakEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.HARVEST_CROPS)) return;
        
        Material cropType = event.getBlock().getType();
        if (!Tag.CROPS.isTagged(cropType) && 
            cropType != Material.WHEAT && 
            cropType != Material.CARROTS && 
            cropType != Material.POTATOES && 
            cropType != Material.BEETROOTS) return;
        if (event.getBlock().getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge()) return; // Not mature
        }
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.HARVEST_CROPS, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        Material blockType = event.getBlock().getType();
        if (blockType == Material.ANCIENT_DEBRIS && 
            plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.MINE_ANCIENT_DEBRIS)) {
            handleMineAncientDebris(event.getPlayer(), playerUuid);
            return;
        }
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.BREAK_BLOCKS)) return;
        
        Player player = event.getPlayer();
        int y = event.getBlock().getY();
        org.bukkit.World.Environment env = player.getWorld().getEnvironment();
        int maxY;
        switch (env) {
            case NETHER -> maxY = 128;      // Full nether height
            case THE_END -> maxY = 128;      // Full end height
            default -> maxY = 128;           // Full overworld, reduced from 64
        }
        
        if (y > maxY) return; // Only skip extreme heights
        String blockName = blockType.name();
        boolean isNaturalBlock = 
            blockType == Material.STONE ||
            blockType == Material.GRANITE ||
            blockType == Material.DIORITE ||
            blockType == Material.ANDESITE ||
            blockType == Material.TUFF ||
            blockType == Material.CALCITE ||
            blockType == Material.DRIPSTONE_BLOCK ||
            blockName.startsWith("DEEPSLATE") ||
            blockName.startsWith("COBBLED_DEEPSLATE") ||
            blockName.endsWith("_ORE") ||
            blockType == Material.RAW_IRON_BLOCK ||
            blockType == Material.RAW_COPPER_BLOCK ||
            blockType == Material.RAW_GOLD_BLOCK ||
            blockType == Material.NETHERRACK ||
            blockType == Material.BASALT ||
            blockType == Material.BLACKSTONE ||
            blockName.startsWith("NETHER") ||
            blockType == Material.END_STONE ||
            blockType == Material.DIRT ||
            blockType == Material.COARSE_DIRT ||
            blockType == Material.ROOTED_DIRT ||
            blockType == Material.GRAVEL ||
            blockType == Material.SAND ||
            blockType == Material.RED_SAND ||
            blockType == Material.CLAY ||
            blockType == Material.TERRACOTTA ||
            blockName.endsWith("_TERRACOTTA") ||
            blockType == Material.SANDSTONE ||
            blockType == Material.RED_SANDSTONE ||
            blockType == Material.COBBLESTONE ||
            blockType == Material.MOSSY_COBBLESTONE;
        
        if (!isNaturalBlock) return;
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null) return;
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(playerUuid));
        if (partner == null) return;
        if (!player.getWorld().equals(partner.getWorld())) return;
        
        double maxDistance = plugin.getConfig().getDouble("quest.types.BREAK_BLOCKS.max-distance", 30);
        if (player.getLocation().distance(partner.getLocation()) <= maxDistance) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.BREAK_BLOCKS, 1);
        }
    }
    
        private void handleMineAncientDebris(Player player, UUID playerUuid) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null) return;
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(playerUuid));
        if (partner == null) return;
        
        if (!player.getWorld().equals(partner.getWorld())) return;
        
        double maxDistance = 50; // Nether mining area
        if (player.getLocation().distance(partner.getLocation()) <= maxDistance) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.MINE_ANCIENT_DEBRIS, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidFinish(RaidFinishEvent event) {
        for (Player player : event.getWinners()) {
            UUID playerUuid = player.getUniqueId();
            
            if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.COMPLETE_RAID)) continue;
            Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
            if (partnership == null) continue;
            
            for (Player winner : event.getWinners()) {
                if (winner.getUniqueId().equals(partnership.getPartner(playerUuid))) {
                    plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.COMPLETE_RAID, 1);
                    break;
                }
            }
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMineAncientDebris(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.ANCIENT_DEBRIS) return;
        
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.MINE_ANCIENT_DEBRIS)) return;
        if (player.getWorld().getEnvironment() != org.bukkit.World.Environment.NETHER) return;
        if (event.getBlock().getY() >= 22) return;
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null) return;
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(playerUuid));
        if (partner == null || !player.getWorld().equals(partner.getWorld())) return;
        
        double maxDistance = 50;
        if (player.getLocation().distance(partner.getLocation()) <= maxDistance) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.MINE_ANCIENT_DEBRIS, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Arrow)) return;
        
        UUID playerUuid = player.getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.SHOOT_ARROWS)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.SHOOT_ARROWS, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        if (!(event.getEntity() instanceof Sheep)) return;
        
        UUID playerUuid = event.getPlayer().getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.SHEAR_SHEEP)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.SHEAR_SHEEP, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnderPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        
        UUID playerUuid = event.getPlayer().getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.USE_ENDER_PEARL)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.USE_ENDER_PEARL, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPartnerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        
        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player) {
            attacker = (Player) projectile.getShooter();
        }
        
        if (attacker == null) return;
        
        UUID attackerUuid = attacker.getUniqueId();
        UUID victimUuid = victim.getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(attackerUuid, QuestType.DAMAGE_EACH_OTHER)) return;
        Partnership partnership = plugin.getPartnershipManager().getPartnership(attackerUuid);
        if (partnership == null) return;
        
        if (victimUuid.equals(partnership.getPartner(attackerUuid))) {
            plugin.getQuestManager().updateQuestProgress(attackerUuid, QuestType.DAMAGE_EACH_OTHER, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!event.getPlayer().getWorld().getEnvironment().name().contains("NETHER")) return;
        
        UUID playerUuid = event.getPlayer().getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.VISIT_NETHER)) return;
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null) return;
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(playerUuid));
        if (partner != null && partner.getWorld().getEnvironment().name().contains("NETHER")) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.VISIT_NETHER, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;
        
        UUID playerUuid = player.getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.RIDE_TOGETHER)) return;
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null) return;
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(playerUuid));
        if (partner == null || !player.getWorld().equals(partner.getWorld())) return;
        if (player.getLocation().distance(partner.getLocation()) <= 10) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.RIDE_TOGETHER, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSnowballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        if (!(event.getEntity().getShooter() instanceof Player thrower)) return;
        if (!(event.getHitEntity() instanceof Player hit)) return;
        
        UUID throwerUuid = thrower.getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(throwerUuid, QuestType.THROW_SNOWBALL_AT_PARTNER)) return;
        Partnership partnership = plugin.getPartnershipManager().getPartnership(throwerUuid);
        if (partnership == null) return;
        
        if (hit.getUniqueId().equals(partnership.getPartner(throwerUuid))) {
            plugin.getQuestManager().updateQuestProgress(throwerUuid, QuestType.THROW_SNOWBALL_AT_PARTNER, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEggThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Egg)) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        
        UUID playerUuid = player.getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.THROW_EGG)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.THROW_EGG, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEatCake(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.CAKE) return;
        
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.EAT_CAKE)) return;
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null) return;
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(playerUuid));
        if (partner == null || !player.getWorld().equals(partner.getWorld())) return;
        
        if (player.getLocation().distance(partner.getLocation()) <= 10) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.EAT_CAKE, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrinkMilk(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.MILK_BUCKET) return;
        
        UUID playerUuid = event.getPlayer().getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.DRINK_MILK)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.DRINK_MILK, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFireworkLaunch(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && 
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null) return;
        if (event.getItem().getType() != Material.FIREWORK_ROCKET) return;
        
        UUID playerUuid = event.getPlayer().getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.LAUNCH_FIREWORK)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.LAUNCH_FIREWORK, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelChange(PlayerLevelChangeEvent event) {
        if (event.getNewLevel() <= event.getOldLevel()) return; // Must be level up, not down
        
        UUID playerUuid = event.getPlayer().getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.EARN_XP_LEVELS)) return;
        
        int levelsGained = event.getNewLevel() - event.getOldLevel();
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.EARN_XP_LEVELS, levelsGained);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMineDeepslateOre(BlockBreakEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.MINE_DEEPSLATE_ORES)) return;
        
        String blockName = event.getBlock().getType().name();
        if (!blockName.startsWith("DEEPSLATE_") || !blockName.endsWith("_ORE")) return;
        if (event.getBlock().getY() >= 0) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.MINE_DEEPSLATE_ORES, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKillWitherSkeleton(EntityDeathEvent event) {
        if (event.getEntity().getType() != EntityType.WITHER_SKELETON) return;
        if (event.getEntity().getKiller() == null) return;
        
        Player killer = event.getEntity().getKiller();
        UUID playerUuid = killer.getUniqueId();
        
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.KILL_WITHER_SKELETONS)) return;
        Partnership partnership = plugin.getPartnershipManager().getPartnership(playerUuid);
        if (partnership == null) return;
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(playerUuid));
        if (partner == null || !killer.getWorld().equals(partner.getWorld())) return;
        
        double maxDistance = 50; // Nether fortress area
        if (killer.getLocation().distance(partner.getLocation()) <= maxDistance) {
            plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.KILL_WITHER_SKELETONS, 1);
        }
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMineDiamondOre(BlockBreakEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.MINE_DIAMOND_ORE)) return;
        
        Material blockType = event.getBlock().getType();
        int y = event.getBlock().getY();
        if (blockType == Material.DIAMOND_ORE && y >= 16) return;  // Normal diamond ore Y < 16
        if (blockType == Material.DEEPSLATE_DIAMOND_ORE && y >= 0) return;  // Deepslate diamond ore Y < 0
        if (blockType != Material.DIAMOND_ORE && blockType != Material.DEEPSLATE_DIAMOND_ORE) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.MINE_DIAMOND_ORE, 1);
    }
    
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmeltNetherite(FurnaceExtractEvent event) {
        if (event.getItemType() != Material.NETHERITE_SCRAP) return;
        
        UUID playerUuid = event.getPlayer().getUniqueId();
        if (!plugin.getQuestManager().hasActiveQuest(playerUuid, QuestType.SMELT_NETHERITE)) return;
        
        plugin.getQuestManager().updateQuestProgress(playerUuid, QuestType.SMELT_NETHERITE, event.getItemAmount());
    }
    
    // PLAY_TOGETHER is handled by PlayTogetherTask, not an event listener
}

