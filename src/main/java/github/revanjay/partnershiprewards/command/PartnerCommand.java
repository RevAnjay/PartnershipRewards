package github.revanjay.partnershiprewards.command;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.gui.LevelsGUI;
import github.revanjay.partnershiprewards.model.ActiveQuest;
import github.revanjay.partnershiprewards.model.PartnerRequest;
import github.revanjay.partnershiprewards.model.Partnership;
import github.revanjay.partnershiprewards.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;
import static github.revanjay.partnershiprewards.PartnershipRewards.sendActionBar;
import static github.revanjay.partnershiprewards.PartnershipRewards.playErrorSound;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartnerCommand implements CommandExecutor, TabCompleter {
    
    private final PartnershipRewards plugin;
    private final Map<UUID, Long> teleportCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> homeCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> homePending = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> proposalCooldown = new ConcurrentHashMap<>();
    
    public PartnerCommand(PartnershipRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("cmd-only-players", true));
            return true;
        }
        
        if (!player.hasPermission("partnershiprewards.use")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("cmd-no-permission", true));
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
            case "top", "leaderboard" -> handleLeaderboard(player);
            case "stats", "queststats" -> handleStats(player);
            case "propose" -> handlePropose(player, args);
            case "prestige", "prestige-reset" -> handlePrestige(player);
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
            player.sendMessage(plugin.getLanguageManager().getMessage("cmd-usage-request", true));
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
            player.sendMessage(getMsg("request-no-pending"));
            playErrorSound(player);
            return;
        }
        
        plugin.getRequestManager().removeRequest(player.getUniqueId());
        player.sendMessage(getMsg("request-rejected-self"));
        
        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender != null) {
            sender.sendMessage(getMsg("request-rejected-target").replace("{player}", player.getName()));
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
        int level = partnership.getLevel();
        int xp = partnership.getXp();
        int requiredXp = plugin.getQuestManager().getRequiredXpForLevel(level + 1);
        int xpPercentage = requiredXp > 0 ? (xp * 100) / requiredXp : 100;
        
        ActiveQuest quest = plugin.getQuestManager().getActiveQuest(player.getUniqueId());
        String questDesc = quest != null ? quest.getFormattedDescription() : plugin.getLanguageManager().getMessage("quest-none");

        String infoMsg = plugin.getLanguageManager().getMessage("partnership-info")
                .replace("{player}", partnerName)
                .replace("{duration}", duration)
                .replace("{start_date}", startDate)
                .replace("{level}", String.valueOf(level))
                .replace("{xp}", String.valueOf(xp))
                .replace("{required}", String.valueOf(requiredXp))
                .replace("{percent}", String.valueOf(xpPercentage))
                .replace("{quest}", questDesc);

        player.sendMessage(infoMsg);
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
                player.sendMessage(getMsg("quest-new-in").replace("{mins}", String.valueOf(remaining)));
                playErrorSound(player);
                return;
            }
            quest = plugin.getQuestManager().generateRandomQuest(partnership);
            if (quest == null) {
                player.sendMessage(getMsg("quest-failed"));
                playErrorSound(player);
                return;
            }
        }
        
        player.sendMessage(plugin.getLanguageManager().getMessage("quest-info-header"));
        player.sendMessage(plugin.getLanguageManager().getMessage("quest-info-type").replace("{type}", quest.getQuestType().getDisplayName()));
        player.sendMessage(plugin.getLanguageManager().getMessage("quest-info-desc").replace("{desc}", quest.getFormattedDescription()));
        player.sendMessage(plugin.getLanguageManager().getMessage("quest-info-progress")
                .replace("{bar}", quest.getProgressBar())
                .replace("{current}", String.valueOf(quest.getProgress()))
                .replace("{required}", String.valueOf(quest.getRequiredAmount())));
        player.sendMessage(plugin.getLanguageManager().getMessage("quest-info-completion").replace("{percent}", String.valueOf(quest.getCompletionPercentage())));
        
        long resetHours = plugin.getConfig().getLong("quest.reset-hours", 24);
        long resetSeconds = resetHours * 3600;
        long now = java.time.Instant.now().getEpochSecond();
        long elapsed = now - quest.getCreatedAt();
        long remaining = resetSeconds - elapsed;
        
        if (remaining > 0) {
            long hoursRemaining = remaining / 3600;
            long minutesRemaining = (remaining % 3600) / 60;
            player.sendMessage(plugin.getLanguageManager().getMessage("quest-info-time-left")
                    .replace("{hours}", String.valueOf(hoursRemaining))
                    .replace("{mins}", String.valueOf(minutesRemaining)));
        } else {
            player.sendMessage(plugin.getLanguageManager().getMessage("quest-info-expired"));
        }
        
        int xpReward = plugin.getConfig().getInt("quest.xp-per-quest", 100);
        player.sendMessage(plugin.getLanguageManager().getMessage("quest-info-reward").replace("{xp}", String.valueOf(xpReward)));
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
        SchedulerUtil.runTaskAsynchronously(plugin, () -> {
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
        
        SchedulerUtil.runTaskAsynchronously(plugin, () -> {
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
        String chatFormat = plugin.getLanguageManager().getMessage("chat-format")
            .replace("{player}", player.getName())
            .replace("{message}", message);
        player.sendMessage(chatFormat);
        if (partner != null) {
            partner.sendMessage(chatFormat);
            plugin.getPartnerListener().notifySpyingAdmins(player, partner, message);
        } else {
            player.sendMessage(plugin.getLanguageManager().getMessage("chat-partner-offline", true));
        }
    }
    
    private void handleToggle(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(getMsg("cmd-usage-toggle"));
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
            SchedulerUtil.runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().updatePvpEnabled(partnership.getId(), newState);
            });
            String msgKey = newState ? "pvp-enabled" : "pvp-disabled";
            String message = getMsg(msgKey);
            player.sendMessage(message);
            sendActionBar(player, newState ? "&7PvP: &aON" : "&7PvP: &cOFF");
            Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
            if (partner != null) {
                partner.sendMessage(message);
                sendActionBar(partner, newState ? "&7PvP: &aON" : "&7PvP: &cOFF");
            }
        } else if (args[1].equalsIgnoreCase("effects")) {
            boolean newState = !partnership.isEffectsEnabled();
            partnership.setEffectsEnabled(newState);
            SchedulerUtil.runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().updateEffectsEnabled(partnership.getId(), newState);
            });
            String msgKey = newState ? "effects-enabled" : "effects-disabled";
            String message = getMsg(msgKey);
            player.sendMessage(message);
            sendActionBar(player, newState ? "&7Effects: &aON" : "&7Effects: &cOFF");
            Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
            if (partner != null) {
                partner.sendMessage(message);
                sendActionBar(partner, newState ? "&7Effects: &aON" : "&7Effects: &cOFF");
            }
        } else {
            player.sendMessage(getMsg("cmd-usage-toggle"));
            playErrorSound(player);
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
            player.sendMessage(getMsg("home-min-level").replace("{level}", String.valueOf(minLevel)));
            playErrorSound(player);
            return;
        }
        
        Location loc = player.getLocation();
        partnership.setHome(loc);
        
        SchedulerUtil.runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().updatePartnerHome(partnership.getId(), loc);
        });
        
        player.sendMessage(getMsg("home-set-success"));
        
        Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
        if (partner != null) {
            partner.sendMessage(getMsg("home-changed-notify").replace("{player}", player.getName()));
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
            player.sendMessage(getMsg("home-no-home"));
            playErrorSound(player);
            return;
        }
        
        Location homeLoc = partnership.getHomeLocation();
        if (homeLoc == null) {
            player.sendMessage(getMsg("home-world-not-found"));
            playErrorSound(player);
            return;
        }
        
        int cooldownSeconds = plugin.getConfig().getInt("partner-home.cooldown-seconds", 60);
        Long lastUse = homeCooldowns.get(player.getUniqueId());
        if (lastUse != null) {
            long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
            if (elapsed < cooldownSeconds) {
                long remaining = cooldownSeconds - elapsed;
                player.sendMessage(getMsg("home-cooldown").replace("{seconds}", String.valueOf(remaining)));
                playErrorSound(player);
                return;
            }
        }
        
        if (homePending.contains(player.getUniqueId())) {
            player.sendMessage(getMsg("home-already-teleporting"));
            playErrorSound(player);
            return;
        }
        
        int warmupSeconds = plugin.getConfig().getInt("partner-home.warmup-seconds", 3);
        Location startLoc = player.getLocation().clone();
        
        player.sendMessage(getMsg("home-teleporting").replace("{seconds}", String.valueOf(warmupSeconds)));
        sendActionBar(player, "&eTeleporting in &f" + warmupSeconds + "s&e...");
        homePending.add(player.getUniqueId());
        
        SchedulerUtil.runForEntityLater(plugin, player, () -> {
            homePending.remove(player.getUniqueId());

            if (!player.isOnline()) return;
            
            Location current = player.getLocation();
            if (current.getWorld() != startLoc.getWorld() || 
                current.distanceSquared(startLoc) > 1.0) {
                player.sendMessage(plugin.getLanguageManager().getMessage("home-moved", true));
                playErrorSound(player);
                return;
            }
            
            SchedulerUtil.teleportAsync(player, homeLoc).thenAccept(success -> {
                if (Boolean.TRUE.equals(success)) {
                    homeCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                    sendActionBar(player, "&aTeleported to partner home!");
                }
            });
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
            player.sendMessage(getMsg("home-no-home"));
            playErrorSound(player);
            return;
        }
        
        partnership.setHomeWorld(null);
        
        SchedulerUtil.runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().deletePartnerHome(partnership.getId());
        });
        
        player.sendMessage(getMsg("home-del-success"));
    }
    
    private void sendHelp(Player player) {
        player.sendMessage(plugin.getLanguageManager().getMessage("help-header"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "request <player>").replace("{desc}", "Send partnership request"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "accept [player]").replace("{desc}", "Accept partnership request"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "reject").replace("{desc}", "Reject partnership request"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "break").replace("{desc}", "Break current partnership"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "info").replace("{desc}", "View partnership info"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "quest").replace("{desc}", "View active quest"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "level").replace("{desc}", "Open level progress GUI"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "chat [msg]").replace("{desc}", "Toggle/send partner chat"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "gift").replace("{desc}", "Send held item to partner"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "gifts").replace("{desc}", "Claim pending gifts"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "sethome").replace("{desc}", "Set partner home"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "home").replace("{desc}", "Teleport to partner home"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "delhome").replace("{desc}", "Delete partner home"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "toggle <pvp|effects>").replace("{desc}", "Toggle features"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "top / leaderboard").replace("{desc}", "View leaderboards"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "stats").replace("{desc}", "View detailed stats"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "propose").replace("{desc}", "Propose to partner"));
        player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "prestige").replace("{desc}", "Ascend to next Prestige"));
        
        if (player.hasPermission("partnershiprewards.admin")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("help-format").replace("{cmd}", "list").replace("{desc}", "View all partnerships"));
        }
    }

    private void handleLeaderboard(Player player) {
        new github.revanjay.partnershiprewards.gui.LeaderboardGUI(plugin, player).open();
    }

    private void handleStats(Player player) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            player.sendMessage(getMsg("no-partner"));
            playErrorSound(player);
            return;
        }
        new github.revanjay.partnershiprewards.gui.StatsGUI(plugin, player, partnership).open();
    }

    private void handlePropose(Player player, String[] args) {
        Partnership partnership = plugin.getPartnershipManager().getPartnership(player.getUniqueId());
        if (partnership == null) {
            player.sendMessage(getMsg("no-partner"));
            playErrorSound(player);
            return;
        }

        long now = System.currentTimeMillis();
        Long lastProposal = proposalCooldown.get(player.getUniqueId());
        if (lastProposal != null && now - lastProposal < 60000L) {
            long remainingSec = (60000L - (now - lastProposal)) / 1000L;
            player.sendMessage(getMsg("proposal-cooldown").replace("{seconds}", String.valueOf(remainingSec)));
            playErrorSound(player);
            return;
        }

        Player partner = Bukkit.getPlayer(partnership.getPartner(player.getUniqueId()));
        if (partner == null || !partner.isOnline()) {
            player.sendMessage(getMsg("proposal-partner-offline"));
            playErrorSound(player);
            return;
        }

        proposalCooldown.put(player.getUniqueId(), now);
        new github.revanjay.partnershiprewards.gui.ProposalGUI(plugin, player, partner).open();
        player.sendMessage(getMsg("proposal-sent").replace("{player}", partner.getName()));
    }
    private void handlePrestige(Player player) {
        if (plugin.getPrestigeManager() != null) {
            plugin.getPrestigeManager().performPrestige(player);
        }
    }
    
    private String getMsg(String key) {
        return plugin.getLanguageManager().getMessage(key, true);
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
    
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("request", "accept", "reject", "break", "info", "quest", "level", "chat", "gift", "gifts", "sethome", "home", "delhome", "toggle", "top", "leaderboard", "stats", "propose", "prestige", "list");
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
