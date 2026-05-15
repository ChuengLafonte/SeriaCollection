package id.seria.collection.models;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Category {
    private final String id;
    private String name;
    private Material icon;
    private List<String> lore;
    private final Map<String, Collection> collections;

    public Category(String id, String name, Material icon, List<String> lore) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.collections = new java.util.LinkedHashMap<>();
    }

    public void addCollection(Collection collection) {
        collections.put(collection.getId(), collection);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Material getIcon() { return icon; }
    public List<String> getLore() { return lore; }
    public Map<String, Collection> getCollections() { return collections; }

    public void setName(String name) { this.name = name; }
    public void setIcon(Material icon) { this.icon = icon; }
    public void setLore(List<String> lore) { this.lore = lore; }
}
