package github.revanjay.partnershiprewards;

import github.revanjay.partnershiprewards.command.PartnerAdminCommand;
import github.revanjay.partnershiprewards.command.PartnerCommand;
import github.revanjay.partnershiprewards.database.DatabaseManager;
import github.revanjay.partnershiprewards.hook.PartnerPlaceholderExpansion;
import github.revanjay.partnershiprewards.listener.PartnerListener;
import github.revanjay.partnershiprewards.listener.PlayerListener;
import github.revanjay.partnershiprewards.listener.QuestListener;
import github.revanjay.partnershiprewards.manager.*;
import github.revanjay.partnershiprewards.task.PlayTogetherTask;
import lombok.Getter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class PartnershipRewards extends JavaPlugin {
    
    private DatabaseManager databaseManager;
    private PartnershipManager partnershipManager;
    private RewardManager rewardManager;
    private RequestManager requestManager;
    private QuestManager questManager;
    private GiftManager giftManager;
    private ChatManager chatManager;
    private EffectManager effectManager;
    private StreakManager streakManager;
    
    private PartnerListener partnerListener;
    private PlayTogetherTask playTogetherTask;
    @Getter
    private static double globalXpMultiplier = 1.0;

    public static void setGlobalXpMultiplier(double multiplier) {
        globalXpMultiplier = multiplier;
    }
    
    public static String colorize(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
    
    public static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(colorize(message)));
    }
    
    public static void playErrorSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        initializeManagers();
        registerCommands();
        registerListeners();
        startTasks();
        registerHooks();
        
        getLogger().info("PartnershipRewards v" + getDescription().getVersion() + " has been enabled!");
    }
    
    @Override
    public void onDisable() {
        if (questManager != null) {
            questManager.shutdown();
        }
        
        if (rewardManager != null) {
            rewardManager.shutdown();
        }
        
        if (effectManager != null) {
            effectManager.shutdown();
        }
        
        if (playTogetherTask != null) {
            playTogetherTask.cancel();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("PartnershipRewards has been disabled!");
    }
    
    private void initializeManagers() {
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();
        
        this.partnershipManager = new PartnershipManager(this);
        this.requestManager = new RequestManager(this);
        this.rewardManager = new RewardManager(this);
        this.questManager = new QuestManager(this);
        this.giftManager = new GiftManager(this);
        this.chatManager = new ChatManager(this);
        this.effectManager = new EffectManager(this);
        this.streakManager = new StreakManager(this);
        
        this.rewardManager.startRewardTask();
    }
    
    private void registerCommands() {
        getCommand("partner").setExecutor(new PartnerCommand(this));
        getCommand("partneradmin").setExecutor(new PartnerAdminCommand(this));
    }
    
    private void registerListeners() {
        this.partnerListener = new PartnerListener(this);
        getServer().getPluginManager().registerEvents(partnerListener, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new QuestListener(this), this);
    }
    
    private void startTasks() {
        this.playTogetherTask = new PlayTogetherTask(this);
        this.playTogetherTask.start();
        this.effectManager.start();
    }
    
    private void registerHooks() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PartnerPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI hook registered! Use %partner_<placeholder>%");
        }
    }
    
    public void reload() {
        reloadConfig();
        
        if (questManager != null) {
            questManager.shutdown();
        }
        
        if (rewardManager != null) {
            rewardManager.shutdown();
        }
        
        if (effectManager != null) {
            effectManager.shutdown();
        }
        
        this.rewardManager = new RewardManager(this);
        this.rewardManager.startRewardTask();
        
        this.questManager = new QuestManager(this);
        this.giftManager = new GiftManager(this);
        this.effectManager = new EffectManager(this);
        this.effectManager.start();
        this.streakManager = new StreakManager(this);
    }
}

