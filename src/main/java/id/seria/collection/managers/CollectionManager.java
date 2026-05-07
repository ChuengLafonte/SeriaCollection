package id.seria.collection.managers;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Category;
import id.seria.collection.models.Collection;
import id.seria.collection.models.Tier;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class CollectionManager {

    private final SeriaCollectionPlugin plugin;
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<String, Collection> collectionsById = new LinkedHashMap<>();
    private final Map<Material, Collection> collectionsByMaterial = new HashMap<>();
    private final Map<String, Collection> collectionsByMmoId = new HashMap<>();

    public CollectionManager(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
        loadCollections();
    }

    public void loadCollections() {
        categories.clear();
        collectionsById.clear();
        collectionsByMaterial.clear();
        collectionsByMmoId.clear();

        // 1. Load Category Order & Definitions from collections.yml
        FileConfiguration mainConfig = plugin.getConfigManager().getCollectionsConfig();
        loadCategoriesFromConfig(mainConfig);

        // 2. Load all collection files
        List<FileConfiguration> allConfigs = new ArrayList<>();
        allConfigs.add(mainConfig);
        allConfigs.addAll(plugin.getConfigManager().getCollectionFiles());

        for (FileConfiguration config : allConfigs) {
            // Kita dukung 3 format:
            // 1. Root-level collections (koleksi langsung di root)
            // 2. Category-wrapped collections (kategori -> collections -> item) -> Format baru Anda
            // 3. Deep-wrapped (categories -> kategori -> collections -> item) -> Format lama
            
            for (String key : config.getKeys(false)) {
                if (key.equals("categories")) {
                    // Format 3: Deep-wrapped
                    ConfigurationSection deepCats = config.getConfigurationSection("categories");
                    if (deepCats != null) {
                        for (String catKey : deepCats.getKeys(false)) {
                            // Daftarkan kategori jika belum ada
                            ensureCategoryExists(catKey, deepCats.getConfigurationSection(catKey));
                            
                            ConfigurationSection catColls = deepCats.getConfigurationSection(catKey + ".collections");
                            if (catColls != null) {
                                for (String collKey : catColls.getKeys(false)) {
                                    parseAndRegisterCollection(collKey, catColls.getConfigurationSection(collKey), catKey);
                                }
                            }
                        }
                    }
                    continue;
                }

                ConfigurationSection section = config.getConfigurationSection(key);
                if (section == null) continue;

                // Cek apakah ini Format 2: Category-wrapped (kategori -> collections -> item)
                if (section.contains("collections")) {
                    ensureCategoryExists(key, section);
                    ConfigurationSection colls = section.getConfigurationSection("collections");
                    if (colls != null) {
                        for (String collKey : colls.getKeys(false)) {
                            parseAndRegisterCollection(collKey, colls.getConfigurationSection(collKey), key);
                        }
                    }
                } 
                // Cek apakah ini Format 1: Root-level collection
                else if (section.contains("material") || section.contains("tiers")) {
                    String categoryHint = section.getString("category", "OTHERS");
                    parseAndRegisterCollection(key, section, categoryHint);
                }
            }
        }
    }

    private void loadCategoriesFromConfig(FileConfiguration config) {
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) return;

        for (String catKey : categoriesSection.getKeys(false)) {
            ensureCategoryExists(catKey, categoriesSection.getConfigurationSection(catKey));
        }
    }

    private void ensureCategoryExists(String catKey, ConfigurationSection section) {
        String key = catKey.toUpperCase();
        if (categories.containsKey(key)) return;

        String name = catKey;
        Material icon = Material.CHEST;
        List<String> lore = new ArrayList<>();

        if (section != null) {
            name = section.getString("name", catKey);
            try {
                icon = Material.valueOf(section.getString("icon", "CHEST").toUpperCase());
            } catch (Exception ignored) {}
            lore = section.getStringList("lore");
        }

        categories.put(key, new Category(key, name, icon, lore));
    }

    private void parseAndRegisterCollection(String collKey, ConfigurationSection collSection, String categoryKey) {
        if (collSection == null || collectionsById.containsKey(collKey)) return;

        String collName = collSection.getString("name", collKey);
        String mmoId = collSection.getString("mmoitem-id");
        
        List<Material> materialList = new ArrayList<>();
        if (collSection.contains("material")) {
            if (collSection.isList("material")) {
                for (String matStr : collSection.getStringList("material")) {
                    try { materialList.add(Material.valueOf(matStr.toUpperCase())); } catch (IllegalArgumentException ignored) {}
                }
            } else {
                try { materialList.add(Material.valueOf(collSection.getString("material", "AIR").toUpperCase())); } catch (IllegalArgumentException ignored) {}
            }
        }

        Collection collection = new Collection(collKey, collName, materialList, mmoId);
        
        // Load Tiers
        ConfigurationSection tiersSection = collSection.getConfigurationSection("tiers");
        if (tiersSection != null) {
            for (String tierKey : tiersSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(tierKey);
                    int req = tiersSection.getInt(tierKey + ".requirement");
                    List<String> rewards = tiersSection.getStringList(tierKey + ".rewards");
                    List<String> displayRewards = tiersSection.getStringList(tierKey + ".display-rewards");
                    collection.addTier(new Tier(level, req, rewards, displayRewards));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Register
        collectionsById.put(collKey, collection);
        for (Material m : materialList) collectionsByMaterial.put(m, collection);
        if (mmoId != null && !mmoId.isEmpty()) collectionsByMmoId.put(mmoId.toUpperCase(), collection);

        // Add to Category
        String catKey = categoryKey.toUpperCase();
        Category category = categories.get(catKey);
        if (category == null) {
            ensureCategoryExists(catKey, null);
            category = categories.get(catKey);
        }
        category.addCollection(collection);
    }

    public Map<String, Category> getCategories() {
        return categories;
    }

    public Collection getCollection(String id) {
        if (id == null) return null;
        return collectionsById.get(id.trim().toLowerCase());
    }

    public Collection getCollectionByMaterial(Material material) {
        return collectionsByMaterial.get(material);
    }

    public Collection getCollectionByMmoId(String mmoId) {
        if (mmoId == null) return null;
        return collectionsByMmoId.get(mmoId.toUpperCase());
    }
}
