package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import github.revanjay.partnershiprewards.model.GiftData;
import github.revanjay.partnershiprewards.model.Partnership;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;
import static github.revanjay.partnershiprewards.PartnershipRewards.playErrorSound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class GiftManager {
    
    private final PartnershipRewards plugin;
    private final int maxPendingGifts;
    
    public GiftManager(PartnershipRewards plugin) {
        this.plugin = plugin;
        this.maxPendingGifts = plugin.getConfig().getInt("gift.max-pending", 5);
    }
    
    public void sendGift(Player sender, UUID receiverUuid) {
        ItemStack item = sender.getInventory().getItemInMainHand();
        
        if (item.getType().isAir()) {
            sender.sendMessage(colorize("&cYou must hold an item to send!"));
            playErrorSound(sender);
            return;
        }
        
        Partnership partnership = plugin.getPartnershipManager().getPartnership(sender.getUniqueId());
        if (partnership == null) {
            sender.sendMessage(getMsg("no-partner"));
            playErrorSound(sender);
            return;
        }
        
        UUID partnerUuid = partnership.getPartner(sender.getUniqueId());
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int count = plugin.getDatabaseManager().getGiftCount(partnerUuid);
            if (count >= maxPendingGifts) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(colorize("&cPartner already has &e" + maxPendingGifts + " &cpending gifts! Wait until they claim."));
                    playErrorSound(sender);
                });
                return;
            }
            
            String serialized = serializeItem(item);
            if (serialized == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(colorize("&cFailed to send gift! Item cannot be serialized."));
                    playErrorSound(sender);
                });
                return;
            }
            
            plugin.getDatabaseManager().saveGift(sender.getUniqueId(), partnerUuid, serialized, Instant.now().getEpochSecond());
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.getInventory().setItemInMainHand(null);
                
                String partnerName = Bukkit.getOfflinePlayer(partnerUuid).getName();
                if (partnerName == null) partnerName = partnerUuid.toString();
                sender.sendMessage(colorize("&aGift sent to &e" + partnerName + "&a!"));
                sender.sendMessage(colorize("&7Item: &f" + item.getType().name() + " x" + item.getAmount()));
                
                Player partner = Bukkit.getPlayer(partnerUuid);
                if (partner != null) {
                    partner.sendMessage(colorize("&7You received a gift from &e" + sender.getName() + "&7!"));
                    partner.sendMessage(colorize("&7Use &e/partner gifts &7to claim."));
                }
            });
        });
    }
    
    public void claimGifts(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<GiftData> gifts = plugin.getDatabaseManager().getPendingGifts(player.getUniqueId());
            
            if (gifts.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(colorize("&7No pending gifts."));
                });
                return;
            }
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                int claimed = 0;
                for (GiftData gift : gifts) {
                    ItemStack item = deserializeItem(gift.getItemData());
                    if (item == null) {
                        plugin.getDatabaseManager().deleteGift(gift.getId());
                        continue;
                    }
                    
                    var leftover = player.getInventory().addItem(item);
                    if (leftover.isEmpty()) {
                        plugin.getDatabaseManager().deleteGift(gift.getId());
                        String senderName = Bukkit.getOfflinePlayer(gift.getSender()).getName();
                        if (senderName == null) senderName = "Unknown";
                        player.sendMessage(colorize("&a+ &7Gift from &e" + senderName + "&7: &f" + item.getType().name() + " x" + item.getAmount()));
                        claimed++;
                    } else {
                        player.sendMessage(colorize("&cInventory full! Remaining gifts cannot be claimed."));
                        playErrorSound(player);
                        break;
                    }
                }
                
                if (claimed > 0) {
                    player.sendMessage(colorize("&a" + claimed + " &agifts claimed!"));
                }
            });
        });
    }
    
    public void notifyPendingGifts(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int count = plugin.getDatabaseManager().getGiftCount(player.getUniqueId());
            if (count > 0) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(colorize("&7You have &e" + count + " &7pending gifts!"));
                    player.sendMessage(colorize("&7Use &e/partner gifts &7to claim."));
                });
            }
        });
    }
    
    private String serializeItem(ItemStack item) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
            oos.writeObject(item);
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().severe("Error serializing item: " + e.getMessage());
            return null;
        }
    }
    
    private ItemStack deserializeItem(String data) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bis)) {
            return (ItemStack) ois.readObject();
        } catch (Exception e) {
            plugin.getLogger().severe("Error deserializing item: " + e.getMessage());
            return null;
        }
    }
    
    private String getMsg(String key) {
        return colorize(plugin.getConfig().getString("messages." + key, ""));
    }
}
