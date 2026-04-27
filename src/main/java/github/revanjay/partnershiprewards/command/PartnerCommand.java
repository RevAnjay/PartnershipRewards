package github.revanjay.partnershiprewards.command;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.gui.LevelsGUI;
import github.revanjay.partnershiprewards.model.ActiveQuest;
import github.revanjay.partnershiprewards.model.PartnerRequest;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;
import static github.revanjay.partnershiprewards.PartnershipRewards.sendActionBar;
import static github.revanjay.partnershiprewards.PartnershipRewards.playErrorSound;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartnerCommand implements CommandExecutor, TabCompleter {
    
    private final PartnershipRewards plugin;
    private final Map<UUID, Long> homeCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> homePending = ConcurrentHashMap.newKeySet();
    
    public PartnerCommand(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cThis command can only be used by players!"));
            return true;
        }
        
        if (!player.hasPermission("partnershiprewards.use")) {
            player.sendMessage(colorize("&cYou don't have permission!"));
            playErrorSound(player);
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
            case "gift" -> handleGift(player);
            case "gifts" -> handleGifts(player);
            case "sethome" -> handleSetHome(player);
            case "home" -> handleHome(player);
            case "delhome" -> handleDelHome(player);
            default -> sendHelp(player);
        }
        
        return true;
    }
    
    private void handleRequest(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(colorize("&cUsage: /partner request <player>"));
            playErrorSound(player);
            return;
        }
        
        if (plugin.getRequestManager().isOnCooldown(player.getUniqueId())) {
            long remaining = plugin.getRequestManager().getRemainingCooldown(player.getUniqueId());
            player.sendMessage(getMsg("cooldown").replace("{seconds}", String.valueOf(remaining)));
            playErrorSound(player);
            return;
        }
        
        if (plugin.getPartnershipManager().hasPartner(player.getUniqueId())) {
            player.sendMessage(getMsg("already-partnered"));
            playErrorSound(player);
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(getMsg("player-not-found").replace("{player}", args[1]));
            playErrorSound(player);
            return;
        }
        
        if (target.equals(player)) {
            if (!plugin.getConfig().getBoolean("partnership.allow-self-partner", false)) {
                player.sendMessage(getMsg("cannot-partner-self"));
                playErrorSound(player);
                return;
            }
        }
        
        if (plugin.getPartnershipManager().hasPartner(target.getUniqueId())) {
            player.sendMessage(getMsg("target-has-partner").replace("{player}", target.getName()));
            playErrorSound(player);
            return;
        }
        
        double maxDistance = plugin.getConfig().getDouble("partnership.max-distance", 10.0);
        if (!player.getWorld().equals(target.getWorld())) {
            player.sendMessage(getMsg("too-far").replace("{player}", target.getName()));
            playErrorSound(player);
            return;
        }
        
        if (player.getLocation().distance(target.getLocation()) > maxDistance) {
            player.sendMessage(getMsg("too-far").replace("{player}", target.getName()));
            playErrorSound(player);
            return;
        }
        
        plugin.getRequestManager().createRequest(player.getUniqueId(), target.getUniqueId());
        
        player.sendMessage(getMsg("request-sent").replace("{player}", target.getName()));
        target.sendMessage(getMsg("request-received").replace("{player}", player.getName()));
    }
    
    private void handleAccept(Player player, String[] args) {
        PartnerRequest request = plugin.getRequestManager().getRequest(player.getUniqueId());
        
        if (request == null) {
            String senderName = args.length > 1 ? args[1] : "that player";
            player.sendMessage(getMsg("no-pending-request").replace("{player}", senderName));
            playErrorSound(player);
            return;
        }
        
        if (plugin.getPartnershipManager().hasPartner(player.getUniqueId())) {
            player.sendMessage(getMsg("already-partnered"));
            playErrorSound(player);
            plugin.getRequestManager().removeRequest(player.getUniqueId());
            return;
        }
        
        Player sender = Bukkit.getPlayer(request.getSender());
        
        if (plugin.getConfig().getBoolean("partnership.require-both-online", true) && sender == null) {
            player.sendMessage(getMsg("player-not-online").replace("{player}", Bukkit.getOfflinePlayer(request.getSender()).getName()));
            playErrorSound(player);
            plugin.getRequestManager().removeRequest(player.getUniqueId());
            return;
        }
        
        plugin.getPartnershipManager().createPartnership(request.getSender(), request.getTarget());
        plugin.getRequestManager().removeRequest(player.getUniqueId());
        
        player.sendMessage(getMsg("partnership-formed").replace("{player}", Bukkit.getOfflinePlayer(request.getSender()).getName()));
        if (sender != null) {
            sender.sendMessage(getMsg("partnership-formed").replace("{player}", player.getName()));
        }
    }
    
    private void handleReject(Player player) {
        PartnerRequest request = plugin.getRequestManager().getRequest(player.getUniqueId());
        
        if (request == null) {
            player.sendMessage(colorize("&cYou don't have any pending partnership requests!"));
            playErrorSound(player);
            return;
        }
        
        plugin.getRequestManager().removeRequest(player.getUniqueId());
        player.sendMessage(colorize("&aYou rejected the partnership request!"));
        
        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender != null) {
            sender.sendMessage(colorize("&c" + player.getName() + " rejected your partnership request!"));
        }
    }
    
    private void handleBreak(Player player) {
        if (!plugin.getPartnershipManager().hasPartner(player.getUniqueId())) {
            player.sendMessage(getMsg("no-partner"));
            playErrorSound(player);
            return;
        }
        
        UUID partnerUUID = plugin.getPartnershipManager().getPartnerUUID(player.getUniqueId());
        String partnerName = Bukkit.getOfflinePlayer(partnerUUID).getName();
        
        plugin.getPartnershipManager().breakPartnership(player.getUniqueId());
        
        player.sendMessage(getMsg("partnership-broken").replace("{player}", partnerName));
        
        Player partner = Bukkit.getPlayer(partnerUUID);
        if (partner != null) {
            partner.sendMessage(getMsg("partnership-broken").replace("{player}", player.getName()));
        }
    }
    
    private void handleInfo(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        
        if (partnership == null) {
            player.sendMessage(getMsg("no-partner"));
            playErrorSound(player);
            return;
        }
        
        UUID partnerUUID = partnership.getPartner(player.getUniqueId());
        String partnerName = Bukkit.getOfflinePlayer(partnerUUID).getName();
        
        long durationSeconds = partnership.getDurationInSeconds();
        String duration = formatDuration(durationSeconds);
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String startDate = sdf.format(new Date(partnership.getStartedAt() * 1000));
        player.sendMessage(colorize("&d&l=== &ePartnership Info &d&l==="));
        player.sendMessage(colorize("&7Partner: &e" + partnerName));
        player.sendMessage(colorize("&7Duration: &a" + duration));
        player.sendMessage(colorize("&7Started: &a" + startDate));
        int level = partnership.getLevel();
        int xp = partnership.getXp();
        int requiredXp = plugin.getQuestManager().getRequiredXpForLevel(level + 1);
        int xpPercentage = requiredXp > 0 ? (xp * 100) / requiredXp : 100;
        
        player.sendMessage(colorize("&7Level: &b" + level + " &7| XP: &b" + xp + "/" + requiredXp + " &7(" + xpPercentage + "%)"));
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(player.getUniqueId());
        if (quest != null) {
            player.sendMessage(colorize("&7Quest: &e" + quest.getFormattedDescription()));
            player.sendMessage(colorize("&7Progress: ") + quest.getProgressBar() + colorize(" &7(" + quest.getProgress() + "/" + quest.getRequiredAmount() + ")"));
        } else {
            player.sendMessage(colorize("&7Quest: &cNo active quest"));
        }
    }
    
    private void handleQuest(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        
        if (partnership == null) {
            player.sendMessage(getMsg("no-partner"));
            playErrorSound(player);
            return;
        }
        
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(player.getUniqueId());
        
        if (quest == null) {
            if (plugin.getQuestManager().isOnQuestCooldown(partnership)) {
                long remaining = plugin.getQuestManager().getQuestCooldownRemaining(partnership);
                player.sendMessage(colorize("&cCooldown active! &7New quest in &e" + remaining + " minutes&7."));
                playErrorSound(player);
                player.sendMessage(colorize("&7Use &e/partner level &7to view full status."));
                return;
            }
            quest = plugin.getQuestManager().generateRandomQuest(partnership);
            if (quest == null) {
                player.sendMessage(colorize("&cFailed to generate quest! Check config."));
                playErrorSound(player);
                return;
            }
            player.sendMessage(colorize("&aNew quest generated!"));
        }
        
        player.sendMessage(colorize("&d&l=== &eActive Quest &d&l==="));
        player.sendMessage(colorize("&7Type: &e" + quest.getQuestType().getDisplayName()));
        player.sendMessage(colorize("&7Description: &f" + quest.getFormattedDescription()));
        player.sendMessage(colorize("&7Progress: ") + quest.getProgressBar() + colorize(" &e" + quest.getProgress() + "&7/&e" + quest.getRequiredAmount()));
        player.sendMessage(colorize("&7Completion: &a" + quest.getCompletionPercentage() + "%"));
        long resetHours = plugin.getConfig().getLong("quest.reset-hours", 24);
        long resetSeconds = resetHours * 3600;
        long now = java.time.Instant.now().getEpochSecond();
        long elapsed = now - quest.getCreatedAt();
        long remaining = resetSeconds - elapsed;
        
        if (remaining > 0) {
            long hoursRemaining = remaining / 3600;
            long minutesRemaining = (remaining % 3600) / 60;
            player.sendMessage(colorize("&7Time Left: &c" + hoursRemaining + "h " + minutesRemaining + "m"));
        } else {
            player.sendMessage(colorize("&7Time Left: &cExpired!"));
        }
        
        int xpReward = plugin.getConfig().getInt("quest.xp-per-quest", 100);
        player.sendMessage(colorize("&7Reward: &b+" + xpReward + " XP"));
    }
    
    private void handleLevelGUI(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        
        if (partnership == null) {
            player.sendMessage(getMsg("no-partner"));
            playErrorSound(player);
            return;
        }
        
        new LevelsGUI(plugin, player, partnership).open();
    }
    
    private void handleTop(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Partnership> topPartnerships = plugin.getDatabaseManager().getTopPartnerships(10);
            
            if (topPartnerships.isEmpty()) {
                player.sendMessage(getMsg("no-partnerships"));
                playErrorSound(player);
                return;
            }
            
            player.sendMessage(colorize("&d&l=== &eTop 10 Partnerships &d&l==="));
            
            int rank = 1;
            for (Partnership partnership : topPartnerships) {
                String player1 = Bukkit.getOfflinePlayer(partnership.getPlayer1()).getName();
                String player2 = Bukkit.getOfflinePlayer(partnership.getPlayer2()).getName();
                
                String rankColor = switch (rank) {
                    case 1 -> "&6&l";
                    case 2 -> "&f&l";
                    case 3 -> "&c&l";
                    default -> "&7";
                };
                
                player.sendMessage(colorize(rankColor + "#" + rank + " &e" + player1 + " &7& &e" + player2 + 
                    " &7- &bLv." + partnership.getLevel() + " &7(" + partnership.getXp() + " XP)"));
                rank++;
            }
        });
    }
    
    private void handleList(Player player) {
        if (!player.hasPermission("partnershiprewards.admin")) {
            player.sendMessage(colorize("&cYou don't have permission!"));
            playErrorSound(player);
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Partnership> partnerships = plugin.getPartnershipManager().getAllPartnerships();
            
            if (partnerships.isEmpty()) {
                player.sendMessage(getMsg("no-partnerships"));
                playErrorSound(player);
                return;
            }
            
            player.sendMessage(colorize("&d&l=== &ePartnership List &d&l==="));
            for (Partnership partnership : partnerships) {
                String player1 = Bukkit.getOfflinePlayer(partnership.getPlayer1()).getName();
                String player2 = Bukkit.getOfflinePlayer(partnership.getPlayer2()).getName();
                String duration = formatDuration(partnership.getDurationInSeconds());
                
                player.sendMessage(colorize("&e" + player1 + " &7& &e" + player2 + " &7- &a" + duration + " &7| &bLv." + partnership.getLevel()));
            }
        });
    }
    
    private void handleChat(Player player, String[] args) {
        if (args.length < 2) {
            Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
            if (partnership == null) {
                player.sendMessage(getMsg("no-partner"));
                playErrorSound(player);
                return;
            }
            boolean toggled = plugin.getChatManager().toggleChat(player.getUniqueId());
            if (toggled) {
                player.sendMessage(colorize("&7Partner chat mode &a&lENABLED&7! All messages will go to partner channel."));
                player.sendMessage(colorize("&7Type &e/partner chat &7again to disable."));
                sendActionBar(player, "&fPartner Chat: &aON");
            } else {
                player.sendMessage(colorize("&7Partner chat mode &c&lDISABLED&7. Chat is back to normal."));
                sendActionBar(player, "&fPartner Chat: &cOFF");
            }
            return;
        }
        
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            player.sendMessage(getMsg("no-partner"));
            playErrorSound(player);
            return;
        }
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();
        Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
        String chatFormat = colorize("&d[Partner] &f" + player.getName() + "&7: &f" + message);
        player.sendMessage(chatFormat);
        if (partner != null) {
            partner.sendMessage(chatFormat);
            plugin.getPartnerListener().notifySpyingAdmins(player, partner, message);
        } else {
            player.sendMessage(colorize("&7(Partner is offline, message not delivered)"));
        }
    }
    
    private void handleToggle(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(colorize("&cUsage: &e/partner toggle <pvp|effects>"));
            playErrorSound(player);
            return;
        }
        
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            player.sendMessage(getMsg("no-partner"));
            playErrorSound(player);
            return;
        }
        
        if (args[1].equalsIgnoreCase("pvp")) {
            boolean newState = !partnership.isPvpEnabled();
            partnership.setPvpEnabled(newState);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().updatePvpEnabled(partnership.getId(), newState);
            });
            String statusMsg = newState ? colorize("&a&lENABLED") : colorize("&c&lDISABLED");
            String message = colorize("&7PvP with partner is now ") + statusMsg + colorize("&7!");
            
            player.sendMessage(message);
            sendActionBar(player, "&7PvP: " + (newState ? "&aON" : "&cOFF"));
            Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
            if (partner != null) {
                partner.sendMessage(message);
                sendActionBar(partner, "&7PvP: " + (newState ? "&aON" : "&cOFF"));
            }
        } else if (args[1].equalsIgnoreCase("effects")) {
            boolean newState = !partnership.isEffectsEnabled();
            partnership.setEffectsEnabled(newState);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().updateEffectsEnabled(partnership.getId(), newState);
            });
            String statusMsg = newState ? colorize("&a&lENABLED") : colorize("&c&lDISABLED");
            String message = colorize("&7Partner effects are now ") + statusMsg + colorize("&7!");
            
            player.sendMessage(message);
            sendActionBar(player, "&7Effects: " + (newState ? "&aON" : "&cOFF"));
            Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
            if (partner != null) {
                partner.sendMessage(message);
                sendActionBar(partner, "&7Effects: " + (newState ? "&aON" : "&cOFF"));
            }
        } else {
            player.sendMessage(colorize("&cUnknown toggle option: &e" + args[1]));
            playErrorSound(player);
            player.sendMessage(colorize("&7Available: &epvp, effects"));
        }
    }
    
    
    private void handleGift(Player player) {
        plugin.getGiftManager().sendGift(player, null);
    }
    
    private void handleGifts(Player player) {
        plugin.getGiftManager().claimGifts(player);
    }
    
    
    private void handleSetHome(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            player.sendMessage(getMsg("no-partner"));
            playErrorSound(player);
            return;
        }
        
        int minLevel = plugin.getConfig().getInt("partner-home.min-level", 3);
        if (partnership.getLevel() < minLevel) {
            player.sendMessage(colorize("&cPartnership must be level &e" + minLevel + " &cto set home!"));
            playErrorSound(player);
            return;
        }
        
        Location loc = player.getLocation();
        partnership.setHome(loc);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().updatePartnerHome(partnership.getId(), loc);
        });
        
        player.sendMessage(colorize("&aPartner home set successfully!"));
        player.sendMessage(colorize("&7Location: &e" + loc.getWorld().getName() + " &7(" + (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ() + ")"));
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
        if (partner != null) {
            partner.sendMessage(colorize("&e" + player.getName() + " &7has changed the partner home!"));
        }
    }
    
    private void handleHome(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            player.sendMessage(getMsg("no-partner"));
            playErrorSound(player);
            return;
        }
        
        if (!partnership.hasHome()) {
            player.sendMessage(colorize("&cPartner home is not set! Use &e/partner sethome"));
            playErrorSound(player);
            return;
        }
        
        Location homeLoc = partnership.getHomeLocation();
        if (homeLoc == null) {
            player.sendMessage(colorize("&cPartner home world not found!"));
            playErrorSound(player);
            return;
        }
        
        int cooldownSeconds = plugin.getConfig().getInt("partner-home.cooldown-seconds", 60);
        Long lastUse = homeCooldowns.get(player.getUniqueId());
        if (lastUse != null) {
            long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
            if (elapsed < cooldownSeconds) {
                long remaining = cooldownSeconds - elapsed;
                player.sendMessage(colorize("&cWait &e" + remaining + " seconds &cmore!"));
                playErrorSound(player);
                return;
            }
        }
        
        if (homePending.contains(player.getUniqueId())) {
            player.sendMessage(colorize("&cYou are already teleporting!"));
            playErrorSound(player);
            return;
        }
        
        int warmupSeconds = plugin.getConfig().getInt("partner-home.warmup-seconds", 3);
        Location startLoc = player.getLocation().clone();
        
        player.sendMessage(colorize("&eTeleporting in &f" + warmupSeconds + " seconds&e... Don't move!"));
        sendActionBar(player, "&eTeleporting in &f" + warmupSeconds + "s&e...");
        homePending.add(player.getUniqueId());
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            homePending.remove(player.getUniqueId());
            
            if (!player.isOnline()) return;
            
            Location current = player.getLocation();
            if (current.getWorld() != startLoc.getWorld() || 
                current.distanceSquared(startLoc) > 1.0) {
                player.sendMessage(colorize("&cTeleport cancelled! You moved."));
                playErrorSound(player);
                return;
            }
            
            player.teleport(homeLoc);
            homeCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            sendActionBar(player, "&aTeleported to partner home!");
        }, 20L * warmupSeconds);
    }
    
    private void handleDelHome(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            player.sendMessage(getMsg("no-partner"));
            playErrorSound(player);
            return;
        }
        
        if (!partnership.hasHome()) {
            player.sendMessage(colorize("&cPartner home is not set!"));
            playErrorSound(player);
            return;
        }
        
        partnership.setHomeWorld(null);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().deletePartnerHome(partnership.getId());
        });
        
        player.sendMessage(colorize("&aPartner home deleted!"));
    }
    
    private void sendHelp(Player player) {
        player.sendMessage(colorize("&d&l=== &ePartner Commands &d&l==="));
        player.sendMessage(colorize("&e/partner request <player> &7- Send a partnership request"));
        player.sendMessage(colorize("&e/partner accept &7- Accept a partnership request"));
        player.sendMessage(colorize("&e/partner reject &7- Reject a partnership request"));
        player.sendMessage(colorize("&e/partner break &7- Break the partnership"));
        player.sendMessage(colorize("&e/partner info &7- View partnership info"));
        player.sendMessage(colorize("&e/partner quest &7- View active quest"));
        player.sendMessage(colorize("&e/partner level &7- Open level progress GUI"));
        player.sendMessage(colorize("&e/partner chat [message] &7- Toggle/send message to partner"));
        player.sendMessage(colorize("&e/partner gift &7- Send an item to your partner"));
        player.sendMessage(colorize("&e/partner gifts &7- Claim gifts from your partner"));
        player.sendMessage(colorize("&e/partner sethome &7- Set partner home"));
        player.sendMessage(colorize("&e/partner home &7- Teleport to partner home"));
        player.sendMessage(colorize("&e/partner delhome &7- Delete partner home"));
        player.sendMessage(colorize("&e/partner toggle <pvp|effects> &7- Toggle features"));
        player.sendMessage(colorize("&e/partner top &7- View top 10 partnerships"));
        
        if (player.hasPermission("partnershiprewards.admin")) {
            player.sendMessage(colorize("&e/partner list &7- View all partnerships"));
        }
    }
    
    private String getMsg(String key) {
        return colorize(plugin.getConfig().getString("messages." + key, ""));
    }
    
    private String formatDuration(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        
        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("request", "accept", "reject", "break", "info", "quest", "level", "chat", "gift", "gifts", "sethome", "home", "delhome", "toggle", "top", "list");
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
