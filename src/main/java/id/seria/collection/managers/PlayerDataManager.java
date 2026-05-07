package id.seria.collection.managers;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Collection;
import id.seria.collection.models.Tier;
import id.seria.collection.integration.topminions.TopMinionsHook;
import id.seria.collection.utils.GuiUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final SeriaCollectionPlugin plugin;
    private final Map<UUID, Map<String, Integer>> cache = new HashMap<>();
    private final Map<UUID, Map<String, List<Integer>>> minionCraftCache = new HashMap<>();

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
                smt.execute("CREATE TABLE IF NOT EXISTS player_minion_crafts (" +
                        "uuid TEXT NOT NULL, " +
                        "minion_id TEXT NOT NULL, " +
                        "tier INTEGER NOT NULL, " +
                        "PRIMARY KEY (uuid, minion_id, tier)" +
                        ");");
            } catch (java.sql.SQLException e) {
                plugin.getLogger().severe("Failed to setup tables!");
                e.printStackTrace();
            }
        });
    }

    public void loadPlayerData(UUID uuid) {
        Map<String, Integer> data = new HashMap<>();
        String sql = "SELECT collection_id, amount FROM player_collections WHERE uuid = ?";
        try {
            Connection conn = id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().getConnection();
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        data.put(rs.getString("collection_id").toLowerCase(), rs.getInt("amount"));
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        cache.put(uuid, data);
        loadMinionCraftData(uuid);
    }

    public void loadMinionCraftData(UUID uuid) {
        Map<String, List<Integer>> data = new HashMap<>();
        String sql = "SELECT minion_id, tier FROM player_minion_crafts WHERE uuid = ?";
        try {
            Connection conn = id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().getConnection();
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        String id = rs.getString("minion_id").toLowerCase();
                        int tier = rs.getInt("tier");
                        data.computeIfAbsent(id, k -> new ArrayList<>()).add(tier);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        minionCraftCache.put(uuid, data);
    }

    public boolean hasCraftedMinion(UUID uuid, String type, int tier) {
        Map<String, List<Integer>> data = minionCraftCache.get(uuid);
        if (data == null) {
            loadMinionCraftData(uuid);
            data = minionCraftCache.get(uuid);
        }
        List<Integer> tiers = data.get(type.toLowerCase());
        return tiers != null && tiers.contains(tier);
    }

    public void handleMinionCraft(Player player, String type, int tier) {
        UUID uuid = player.getUniqueId();
        String lowerType = type.toLowerCase();
        Map<String, List<Integer>> data = minionCraftCache.get(uuid);
        if (data == null) {
            loadMinionCraftData(uuid);
            data = minionCraftCache.get(uuid);
        }
        List<Integer> tiers = data.computeIfAbsent(lowerType, k -> new ArrayList<>());
        if (!tiers.contains(tier)) {
            tiers.add(tier);
            saveMinionCraftData(uuid, lowerType, tier);
            
            // Notification for new craft (Image 2)
            int uniqueCrafts = getTotalUniqueMinionCrafts(uuid);
            int needed = 0;
            int nextSlot = 0;
            
            ConfigurationSection milestones = plugin.getConfigManager().getMinionsConfig().getConfigurationSection("milestones");
            if (milestones != null) {
                List<String> keys = new ArrayList<>(milestones.getKeys(false));
                keys.sort((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
                for (String key : keys) {
                    int req = Integer.parseInt(key);
                    if (req > uniqueCrafts) {
                        needed = req - uniqueCrafts;
                        nextSlot = milestones.getInt(key);
                        break;
                    }
                }
            }

            List<String> lines = plugin.getConfigManager().getMessagesConfig().getStringList("minion-new-craft");
            for (String line : lines) {
                player.sendMessage(GuiUtils.format(line
                    .replace("%tier%", id.seria.core.utils.NumberUtils.toRoman(tier))
                    .replace("%minion%", type)
                    .replace("%needed%", String.valueOf(needed))
                    .replace("%next_slot%", String.valueOf(nextSlot))));
            }
            
            checkMinionMilestones(player);
        }
    }

    public void addMinionCraft(Player player, String type, int tier) {
        handleMinionCraft(player, type, tier);
    }

    private void saveMinionCraftData(UUID uuid, String minionId, int tier) {
        id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().runAsync(conn -> {
            String sql = "INSERT OR IGNORE INTO player_minion_crafts (uuid, minion_id, tier) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, minionId.toLowerCase());
                ps.setInt(3, tier);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public int getTotalUniqueMinionCrafts(UUID uuid) {
        Map<String, List<Integer>> data = minionCraftCache.get(uuid);
        if (data == null) return 0;
        int total = 0;
        for (List<Integer> tiers : data.values()) total += tiers.size();
        return total;
    }

    public void checkMinionMilestones(Player player) {
        int uniqueCrafts = getTotalUniqueMinionCrafts(player.getUniqueId());
        int currentLevel = 0;
        
        ConfigurationSection milestones = plugin.getConfigManager().getMinionsConfig().getConfigurationSection("milestones");
        if (milestones != null) {
            List<String> keys = new ArrayList<>(milestones.getKeys(false));
            keys.sort((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
            for (String key : keys) {
                try {
                    int req = Integer.parseInt(key);
                    if (uniqueCrafts >= req) {
                        currentLevel = milestones.getInt(key);
                    } else break;
                } catch (NumberFormatException ignored) {}
            }
        }
        
        if (currentLevel > 0) {
            // Kita perlu melacak level terakhir yang sudah diberikan agar tidak spam notifikasi
            // Untuk sementara kita berikan saja permission-nya
            TopMinionsHook.setMilestonePermission(player, currentLevel);
            
            // Jika pas baru mencapai angka milestone, kirim notifikasi unlock (Image 3)
            if (milestones != null && milestones.contains(String.valueOf(uniqueCrafts))) {
                String msg = plugin.getConfigManager().getMessage("minion-slot-unlock")
                        .replace("%slot%", String.valueOf(currentLevel));
                player.sendMessage(GuiUtils.format(msg));
            }
        }
    }

    public int getAmount(UUID uuid, String colId) {
        if (colId == null) return 0;
        colId = colId.trim().toLowerCase();
        Map<String, Integer> data = cache.get(uuid);
        if (data == null) {
            loadPlayerData(uuid);
            data = cache.get(uuid);
        }
        return data.getOrDefault(colId, 0);
    }

    public void addAmount(Player player, String colId, int amount) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> data = cache.get(uuid);
        if (data == null) {
            loadPlayerData(uuid);
            data = cache.get(uuid);
        }
        int cur = data.getOrDefault(colId, 0);
        int next = cur + amount;
        data.put(colId, next);
        savePlayerData(uuid, colId, next);
        checkTierUp(player, colId, cur, next);
    }

    public void forceSetAmount(Player player, String colId, int target) {
        int cur = getAmount(player.getUniqueId(), colId);
        int diff = target - cur;
        if (diff != 0) addAmount(player, colId, diff);
    }

    public void resetCollection(Player player, String colId) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> data = cache.get(uuid);
        if (data != null) data.remove(colId);
        id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().runAsync(conn -> {
            String sql = "DELETE FROM player_collections WHERE uuid = ? AND collection_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, colId);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void resetAllCollections(Player player) {
        UUID uuid = player.getUniqueId();
        cache.remove(uuid);
        id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().runAsync(conn -> {
            String sql = "DELETE FROM player_collections WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void handleCollectionGain(Player player, org.bukkit.entity.Item entity) {
        if (entity == null || isTainted(entity)) return;
        handleCollectionGain(player, entity.getItemStack());
    }

    public void handleCollectionGain(Player player, org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir() || isTainted(item)) return;
        Collection col = plugin.getCollectionManager().getCollectionByMaterial(item.getType());
        if (col != null) addAmount(player, col.getId(), item.getAmount());
    }

    public void taintEntity(org.bukkit.entity.Item entity) {
        if (entity != null) entity.getPersistentDataContainer().set(SeriaCollectionPlugin.DROPPED_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
    }

    public void taintItem(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(SeriaCollectionPlugin.DROPPED_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
    }

    public boolean isTainted(org.bukkit.inventory.ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(SeriaCollectionPlugin.DROPPED_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE);
    }

    public boolean isTainted(org.bukkit.entity.Item entity) {
        if (entity == null) return false;
        return entity.getPersistentDataContainer().has(SeriaCollectionPlugin.DROPPED_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE);
    }

    private void savePlayerData(UUID uuid, String colId, int amount) {
        id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().runAsync(conn -> {
            String sql = "INSERT OR REPLACE INTO player_collections (uuid, collection_id, amount) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, colId.toLowerCase());
                ps.setInt(3, amount);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    private void checkTierUp(Player player, String colId, int old, int next) {
        Collection col = plugin.getCollectionManager().getCollection(colId);
        if (col == null) return;
        for (Tier tier : col.getTiers().values()) {
            if (old < tier.getRequirement() && next >= tier.getRequirement()) handleTierUp(player, col, tier);
        }
    }

    private void handleTierUp(Player player, Collection col, Tier tier) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String oldRoman = id.seria.core.utils.NumberUtils.toRoman(tier.getLevel() - 1);
            if (tier.getLevel() <= 1) oldRoman = "0";
            String newRoman = id.seria.core.utils.NumberUtils.toRoman(tier.getLevel());
            
            // Format rewards list
            StringBuilder rewardsBuilder = new StringBuilder();
            List<String> rewards = tier.getRewards();
            for (int i = 0; i < rewards.size(); i++) {
                // Sederhanakan tampilan reward (biasanya format reward di core punya prefix tersendiri)
                // Kita buat estetik seperti di gambar
                rewardsBuilder.append("<gray>• ").append(rewards.get(i));
                if (i < rewards.size() - 1) rewardsBuilder.append("\n");
            }

            List<String> lines = plugin.getConfigManager().getMessagesConfig().getStringList("tier-up-broadcast");
            for (String line : lines) {
                player.sendMessage(GuiUtils.format(line
                    .replace("%collection%", col.getName())
                    .replace("%old_roman%", oldRoman)
                    .replace("%new_roman%", newRoman)
                    .replace("%rewards%", rewardsBuilder.toString())));
            }

            // Berikan reward ke pemain
            for (String reward : tier.getRewards()) {
                id.seria.core.utils.RewardUtility.giveReward(player, reward);
            }
        });
    }

    public int getTierLevel(UUID uuid, String colId) {
        int amount = getAmount(uuid, colId);
        Collection col = plugin.getCollectionManager().getCollection(colId);
        if (col == null) return 0;
        int lvl = 0;
        for (Tier tier : col.getTiers().values()) {
            if (amount >= tier.getRequirement()) lvl = tier.getLevel();
            else break;
        }
        return lvl;
    }

    public void resetMinion(Player player, String minionId) {
        UUID uuid = player.getUniqueId();
        Map<String, List<Integer>> data = minionCraftCache.get(uuid);
        if (data != null) data.remove(minionId.toLowerCase());
        
        id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().runAsync(conn -> {
            String sql = "DELETE FROM player_minion_crafts WHERE uuid = ? AND minion_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, minionId.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void resetAllMinions(Player player) {
        UUID uuid = player.getUniqueId();
        minionCraftCache.remove(uuid);
        id.seria.core.SeriaCorePlugin.getInstance().getPlayerDataManager().runAsync(conn -> {
            String sql = "DELETE FROM player_minion_crafts WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void unloadPlayerData(UUID uuid) {
        cache.remove(uuid);
        minionCraftCache.remove(uuid);
    }
}
