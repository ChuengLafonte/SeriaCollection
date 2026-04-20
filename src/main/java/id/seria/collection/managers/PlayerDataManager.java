package id.seria.collection.managers;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Collection;
import id.seria.collection.models.Tier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerDataManager {

    private final SeriaCollectionPlugin plugin;
    private final Map<UUID, Map<String, Integer>> cache = new HashMap<>();

    public PlayerDataManager(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadPlayerData(UUID uuid) {
        Map<String, Integer> data = new HashMap<>();
        String sql = "SELECT collection_id, amount FROM player_collections WHERE uuid = ?";
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    data.put(rs.getString("collection_id"), rs.getInt("amount"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cache.put(uuid, data);
    }

    public int getAmount(UUID uuid, String collectionId) {
        Map<String, Integer> data = cache.get(uuid);
        if (data == null) {
            loadPlayerData(uuid);
            data = cache.get(uuid);
        }
        return data.getOrDefault(collectionId, 0);
    }

    public void addAmount(Player player, String collectionId, int amount) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> data = cache.get(uuid);
        if (data == null) {
            loadPlayerData(uuid);
            data = cache.get(uuid);
        }

        int current = data.getOrDefault(collectionId, 0);
        int next = current + amount;
        data.put(collectionId, next);

        // Async save
        savePlayerData(uuid, collectionId, next);

        // Check for tier up
        checkTierUp(player, collectionId, current, next);
    }

    public void handleCollectionGain(Player player, org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        
        id.seria.collection.models.Collection collection = null;
        
        // 1. Try MMOItem Tracking
        try {
            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("MythicLib")) {
                io.lumine.mythic.lib.api.item.NBTItem nbtItem = io.lumine.mythic.lib.api.item.NBTItem.get(item);
                if (nbtItem.hasType()) {
                    String type = nbtItem.getType();
                    String id = nbtItem.getString("MMOITEMS_ITEM_ID");
                    collection = plugin.getCollectionManager().getCollectionByMmoId(type + ":" + id);
                }
            }
        } catch (NoClassDefFoundError ignored) {
            // MythicLib not available or incorrect version
        }
        
        // 2. Fallback to Material Tracking
        if (collection == null) {
            collection = plugin.getCollectionManager().getCollectionByMaterial(item.getType());
        }
        
        if (collection != null) {
            addAmount(player, collection.getId(), item.getAmount());
        }
    }

    private void savePlayerData(UUID uuid, String collectionId, int amount) {
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO player_collections (uuid, collection_id, amount) VALUES (?, ?, ?)";
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, collectionId);
                    ps.setInt(3, amount);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void checkTierUp(Player player, String collectionId, int oldAmount, int newAmount) {
        Collection collection = plugin.getCollectionManager().getCollection(collectionId);
        if (collection == null) return;

        for (Tier tier : collection.getTiers().values()) {
            if (oldAmount < tier.getRequirement() && newAmount >= tier.getRequirement()) {
                // Tier Up Logic
                handleTierUp(player, collection, tier);
            }
        }
    }

    private void handleTierUp(Player player, Collection collection, Tier tier) {
        // Run rewards on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            String prefix = SeriaCollectionPlugin.getMiniMessage().serialize(SeriaCollectionPlugin.getMiniMessage().deserialize(plugin.getConfigManager().getMessage("prefix")));
            
            // Notification Elements
            String oldRoman = id.seria.core.utils.NumberUtils.toRoman(tier.getLevel() - 1);
            String newRoman = id.seria.core.utils.NumberUtils.toRoman(tier.getLevel());
            
            StringBuilder sb = new StringBuilder();
            sb.append("<strikethrough><yellow>                                                             </yellow></strikethrough>\n");
            sb.append("<gold><b>COLLECTION LEVEL UP </b><white>").append(collection.getName()).append(" ");
            if (tier.getLevel() > 1) {
                sb.append("<gray>").append(oldRoman).append("➜<white>").append(newRoman);
            } else {
                sb.append("<white>").append(newRoman);
            }
            sb.append("\n\n");
            sb.append("<green><b>REWARDS</b>\n");
            
            List<String> displayRewards = tier.getDisplayRewards();
            if (displayRewards != null && !displayRewards.isEmpty()) {
                for (String dr : displayRewards) {
                    sb.append("<green>  ✔ <gray>").append(dr).append("\n");
                }
            } else {
                sb.append("<gray>  <i>None</i>\n");
            }
            sb.append("\n<strikethrough><yellow>                                                             </yellow></strikethrough>");

            player.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(sb.toString()));

            // Execute Rewards
            for (String reward : tier.getRewards()) {
                if (reward.startsWith("cmd: ")) {
                    String cmd = reward.substring(5).replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                } else if (reward.startsWith("msg: ")) {
                    String rmsg = reward.substring(5).replace("%player%", player.getName()).replace("%prefix%", prefix);
                    player.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(rmsg));
                } else if (reward.startsWith("fortune: ")) {
                    String[] parts = reward.substring(9).split(" ");
                    if (parts.length >= 2) {
                        String type = parts[0];
                        String amount = parts[1];
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sfortune add " + player.getName() + " " + type + " " + amount);
                    }
                }
            }
        });
    }


    public void unloadPlayerData(UUID uuid) {
        cache.remove(uuid);
    }
}
