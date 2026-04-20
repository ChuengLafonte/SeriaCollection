package id.seria.collection.models;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Category {
    private final String id;
    private final String name;
    private final Material icon;
    private final List<String> lore;
    private final Map<String, Collection> collections;

    public Category(String id, String name, Material icon, List<String> lore) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.collections = new HashMap<>();
    }

    public void addCollection(Collection collection) {
        collections.put(collection.getId(), collection);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Material getIcon() { return icon; }
    public List<String> getLore() { return lore; }
    public Map<String, Collection> getCollections() { return collections; }
}
