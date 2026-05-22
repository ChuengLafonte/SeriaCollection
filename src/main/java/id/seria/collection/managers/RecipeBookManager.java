package id.seria.collection.managers;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Collection;
import id.seria.collection.models.Category;
import id.seria.crafting.SeriaCraftingPlugin;
import id.seria.crafting.recipes.SeriaRecipe;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.sarry20.topminion.models.spawnItem.SpawnItemManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeBookManager {

    private final SeriaCollectionPlugin plugin;
    private final Map<String, ItemStack> outputCache = new HashMap<>();
    private final Map<String, List<SeriaRecipe>> categoryCache = new HashMap<>();

    public RecipeBookManager(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    public void clearCache() {
        outputCache.clear();
        categoryCache.clear();
    }

    public static String formatName(String raw) {
        if (raw == null || raw.isEmpty()) return "Unknown";
        String[] parts = raw.replace("_", " ").toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public List<SeriaRecipe> getAllRecipes() {
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("SeriaCrafting")) {
            return new ArrayList<>();
        }
        try {
            return SeriaCraftingPlugin.getInstance().getRecipeManager().getRecipes();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public Map<String, List<SeriaRecipe>> getRecipesByCategory() {
        if (categoryCache.isEmpty()) {
            rebuildCategoryCache();
        }
        return categoryCache;
    }

    private synchronized void rebuildCategoryCache() {
        List<SeriaRecipe> all = getAllRecipes();
        if (all.isEmpty()) return;

        categoryCache.clear();
        for (SeriaRecipe recipe : all) {
            String category = determineCategory(recipe);
            categoryCache.computeIfAbsent(category, k -> new ArrayList<>()).add(recipe);
        }
    }

    public List<SeriaRecipe> getRecipesByOutput(ItemStack item) {
        List<SeriaRecipe> found = new ArrayList<>();
        if (item == null || item.getType() == Material.AIR) return found;

        String cleanName = getCleanName(item);
        for (SeriaRecipe recipe : getAllRecipes()) {
            ItemStack output = getOutputItem(recipe);
            if (output != null) {
                if (output.isSimilar(item)) {
                    found.add(recipe);
                } else if (output.getType() == item.getType()) {
                    String outputCleanName = getCleanName(output);
                    if (!cleanName.isEmpty() && cleanName.equals(outputCleanName)) {
                        found.add(recipe);
                    }
                }
            }
        }
        return found;
    }

    public SeriaRecipe getBestRecipeForPlayer(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        List<SeriaRecipe> matching = getRecipesByOutput(item);
        if (matching.isEmpty()) return null;
        for (SeriaRecipe recipe : matching) {
            if (isUnlocked(player, recipe)) {
                return recipe;
            }
        }
        return matching.get(0);
    }

    public SeriaRecipe getFirstRecipeForItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        String cleanName = getCleanName(item);
        SeriaCollectionPlugin.debug("Checking recipe for item: " + item.getType() + " (Clean Name: " + cleanName + ")");

        // Gunakan pencocokan langsung terhadap output yang di-cache
        for (SeriaRecipe recipe : getAllRecipes()) {
            ItemStack output = getOutputItem(recipe);
            if (output != null) {
                // 1. Coba isSimilar (paling akurat)
                if (output.isSimilar(item)) {
                    SeriaCollectionPlugin.debug("Found recipe via isSimilar: " + recipe.getMmoId());
                    return recipe;
                }
                
                // 2. Jika isSimilar gagal, coba bandingkan Nama + Material (untuk item dengan lore tambahan)
                if (output.getType() == item.getType()) {
                    String outputCleanName = getCleanName(output);
                    if (cleanName.equals(outputCleanName)) {
                        SeriaCollectionPlugin.debug("Found recipe via Material + Clean Name: " + recipe.getMmoId());
                        return recipe;
                    }
                }
            }
        }
        
        // Fallback khusus untuk Minion (sudah tercover di atas, tapi biarkan untuk keamanan ekstra)
        if (isMinionItem(item)) {
            if (cleanName.isEmpty()) return null;
            for (SeriaRecipe recipe : getAllRecipes()) {
                if (recipe.isMinionRecipe()) {
                    ItemStack output = getOutputItem(recipe);
                    if (output != null) {
                        if (cleanName.equals(getCleanName(output))) return recipe;
                    }
                }
            }
        }

        SeriaCollectionPlugin.debug("No recipe found for this item.");
        return null;
    }

    public static String getCleanName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        ItemMeta meta = item.getItemMeta();
        
        // Coba strip Bukkit
        String name = id.seria.collection.utils.GuiUtils.stripColor(meta.getDisplayName()).toLowerCase();
        
        // Jika kosong (mungkin Component), gunakan Adventure serializer
        if (name.isEmpty() && meta.hasDisplayName()) {
            try {
                name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(meta.displayName()).toLowerCase();
            } catch (Exception ignored) {}
        }
        return name.trim();
    }

    private String determineCategory(SeriaRecipe recipe) {
        if (recipe.isMinionRecipe() || recipe.getMmoId().toLowerCase().contains("minion")) {
            return "MINION";
        }

        String reqId = recipe.getRequireCollectionId();
        if (reqId != null && !reqId.isEmpty()) {
            String lowReqId = reqId.toLowerCase();
            for (Category cat : plugin.getCollectionManager().getCategories().values()) {
                if (cat.getCollections().containsKey(lowReqId)) {
                    return cat.getId().toUpperCase();
                }
            }
            
            String upper = reqId.toUpperCase();
            if (upper.contains("FARMING")) return "FARMING";
            if (upper.contains("MINING")) return "MINING";
            if (upper.contains("FORAGING")) return "FORAGING";
            if (upper.contains("COMBAT")) return "COMBAT";
            if (upper.contains("FISHING")) return "FISHING";
        }

        return "OTHERS";
    }

    public ItemStack getOutputItem(SeriaRecipe recipe) {
        String cacheKey = recipe.getMmoType() + ":" + recipe.getMmoId();
        if (outputCache.containsKey(cacheKey)) {
            ItemStack item = outputCache.get(cacheKey).clone();
            item.setAmount(recipe.getOutputAmount());
            return item;
        }

        ItemStack item = null;
        try {
            boolean isMinion = recipe.isMinionRecipe() || recipe.getMmoId().contains("_MINION_");
            if (isMinion) {
                String mType = recipe.getMinionType();
                String mMat = recipe.getMinionMaterial();
                String mLevel = String.valueOf(recipe.getMinionLevel());
                
                if (mMat == null || mMat.isEmpty() || mMat.equals("null")) {
                    String id = recipe.getMmoId().toUpperCase();
                    if (id.contains("_MINION_")) {
                        mMat = id.split("_MINION_")[0];
                        java.util.regex.Pattern p = java.util.regex.Pattern.compile("LEVEL_(\\d+)");
                        java.util.regex.Matcher m = p.matcher(id);
                        if (m.find()) mLevel = m.group(1);
                        else mLevel = "1";
                    } else {
                        mMat = id;
                        mLevel = "1";
                    }
                }

                String[] categories = {"miner", "farmer", "lumberjack", "foraging", "mining", "farming", "combat", "fishing", "slayer"};
                item = tryFindMinion(mType, mMat, mLevel, categories);
                
                if (item == null) {
                    try {
                        net.Indyuce.mmoitems.api.Type mmoType = net.Indyuce.mmoitems.MMOItems.plugin.getTypes().get(recipe.getMmoType().toUpperCase());
                        if (mmoType != null) {
                            item = net.Indyuce.mmoitems.MMOItems.plugin.getItem(mmoType, recipe.getMmoId());
                        }
                    } catch (Exception ignored) {}
                }
            } else {
                try {
                    net.Indyuce.mmoitems.api.Type mmoType = net.Indyuce.mmoitems.MMOItems.plugin.getTypes().get(recipe.getMmoType().toUpperCase());
                    if (mmoType != null) {
                        item = net.Indyuce.mmoitems.MMOItems.plugin.getItem(mmoType, recipe.getMmoId());
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        if (item == null) {
            item = id.seria.collection.utils.GuiUtils.createItem(Material.BARRIER, "<red>ERROR: Item Missing", 
                "<gray>Resep ID: <white>" + recipe.getMmoId(),
                "<gray>Pastikan item ini ada di TopMinion!",
                "<gray>Atau cek kategori di MMOItems!");
        }

        outputCache.put(cacheKey, item.clone());
        ItemStack result = item.clone();
        result.setAmount(recipe.getOutputAmount());
        return result;
    }

    private ItemStack tryFindMinion(String mType, String mMat, String mLevel, String[] categories) {
        if (mMat == null || mMat.isEmpty()) return null;
        try {
            String type = (mType != null && !mType.isEmpty() && !mType.equals("null")) ? mType.toUpperCase() : "";
            String mat = mMat.toUpperCase();
            String lvl = mLevel;

            if (!type.isEmpty()) {
                ItemStack item = SpawnItemManager.getSpawnerItem(type, mat, lvl);
                if (item != null) return item;
                item = SpawnItemManager.getSpawnerItem(mat, type, lvl);
                if (item != null) return item;
            }
            
            for (String cat : categories) {
                String c = cat.toUpperCase();
                ItemStack item = SpawnItemManager.getSpawnerItem(c, mat, lvl);
                if (item != null) return item;
                item = SpawnItemManager.getSpawnerItem(mat, c, lvl);
                if (item != null) return item;
                
                if (mat.contains("_")) {
                    String simpleMat = mat.split("_")[0];
                    item = SpawnItemManager.getSpawnerItem(c, simpleMat, lvl);
                    if (item != null) return item;
                    item = SpawnItemManager.getSpawnerItem(simpleMat, c, lvl);
                    if (item != null) return item;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public ItemStack[] getIngredientItems(SeriaRecipe recipe) {
        ItemStack[] items = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            String minionArg = recipe.getTopMinionSlots().get(i);
            if (minionArg != null) {
                String[] parts = minionArg.split(":");
                String minionType = parts[0];
                String minionLevel = parts.length > 1 ? parts[1] : "1";
                
                ItemStack item = null;
                String[] categories = {"miner", "farmer", "lumberjack", "foraging", "mining", "farming", "combat", "fishing", "slayer"};
                item = tryFindMinion("", minionType, minionLevel, categories);
                
                if (item == null) {
                    item = id.seria.collection.utils.GuiUtils.createItem(Material.BARRIER, "<red>Item Missing", 
                        "<gray>Minion: <white>" + minionType + " Lvl " + minionLevel);
                }
                item.setAmount(recipe.getGridAmounts()[i]);
                items[i] = item;
            } else {
                io.lumine.mythic.lib.api.crafting.uimanager.ProvidedUIFilter filter = recipe.getGrid()[i];
                if (filter != null && !filter.isAir()) {
                    ItemStack item = filter.getItemStack(null);
                    if (item != null) {
                        item.setAmount(recipe.getGridAmounts()[i]);
                        items[i] = item;
                    }
                }
            }
        }
        return items;
    }

    public RecipeProgress getProgress(Player player, String category) {
        List<SeriaRecipe> categoryRecipes = getRecipesByCategory().getOrDefault(category.toUpperCase(), new ArrayList<>());
        int unlocked = 0;
        for (SeriaRecipe recipe : categoryRecipes) {
            if (isUnlocked(player, recipe)) unlocked++;
        }
        return new RecipeProgress(unlocked, categoryRecipes.size());
    }

    public boolean isUnlocked(Player player, SeriaRecipe recipe) {
        if (recipe.getRequireCollectionId() == null || recipe.getRequireCollectionId().isEmpty()) return true;
        int playerTier = plugin.getPlayerDataManager().getTierLevel(player.getUniqueId(), recipe.getRequireCollectionId());
        return playerTier >= recipe.getRequireCollectionTier();
    }

    public static boolean isMinionItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        String name = getCleanName(item);
        if (name.contains("minion")) return true;

        if (meta.hasLore()) {
            for (net.kyori.adventure.text.Component line : meta.lore()) {
                String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
                if (plain.toLowerCase().contains("minion")) return true;
            }
        }
        return false;
    }

    public static String getMinionTypeFromItem(ItemStack item) {
        String name = id.seria.collection.utils.GuiUtils.stripColor(item.getItemMeta().getDisplayName()).toLowerCase();
        return name.replace(" minion", "").trim().toUpperCase().replace(" ", "_");
    }

    public static int getMinionLevelFromItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 1;
        
        String name = id.seria.collection.utils.GuiUtils.stripColor(meta.getDisplayName());
        String[] parts = name.split(" ");
        if (parts.length > 0) {
            String last = parts[parts.length - 1];
            try {
                return Integer.parseInt(last);
            } catch (Exception ignored) {}
        }

        if (!meta.hasLore()) return 1;
        for (net.kyori.adventure.text.Component line : meta.lore()) {
            String plain = id.seria.collection.utils.GuiUtils.stripColor(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line));
            if (plain.toLowerCase().contains("level") || plain.toLowerCase().contains("tier")) {
                String digits = plain.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) return Integer.parseInt(digits);
            }
        }
        return 1;
    }

    public static class RecipeProgress {
        public final int unlocked;
        public final int total;
        public RecipeProgress(int unlocked, int total) {
            this.unlocked = unlocked;
            this.total = total;
        }
        public double getPercentage() {
            return total == 0 ? 100.0 : (double) unlocked / total * 100.0;
        }
    }
}
