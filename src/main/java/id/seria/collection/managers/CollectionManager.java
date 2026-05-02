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
    private final Map<String, Collection> collectionsById = new HashMap<>();
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

        FileConfiguration config = plugin.getConfigManager().getCollectionsConfig();
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) return;

        for (String catKey : categoriesSection.getKeys(false)) {
            ConfigurationSection catSection = categoriesSection.getConfigurationSection(catKey);
            if (catSection == null) continue;

            String name = catSection.getString("name", catKey);
            Material icon = Material.BARRIER;
            try {
                icon = Material.valueOf(catSection.getString("icon", "BARRIER").toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid icon material for category " + catKey + ": " + catSection.getString("icon"));
            }
            List<String> lore = catSection.getStringList("lore");

            Category category = new Category(catKey, name, icon, lore);

            ConfigurationSection collsSection = catSection.getConfigurationSection("collections");
            if (collsSection != null) {
                for (String collKey : collsSection.getKeys(false)) {
                    ConfigurationSection collSection = collsSection.getConfigurationSection(collKey);
                    if (collSection == null) continue;

                    String collName = collSection.getString("name", collKey);
                    String mmoId = collSection.getString("mmoitem-id");
                    
                    List<Material> materialList = new ArrayList<>();
                    if (collSection.contains("material")) {
                        // Check if it's a list or a single string
                        if (collSection.isList("material")) {
                            for (String matStr : collSection.getStringList("material")) {
                                try {
                                    materialList.add(Material.valueOf(matStr.toUpperCase()));
                                } catch (IllegalArgumentException ignored) {}
                            }
                        } else {
                            try {
                                materialList.add(Material.valueOf(collSection.getString("material", "AIR").toUpperCase()));
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }

                    Collection collection = new Collection(collKey, collName, materialList, mmoId);

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

                    category.addCollection(collection);
                    collectionsById.put(collKey, collection);
                    
                    // Register all materials to this collection
                    for (Material m : materialList) {
                        collectionsByMaterial.put(m, collection);
                    }
                    
                    if (mmoId != null && !mmoId.isEmpty()) {
                        collectionsByMmoId.put(mmoId.toUpperCase(), collection);
                    }
                }
            }
            categories.put(catKey, category);
        }
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
