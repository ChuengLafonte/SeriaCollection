package id.seria.collection.models;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Collection {
    private final String id;
    private final String name;
    private final List<Material> materials;
    private final String mmoitemId;
    private final Map<Integer, Tier> tiers;

    public Collection(String id, String name, List<Material> materials, String mmoitemId) {
        this.id = id;
        this.name = name;
        this.materials = materials != null ? materials : new ArrayList<>();
        this.mmoitemId = mmoitemId;
        this.tiers = new TreeMap<>(); // Sorted by tier level
    }

    // Deprecated constructor for compatibility (if needed)
    public Collection(String id, String name, Material material, String mmoitemId) {
        this.id = id;
        this.name = name;
        this.materials = new ArrayList<>();
        if (material != null) this.materials.add(material);
        this.mmoitemId = mmoitemId;
        this.tiers = new TreeMap<>();
    }

    public void addTier(Tier tier) {
        tiers.put(tier.getLevel(), tier);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<Material> getMaterials() { return materials; }
    
    // Helper to get first material (e.g. for icon)
    public Material getMaterial() { 
        return materials.isEmpty() ? Material.BARRIER : materials.get(0); 
    }
    
    public String getMmoitemId() { return mmoitemId; }
    public Map<Integer, Tier> getTiers() { return tiers; }

    public Tier getTier(int level) {
        return tiers.get(level);
    }

    public Tier getNextTier(int currentAmount) {
        for (Tier tier : tiers.values()) {
            if (currentAmount < tier.getRequirement()) {
                return tier;
            }
        }
        return null; // Maxed out
    }
}
