package github.revanjay.partnershiprewards;

import github.revanjay.partnershiprewards.command.PartnerAdminCommand;
import github.revanjay.partnershiprewards.command.PartnerCommand;
import github.revanjay.partnershiprewards.database.DatabaseManager;
import github.revanjay.partnershiprewards.listener.PartnerListener;
import github.revanjay.partnershiprewards.listener.PlayerListener;
import github.revanjay.partnershiprewards.listener.QuestListener;
import github.revanjay.partnershiprewards.manager.PartnershipManager;
import github.revanjay.partnershiprewards.manager.QuestManager;
import github.revanjay.partnershiprewards.manager.RewardManager;
import github.revanjay.partnershiprewards.manager.RequestManager;
import github.revanjay.partnershiprewards.task.PlayTogetherTask;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class PartnershipRewards extends JavaPlugin {
    
    private DatabaseManager databaseManager;
    private PartnershipManager partnershipManager;
    private RewardManager rewardManager;
    private RequestManager requestManager;
    private QuestManager questManager;
    
    private PartnerListener partnerListener;
    private PlayTogetherTask playTogetherTask;
    @Getter
    private static double globalXpMultiplier = 1.0;

    public static void setGlobalXpMultiplier(double multiplier) {
        globalXpMultiplier = multiplier;
    }
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        initializeManagers();
        registerCommands();
        registerListeners();
        startTasks();
        
        getLogger().info("PartnershipRewards telah diaktifkan!");
    }
    
    @Override
    public void onDisable() {
        if (questManager != null) {
            questManager.shutdown();
        }
        
        if (rewardManager != null) {
            rewardManager.shutdown();
        }
        
        if (playTogetherTask != null) {
            playTogetherTask.cancel();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("PartnershipRewards telah dinonaktifkan!");
    }
    
    private void initializeManagers() {
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();
        
        this.partnershipManager = new PartnershipManager(this);
        this.requestManager = new RequestManager(this);
        this.rewardManager = new RewardManager(this);
        this.questManager = new QuestManager(this);
        
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
    }
    
    public void reload() {
        reloadConfig();
        
        if (questManager != null) {
            questManager.shutdown();
        }
        
        if (rewardManager != null) {
            rewardManager.shutdown();
        }
        
        this.rewardManager = new RewardManager(this);
        this.rewardManager.startRewardTask();
        
        this.questManager = new QuestManager(this);
    }
}
