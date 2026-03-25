package github.revanjay.partnershiprewards.command;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.gui.LevelsGUI;
import github.revanjay.partnershiprewards.model.ActiveQuest;
import github.revanjay.partnershiprewards.model.PartnerRequest;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class PartnerCommand implements CommandExecutor, TabCompleter {
    
    private final PartnershipRewards plugin;
    
    public PartnerCommand(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Â§cCommand ini hanya bisa digunakan oleh player!");
            return true;
        }
        
        if (!player.hasPermission("partnershiprewards.use")) {
            player.sendMessage(getMsg("prefix") + "Â§cKamu tidak memiliki permission!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "request" -> handleRequest(player, args);
            case "accept" -> handleAccept(player, args);
            case "reject" -> handleReject(player);
            case "break" -> handleBreak(player);
            case "info" -> handleInfo(player);
            case "quest" -> handleQuest(player);
            case "level", "gui" -> handleLevelGUI(player);
            case "top" -> handleTop(player);
            case "list" -> handleList(player);
            case "chat" -> handleChat(player, args);
            case "toggle" -> handleToggle(player, args);
            default -> sendHelp(player);
        }
        
        return true;
    }
    
    private void handleRequest(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(getMsg("prefix") + "Â§cGunakan: /partner request <player>");
            return;
        }
        
        if (plugin.getRequestManager().isOnCooldown(player.getUniqueId())) {
            long remaining = plugin.getRequestManager().getRemainingCooldown(player.getUniqueId());
            player.sendMessage(getMsg("prefix") + getMsg("cooldown").replace("{seconds}", String.valueOf(remaining)));
            return;
        }
        
        if (plugin.getPartnershipManager().hasPartner(player.getUniqueId())) {
            player.sendMessage(getMsg("prefix") + getMsg("already-partnered"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(getMsg("prefix") + getMsg("player-not-found").replace("{player}", args[1]));
            return;
        }
        
        if (target.equals(player)) {
            if (!plugin.getConfig().getBoolean("partnership.allow-self-partner", false)) {
                player.sendMessage(getMsg("prefix") + getMsg("cannot-partner-self"));
                return;
            }
        }
        
        if (plugin.getPartnershipManager().hasPartner(target.getUniqueId())) {
            player.sendMessage(getMsg("prefix") + getMsg("target-has-partner").replace("{player}", target.getName()));
            return;
        }
        
        double maxDistance = plugin.getConfig().getDouble("partnership.max-distance", 10.0);
        if (!player.getWorld().equals(target.getWorld())) {
            player.sendMessage(getMsg("prefix") + getMsg("too-far").replace("{player}", target.getName()));
            return;
        }
        
        if (player.getLocation().distance(target.getLocation()) > maxDistance) {
            player.sendMessage(getMsg("prefix") + getMsg("too-far").replace("{player}", target.getName()));
            return;
        }
        
        plugin.getRequestManager().createRequest(player.getUniqueId(), target.getUniqueId());
        
        player.sendMessage(getMsg("prefix") + getMsg("request-sent").replace("{player}", target.getName()));
        target.sendMessage(getMsg("prefix") + getMsg("request-received").replace("{player}", player.getName()));
    }
    
    private void handleAccept(Player player, String[] args) {
        PartnerRequest request = plugin.getRequestManager().getRequest(player.getUniqueId());
        
        if (request == null) {
            String senderName = args.length > 1 ? args[1] : "player tersebut";
            player.sendMessage(getMsg("prefix") + getMsg("no-pending-request").replace("{player}", senderName));
            return;
        }
        
        if (plugin.getPartnershipManager().hasPartner(player.getUniqueId())) {
            player.sendMessage(getMsg("prefix") + getMsg("already-partnered"));
            plugin.getRequestManager().removeRequest(player.getUniqueId());
            return;
        }
        
        Player sender = Bukkit.getPlayer(request.getSender());
        
        if (plugin.getConfig().getBoolean("partnership.require-both-online", true) && sender == null) {
            player.sendMessage(getMsg("prefix") + getMsg("player-not-online").replace("{player}", Bukkit.getOfflinePlayer(request.getSender()).getName()));
            plugin.getRequestManager().removeRequest(player.getUniqueId());
            return;
        }
        
        plugin.getPartnershipManager().createPartnership(request.getSender(), request.getTarget());
        plugin.getRequestManager().removeRequest(player.getUniqueId());
        
        player.sendMessage(getMsg("prefix") + getMsg("partnership-formed").replace("{player}", Bukkit.getOfflinePlayer(request.getSender()).getName()));
        if (sender != null) {
            sender.sendMessage(getMsg("prefix") + getMsg("partnership-formed").replace("{player}", player.getName()));
        }
    }
    
    private void handleReject(Player player) {
        PartnerRequest request = plugin.getRequestManager().getRequest(player.getUniqueId());
        
        if (request == null) {
            player.sendMessage(getMsg("prefix") + "Â§cKamu tidak memiliki permintaan partnership yang pending!");
            return;
        }
        
        plugin.getRequestManager().removeRequest(player.getUniqueId());
        player.sendMessage(getMsg("prefix") + "Â§aKamu menolak permintaan partnership!");
        
        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender != null) {
            sender.sendMessage(getMsg("prefix") + "Â§c" + player.getName() + " menolak permintaan partnership kamu!");
        }
    }
    
    private void handleBreak(Player player) {
        if (!plugin.getPartnershipManager().hasPartner(player.getUniqueId())) {
            player.sendMessage(getMsg("prefix") + getMsg("no-partner"));
            return;
        }
        
        UUID partnerUUID = plugin.getPartnershipManager().getPartnerUUID(player.getUniqueId());
        String partnerName = Bukkit.getOfflinePlayer(partnerUUID).getName();
        
        plugin.getPartnershipManager().breakPartnership(player.getUniqueId());
        
        player.sendMessage(getMsg("prefix") + getMsg("partnership-broken").replace("{player}", partnerName));
        
        Player partner = Bukkit.getPlayer(partnerUUID);
        if (partner != null) {
            partner.sendMessage(getMsg("prefix") + getMsg("partnership-broken").replace("{player}", player.getName()));
        }
    }
    
    private void handleInfo(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        
        if (partnership == null) {
            player.sendMessage(getMsg("prefix") + getMsg("no-partner"));
            return;
        }
        
        UUID partnerUUID = partnership.getPartner(player.getUniqueId());
        String partnerName = Bukkit.getOfflinePlayer(partnerUUID).getName();
        
        long durationSeconds = partnership.getDurationInSeconds();
        String duration = formatDuration(durationSeconds);
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String startDate = sdf.format(new Date(partnership.getStartedAt() * 1000));
        player.sendMessage("Â§dÂ§l=== Partnership Info ===");
        player.sendMessage("Â§7Partner: Â§e" + partnerName);
        player.sendMessage("Â§7Durasi: Â§a" + duration);
        player.sendMessage("Â§7Dimulai: Â§a" + startDate);
        int level = partnership.getLevel();
        int xp = partnership.getXp();
        int requiredXp = plugin.getQuestManager().getRequiredXpForLevel(level + 1);
        int xpPercentage = requiredXp > 0 ? (xp * 100) / requiredXp : 100;
        
        player.sendMessage("Â§7Level: Â§b" + level + " Â§7| XP: Â§b" + xp + "/" + requiredXp + " Â§7(" + xpPercentage + "%)");
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(player.getUniqueId());
        if (quest != null) {
            player.sendMessage("Â§7Quest: Â§e" + quest.getFormattedDescription());
            player.sendMessage("Â§7Progress: " + quest.getProgressBar() + " Â§7(" + quest.getProgress() + "/" + quest.getRequiredAmount() + ")");
        } else {
            player.sendMessage("Â§7Quest: Â§cTidak ada quest aktif");
        }
    }
    
    private void handleQuest(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        
        if (partnership == null) {
            player.sendMessage(getMsg("prefix") + getMsg("no-partner"));
            return;
        }
        
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(player.getUniqueId());
        
        if (quest == null) {
            if (plugin.getQuestManager().isOnQuestCooldown(partnership)) {
                long remaining = plugin.getQuestManager().getQuestCooldownRemaining(partnership);
                player.sendMessage(getMsg("prefix") + "Â§câ³ Cooldown aktif! Â§7Quest baru dalam Â§e" + remaining + " menitÂ§7.");
                player.sendMessage("Â§7Gunakan Â§e/partner level Â§7untuk melihat status lengkap.");
                return;
            }
            quest = plugin.getQuestManager().generateRandomQuest(partnership);
            if (quest == null) {
                player.sendMessage(getMsg("prefix") + "Â§cTidak bisa generate quest! Cek config.");
                return;
            }
            player.sendMessage(getMsg("prefix") + "Â§aQuest baru di-generate!");
        }
        
        player.sendMessage("Â§dÂ§l=== Quest Aktif ===");
        player.sendMessage("Â§7Tipe: Â§e" + quest.getQuestType().getDisplayName());
        player.sendMessage("Â§7Deskripsi: Â§f" + quest.getFormattedDescription());
        player.sendMessage("Â§7Progress: " + quest.getProgressBar() + " Â§e" + quest.getProgress() + "Â§7/Â§e" + quest.getRequiredAmount());
        player.sendMessage("Â§7Completion: Â§a" + quest.getCompletionPercentage() + "%");
        long resetHours = plugin.getConfig().getLong("quest.reset-hours", 24);
        long resetSeconds = resetHours * 3600;
        long now = java.time.Instant.now().getEpochSecond();
        long elapsed = now - quest.getCreatedAt();
        long remaining = resetSeconds - elapsed;
        
        if (remaining > 0) {
            long hoursRemaining = remaining / 3600;
            long minutesRemaining = (remaining % 3600) / 60;
            player.sendMessage("Â§7Sisa Waktu: Â§c" + hoursRemaining + " jam " + minutesRemaining + " menit");
        } else {
            player.sendMessage("Â§7Sisa Waktu: Â§câš  Kadaluarsa!");
        }
        
        int xpReward = plugin.getConfig().getInt("quest.xp-per-quest", 100);
        player.sendMessage("Â§7Reward: Â§b+" + xpReward + " XP");
    }
    
    private void handleLevelGUI(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        
        if (partnership == null) {
            player.sendMessage(getMsg("prefix") + getMsg("no-partner"));
            return;
        }
        
        new LevelsGUI(plugin, player, partnership).open();
    }
    
    private void handleTop(Player player) {
        List<Partnership> topPartnerships = plugin.getDatabaseManager().getTopPartnerships(10);
        
        if (topPartnerships.isEmpty()) {
            player.sendMessage(getMsg("prefix") + getMsg("no-partnerships"));
            return;
        }
        
        player.sendMessage("Â§dÂ§l=== Top 10 Partnership ===");
        
        int rank = 1;
        for (Partnership partnership : topPartnerships) {
            String player1 = Bukkit.getOfflinePlayer(partnership.getPlayer1()).getName();
            String player2 = Bukkit.getOfflinePlayer(partnership.getPlayer2()).getName();
            
            String rankColor = switch (rank) {
                case 1 -> "Â§6Â§l"; // Gold
                case 2 -> "Â§fÂ§l"; // Silver
                case 3 -> "Â§cÂ§l"; // Bronze
                default -> "Â§7";
            };
            
            player.sendMessage(rankColor + "#" + rank + " Â§e" + player1 + " Â§7& Â§e" + player2 + 
                " Â§7- Â§bLv." + partnership.getLevel() + " Â§7(" + partnership.getXp() + " XP)");
            rank++;
        }
    }
    
    private void handleList(Player player) {
        if (!player.hasPermission("partnershiprewards.admin")) {
            player.sendMessage(getMsg("prefix") + "Â§cKamu tidak memiliki permission!");
            return;
        }
        
        List<Partnership> partnerships = plugin.getPartnershipManager().getAllPartnerships();
        
        if (partnerships.isEmpty()) {
            player.sendMessage(getMsg("prefix") + getMsg("no-partnerships"));
            return;
        }
        
        player.sendMessage("Â§dÂ§l=== Partnership List ===");
        for (Partnership partnership : partnerships) {
            String player1 = Bukkit.getOfflinePlayer(partnership.getPlayer1()).getName();
            String player2 = Bukkit.getOfflinePlayer(partnership.getPlayer2()).getName();
            String duration = formatDuration(partnership.getDurationInSeconds());
            
            player.sendMessage("Â§e" + player1 + " Â§7& Â§e" + player2 + " Â§7- Â§a" + duration + " Â§7| Â§bLv." + partnership.getLevel());
        }
    }
    
    private void handleChat(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(getMsg("prefix") + "Â§cGunakan: Â§e/partner chat <pesan>");
            return;
        }
        
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            player.sendMessage(getMsg("prefix") + getMsg("no-partner"));
            return;
        }
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();
        Player partner = org.bukkit.Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
        String chatFormat = "Â§d[Partner] Â§f" + player.getName() + "Â§7: Â§f" + message;
        player.sendMessage(chatFormat);
        if (partner != null) {
            partner.sendMessage(chatFormat);
        } else {
            player.sendMessage("Â§7(Partner sedang offline, pesan tidak terkirim)");
        }
        if (partner != null) {
            plugin.getPartnerListener().notifySpyingAdmins(player, partner, message);
        }
    }
    
    private void handleToggle(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(getMsg("prefix") + "Â§cGunakan: Â§e/partner toggle <pvp>");
            return;
        }
        
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            player.sendMessage(getMsg("prefix") + getMsg("no-partner"));
            return;
        }
        
        if (args[1].equalsIgnoreCase("pvp")) {
            boolean newState = !partnership.isPvpEnabled();
            partnership.setPvpEnabled(newState);
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().updatePvpEnabled(partnership.getId(), newState);
            });
            String statusMsg = newState ? "Â§aÂ§lAKTIF" : "Â§cÂ§lNONAKTIF";
            String message = getMsg("prefix") + "Â§7PvP dengan partner sekarang " + statusMsg + "Â§7!";
            
            player.sendMessage(message);
            Player partner = org.bukkit.Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
            if (partner != null) {
                partner.sendMessage(message);
            }
        } else {
            player.sendMessage(getMsg("prefix") + "Â§cOpsi toggle tidak dikenal: Â§e" + args[1]);
            player.sendMessage("Â§7Tersedia: Â§epvp");
        }
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("Â§dÂ§l=== Partner Commands ===");
        player.sendMessage("Â§e/partner request <player> Â§7- Kirim permintaan partnership");
        player.sendMessage("Â§e/partner accept Â§7- Terima permintaan partnership");
        player.sendMessage("Â§e/partner reject Â§7- Tolak permintaan partnership");
        player.sendMessage("Â§e/partner break Â§7- Putuskan partnership");
        player.sendMessage("Â§e/partner info Â§7- Lihat info partnership kamu");
        player.sendMessage("Â§e/partner quest Â§7- Lihat quest aktif");
        player.sendMessage("Â§e/partner level Â§7- Buka GUI level progress");
        player.sendMessage("Â§e/partner chat <pesan> Â§7- Kirim pesan ke partner");
        player.sendMessage("Â§e/partner toggle pvp Â§7- Toggle PvP dengan partner");
        player.sendMessage("Â§e/partner top Â§7- Lihat top 10 partnership");
        
        if (player.hasPermission("partnershiprewards.admin")) {
            player.sendMessage("Â§e/partner list Â§7- Lihat semua partnership");
        }
    }
    
    private String getMsg(String key) {
        return plugin.getConfig().getString("messages." + key, "").replace("&", "Â§");
    }
    
    private String formatDuration(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        
        if (days > 0) {
            return days + " hari " + hours + " jam";
        } else if (hours > 0) {
            return hours + " jam " + minutes + " menit";
        } else {
            return minutes + " menit";
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("request", "accept", "reject", "break", "info", "quest", "level", "top", "list");
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("request")) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender)) {
                    players.add(p.getName());
                }
            }
            return players;
        }
        
        return Collections.emptyList();
    }
}
