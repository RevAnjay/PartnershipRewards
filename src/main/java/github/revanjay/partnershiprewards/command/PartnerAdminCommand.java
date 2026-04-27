package github.revanjay.partnershiprewards.command;

import github.revanjay.partnershiprewards.PartnershipRewards;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;
import static github.revanjay.partnershiprewards.PartnershipRewards.playErrorSound;

import java.util.*;

public class PartnerAdminCommand implements CommandExecutor, TabCompleter {
    
    private final PartnershipRewards plugin;
    
    public PartnerAdminCommand(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("partnershiprewards.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission!"));
            if (sender instanceof Player p) playErrorSound(p);
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender, args);
            case "set" -> handleSet(sender, args);
            case "toggle" -> handleToggle(sender, args);
            default -> sendHelp(sender);
        }
        
        return true;
    }
    
    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(getMsg("config-reloaded"));
    }
    
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /partneradmin reset <player>"));
            if (sender instanceof Player p) playErrorSound(p);
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            UUID targetUUID = target.getUniqueId();
            
            github.revanjay.partnershiprewards.model.Partnership partnership = plugin.getDatabaseManager().getPartnership(targetUUID);
            if (partnership == null) {
                String targetName = target.getName() != null ? target.getName() : args[1];
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(colorize("&c" + targetName + " doesn't have a partner!"));
                    if (sender instanceof Player p) playErrorSound(p);
                });
                return;
            }
            
            UUID partnerUUID = partnership.getPartner(targetUUID);
            String partnerName = partnerUUID != null ? Bukkit.getOfflinePlayer(partnerUUID).getName() : "unknown";
            if (partnerName == null) partnerName = "unknown";
            
            final String fPartnerName = partnerName;
            final String targetName = target.getName() != null ? target.getName() : args[1];
            
            plugin.getPartnershipManager().breakPartnershipDB(targetUUID);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(colorize("&aPartnership between &e" + targetName + " &aand &e" + fPartnerName + " &ahas been reset!"));
            });
        });
    }
    
    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(colorize("&cUsage: /partneradmin set <player1> <player2>"));
            if (sender instanceof Player p) playErrorSound(p);
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer player1 = Bukkit.getOfflinePlayer(args[1]);
            @SuppressWarnings("deprecation")
            OfflinePlayer player2 = Bukkit.getOfflinePlayer(args[2]);
            UUID uuid1 = player1.getUniqueId();
            UUID uuid2 = player2.getUniqueId();
            
            github.revanjay.partnershiprewards.model.Partnership p1 = plugin.getDatabaseManager().getPartnership(uuid1);
            if (p1 != null) {
                String name1 = player1.getName() != null ? player1.getName() : args[1];
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(colorize("&c" + name1 + " already has a partner!"));
                    if (sender instanceof Player p) playErrorSound(p);
                });
                return;
            }
            
            github.revanjay.partnershiprewards.model.Partnership p2 = plugin.getDatabaseManager().getPartnership(uuid2);
            if (p2 != null) {
                String name2 = player2.getName() != null ? player2.getName() : args[2];
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(colorize("&c" + name2 + " already has a partner!"));
                    if (sender instanceof Player p) playErrorSound(p);
                });
                return;
            }
            
            String name1 = player1.getName() != null ? player1.getName() : args[1];
            String name2 = player2.getName() != null ? player2.getName() : args[2];
            
            plugin.getPartnershipManager().createPartnership(uuid1, uuid2);
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize("&aPartnership between &e" + name1 + " &aand &e" + name2 + " &ahas been created!")));
        });
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&d&l=== &ePartner Admin Commands &d&l==="));
        sender.sendMessage(colorize("&e/partneradmin reload &7- Reload config"));
        sender.sendMessage(colorize("&e/partneradmin reset <player> &7- Reset player's partnership"));
        sender.sendMessage(colorize("&e/partneradmin set <player1> <player2> &7- Force create partnership"));
        sender.sendMessage(colorize("&e/partneradmin toggle spy &7- Toggle spy mode for partner chat"));
    }
    
    private void handleToggle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cThis command can only be used by players!"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /partneradmin toggle <spy>"));
            playErrorSound(player);
            return;
        }
        
        if (args[1].equalsIgnoreCase("spy")) {
            plugin.getPartnerListener().toggleSpy(player.getUniqueId());
            boolean isSpying = plugin.getPartnerListener().isSpying(player.getUniqueId());
            
            String status = isSpying ? colorize("&a&lENABLED") : colorize("&c&lDISABLED");
            player.sendMessage(colorize("&7Spy mode: ") + status);
            
            if (isSpying) {
                player.sendMessage(colorize("&7You can now see all partner chat messages."));
            }
        } else {
            sender.sendMessage(colorize("&cUnknown toggle option: &e" + args[1]));
            playErrorSound(player);
        }
    }
    
    private String getMsg(String key) {
        return colorize(plugin.getConfig().getString("messages." + key, ""));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "reset", "set", "toggle");
        }
        
        if (args.length == 2 || args.length == 3) {
            if (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("set")) {
                return null;
            }
        }
        
        return Collections.emptyList();
    }
}
