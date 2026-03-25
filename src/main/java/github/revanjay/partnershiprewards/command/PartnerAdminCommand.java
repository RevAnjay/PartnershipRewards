package github.revanjay.partnershiprewards.command;

import github.revanjay.partnershiprewards.PartnershipRewards;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class PartnerAdminCommand implements CommandExecutor, TabCompleter {
    
    private final PartnershipRewards plugin;
    
    public PartnerAdminCommand(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("partnershiprewards.admin")) {
            sender.sendMessage(getMsg("prefix") + "Â§cKamu tidak memiliki permission!");
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
        sender.sendMessage(getMsg("prefix") + getMsg("config-reloaded"));
    }
    
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getMsg("prefix") + "Â§cGunakan: /partneradmin reset <player>");
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            UUID targetUUID = target.getUniqueId();
            
            github.revanjay.partnershiprewards.model.Partnership partnership = plugin.getDatabaseManager().getPartnership(targetUUID);
            if (partnership == null) {
                String targetName = target.getName() != null ? target.getName() : args[1];
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(getMsg("prefix") + "Â§c" + targetName + " tidak memiliki partner!"));
                return;
            }
            
            UUID partnerUUID = partnership.getPartner(targetUUID);
            String partnerName = partnerUUID != null ? Bukkit.getOfflinePlayer(partnerUUID).getName() : "unknown";
            if (partnerName == null) partnerName = "unknown";
            
            final String fPartnerName = partnerName;
            final String targetName = target.getName() != null ? target.getName() : args[1];
            
            plugin.getPartnershipManager().breakPartnershipDB(targetUUID);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(getMsg("prefix") + "Â§aPartnership antara Â§e" + targetName + " Â§adan Â§e" + fPartnerName + " Â§atelah direset!");
            });
        });
    }
    
    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(getMsg("prefix") + "Â§cGunakan: /partneradmin set <player1> <player2>");
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
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(getMsg("prefix") + "Â§c" + name1 + " sudah memiliki partner!"));
                return;
            }
            
            github.revanjay.partnershiprewards.model.Partnership p2 = plugin.getDatabaseManager().getPartnership(uuid2);
            if (p2 != null) {
                String name2 = player2.getName() != null ? player2.getName() : args[2];
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(getMsg("prefix") + "Â§c" + name2 + " sudah memiliki partner!"));
                return;
            }
            
            String name1 = player1.getName() != null ? player1.getName() : args[1];
            String name2 = player2.getName() != null ? player2.getName() : args[2];
            
            plugin.getPartnershipManager().createPartnership(uuid1, uuid2);
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(getMsg("prefix") + "Â§aPartnership antara Â§e" + name1 + " Â§adan Â§e" + name2 + " Â§atelah dibuat!"));
        });
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("Â§dÂ§l=== Partner Admin Commands ===");
        sender.sendMessage("Â§e/partneradmin reload Â§7- Reload config");
        sender.sendMessage("Â§e/partneradmin reset <player> Â§7- Reset partnership player");
        sender.sendMessage("Â§e/partneradmin set <player1> <player2> Â§7- Buat partnership paksa");
        sender.sendMessage("Â§e/partneradmin toggle spy Â§7- Toggle spy mode untuk partner chat");
    }
    
    private void handleToggle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Â§cCommand ini hanya bisa digunakan oleh player!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(getMsg("prefix") + "Â§cGunakan: /partneradmin toggle <spy>");
            return;
        }
        
        if (args[1].equalsIgnoreCase("spy")) {
            plugin.getPartnerListener().toggleSpy(player.getUniqueId());
            boolean isSpying = plugin.getPartnerListener().isSpying(player.getUniqueId());
            
            String status = isSpying ? "Â§aÂ§lAKTIF" : "Â§cÂ§lNONAKTIF";
            player.sendMessage(getMsg("prefix") + "Â§7Spy mode: " + status);
            
            if (isSpying) {
                player.sendMessage("Â§7Kamu sekarang bisa melihat semua chat partner.");
            }
        } else {
            sender.sendMessage(getMsg("prefix") + "Â§cOpsi toggle tidak dikenal: Â§e" + args[1]);
        }
    }
    
    private String getMsg(String key) {
        return plugin.getConfig().getString("messages." + key, "").replace("&", "Â§");
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
