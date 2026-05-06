package id.seria.collection.managers;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Collection;
import id.seria.collection.models.Tier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

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
        setupTables();
    }

    private void setupTables() {
        id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().runAsync(conn -> {
            try (java.sql.Statement smt = conn.createStatement()) {
                smt.execute("CREATE TABLE IF NOT EXISTS player_collections (" +
                        "uuid TEXT NOT NULL, " +
                        "collection_id TEXT NOT NULL, " +
                        "amount INTEGER DEFAULT 0, " +
                        "PRIMARY KEY (uuid, collection_id)" +
                        ");");
            } catch (java.sql.SQLException e) {
                plugin.getLogger().severe("Failed to setup player_collections table in centralized database!");
                e.printStackTrace();
            }
        });
    }

    public void loadPlayerData(UUID uuid) {
        Map<String, Integer> data = new HashMap<>();
        String sql = "SELECT collection_id, amount FROM player_collections WHERE uuid = ?";
        
        try {
            Connection conn = id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().warning("Could not load player data for " + uuid + ": Connection is null!");
                return;
            }
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    data.put(rs.getString("collection_id").toLowerCase(), rs.getInt("amount"));
                }
            }
            // plugin.getLogger().info("Loaded " + data.size() + " collections for player " + uuid);
        } catch (SQLException e) {
            plugin.getLogger().severe("SQL Error loading player data for " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
        cache.put(uuid, data);
    }

    public int getAmount(UUID uuid, String collectionId) {
        if (collectionId == null) return 0;
        collectionId = collectionId.trim().toLowerCase();
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

        SeriaCollectionPlugin.debug("addAmount: player=" + player.getName() + " collectionId=" + collectionId + " amount=" + amount + " (" + current + " -> " + next + ")");

        // Async save
        savePlayerData(uuid, collectionId, next);

        // Check for tier up
        checkTierUp(player, collectionId, current, next);
    }

    public void forceSetAmount(Player player, String collectionId, int targetAmount) {
        int current = getAmount(player.getUniqueId(), collectionId);
        int diff = targetAmount - current;
        if (diff != 0) {
            addAmount(player, collectionId, diff);
        }
    }

    public void resetCollection(Player player, String collectionId) {
        UUID uuid = player.getUniqueId();
        
        // Revoke rewards before resetting
        Collection collection = plugin.getCollectionManager().getCollection(collectionId);
        if (collection != null) {
            int currentTier = getTierLevel(uuid, collectionId);
            for (Tier tier : collection.getTiers().values()) {
                if (tier.getLevel() <= currentTier) {
                    revokeRewards(player, tier.getRewards());
                }
            }
        }

        Map<String, Integer> data = cache.get(uuid);
        if (data != null) {
            data.remove(collectionId);
        }
        
        id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().runAsync(conn -> {
            String sql = "DELETE FROM player_collections WHERE uuid = ? AND collection_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, collectionId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void resetAllCollections(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Revoke all rewards before resetting
        Map<String, Integer> data = cache.get(uuid);
        if (data != null) {
            for (String collectionId : data.keySet()) {
                Collection collection = plugin.getCollectionManager().getCollection(collectionId);
                if (collection != null) {
                    int currentTier = getTierLevel(uuid, collectionId);
                    for (Tier tier : collection.getTiers().values()) {
                        if (tier.getLevel() <= currentTier) {
                            revokeRewards(player, tier.getRewards());
                        }
                    }
                }
            }
        }

        cache.remove(uuid);
        
        id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().runAsync(conn -> {
            String sql = "DELETE FROM player_collections WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void revokeRewards(Player player, List<String> rewards) {
        for (String reward : rewards) {
            if (reward.startsWith("fortune: ")) {
                String value = reward.substring(9).trim();
                String[] args = value.split(" ");
                if (args.length >= 2) {
                    String type = args[0];
                    try {
                        int amount = Integer.parseInt(args[1]);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sfortune remove " + player.getName() + " " + type + " " + amount);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    public void handleCollectionGain(Player player, org.bukkit.entity.Item itemEntity) {
        if (itemEntity == null) return;
        
        // 1. Check if the entity is tainted (dropped by player or container)
        if (isTainted(itemEntity)) {
            SeriaCollectionPlugin.debug("Skipping tainted entity: " + itemEntity.getItemStack().getType() + " (player=" + player.getName() + ")");
            return;
        }

        // 2. Process the ItemStack normally
        handleCollectionGain(player, itemEntity.getItemStack());
    }

    public void handleCollectionGain(Player player, org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        
        // --- ANTI-EXPLOIT: Check Taint ---
        if (isTainted(item)) {
            SeriaCollectionPlugin.debug("Skipping tainted ItemStack: " + item.getType() + " (player=" + player.getName() + ")");
            cleanseItem(item);
            return;
        }


        id.seria.collection.models.Collection collection = null;
        
        // 1. Try MMOItem Tracking
        try {
            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("MythicLib")) {
                io.lumine.mythic.lib.api.item.NBTItem nbtItem = io.lumine.mythic.lib.api.item.NBTItem.get(item);
                if (nbtItem.hasType()) {
                    String type = nbtItem.getType();
                    String mmoId = nbtItem.getString("MMOITEMS_ITEM_ID");
                    collection = plugin.getCollectionManager().getCollectionByMmoId(type + ":" + mmoId);
                    SeriaCollectionPlugin.debug("MMOItem detected: " + type + ":" + mmoId + " -> collection=" + (collection != null ? collection.getId() : "null"));
                }
            }
        } catch (NoClassDefFoundError ignored) {
        }
        
        // 2. Fallback to Material Tracking
        if (collection == null) {
            collection = plugin.getCollectionManager().getCollectionByMaterial(item.getType());
            SeriaCollectionPlugin.debug("Material lookup: " + item.getType() + " -> collection=" + (collection != null ? collection.getId() : "null") + " (player=" + player.getName() + ")");
        }
        
        if (collection != null) {
            addAmount(player, collection.getId(), item.getAmount());
        }
    }

    public void taintEntity(org.bukkit.entity.Item itemEntity) {
        if (itemEntity == null) return;
        itemEntity.getPersistentDataContainer().set(SeriaCollectionPlugin.DROPPED_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
    }

    public void taintItem(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        meta.getPersistentDataContainer().set(SeriaCollectionPlugin.DROPPED_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    public boolean isTainted(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(SeriaCollectionPlugin.DROPPED_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE);
    }

    public boolean isTainted(org.bukkit.entity.Item itemEntity) {
        if (itemEntity == null) return false;
        return itemEntity.getPersistentDataContainer().has(SeriaCollectionPlugin.DROPPED_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE);
    }

    public void cleanseItem(org.bukkit.inventory.ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(SeriaCollectionPlugin.DROPPED_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE)) {
            meta.getPersistentDataContainer().remove(SeriaCollectionPlugin.DROPPED_ITEM_KEY);
            item.setItemMeta(meta);
        }
    }

    private void savePlayerData(UUID uuid, String collectionId, int amount) {
        id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().runAsync(conn -> {
            String sql = "INSERT OR REPLACE INTO player_collections (uuid, collection_id, amount) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, collectionId);
                ps.setInt(3, amount);
                ps.executeUpdate();
                // plugin.getLogger().info("[DEBUG] DB Save Successful: " + uuid + " / " + collectionId + " = " + amount);
            } catch (SQLException e) {
                plugin.getLogger().severe("DB Save Error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void checkTierUp(Player player, String collectionId, int oldAmount, int newAmount) {
        Collection collection = plugin.getCollectionManager().getCollection(collectionId);
        if (collection == null) return;

        for (Tier tier : collection.getTiers().values()) {
            if (oldAmount < tier.getRequirement() && newAmount >= tier.getRequirement()) {
                handleTierUp(player, collection, tier);
            }
        }
    }

    private void handleTierUp(Player player, Collection collection, Tier tier) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String prefix = SeriaCollectionPlugin.getMiniMessage().serialize(SeriaCollectionPlugin.getMiniMessage().deserialize(plugin.getConfigManager().getMessage("prefix")));
            
            ConfigurationSection msgConfig = plugin.getConfigManager().getMessagesConfig().getConfigurationSection("tier-up");
            if (msgConfig == null) return;

            String oldRoman = id.seria.core.utils.NumberUtils.toRoman(tier.getLevel() - 1);
            String newRoman = id.seria.core.utils.NumberUtils.toRoman(tier.getLevel());
            String tierChange = (tier.getLevel() > 1) ? "<gray>" + oldRoman + "➜<white>" + newRoman : "<white>" + newRoman;

            StringBuilder rewardsSb = new StringBuilder();
            List<String> displayRewards = tier.getDisplayRewards();
            String rewardFormat = msgConfig.getString("reward-format", "<green>  ✔ <gray>%reward%");
            
            if (displayRewards != null && !displayRewards.isEmpty()) {
                for (int i = 0; i < displayRewards.size(); i++) {
                    rewardsSb.append(rewardFormat.replace("%reward%", displayRewards.get(i)));
                    if (i < displayRewards.size() - 1) rewardsSb.append("\n");
                }
            } else {
                rewardsSb.append(msgConfig.getString("no-rewards", "<gray>  <i>Tidak ada</i>"));
            }

            List<String> messageLines = msgConfig.getStringList("message");
            for (String line : messageLines) {
                String formatted = line
                        .replace("%collection%", collection.getName())
                        .replace("%tier_change%", tierChange)
                        .replace("%old_tier_roman%", oldRoman)
                        .replace("%new_tier_roman%", newRoman)
                        .replace("%rewards%", rewardsSb.toString());
                
                player.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(formatted));
            }

            // Execute Rewards
            for (String reward : tier.getRewards()) {
                if (reward.startsWith("msg: ")) {
                    String rmsg = reward.substring(5).replace("%player%", player.getName()).replace("%prefix%", prefix);
                    player.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(rmsg));
                } else {
                    id.seria.core.utils.RewardUtility.giveReward(player, reward);
                }
            }
        });
    }

    public int getTierLevel(UUID uuid, String collectionId) {
        int amount = getAmount(uuid, collectionId);
        Collection collection = plugin.getCollectionManager().getCollection(collectionId);
        
        if (collection == null) return 0;

        int currentLevel = 0;
        for (Tier tier : collection.getTiers().values()) {
            if (amount >= tier.getRequirement()) {
                currentLevel = tier.getLevel();
            } else {
                break;
            }
        }
        return currentLevel;
    }

    public void unloadPlayerData(UUID uuid) {
        cache.remove(uuid);
    }
}
