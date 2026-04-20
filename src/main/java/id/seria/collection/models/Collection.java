package id.seria.collection.models;

import org.bukkit.Material;
import java.util.Map;
import java.util.TreeMap;

public class Collection {
    private final String id;
    private final String name;
    private final Material material;
    private final String mmoitemId;
    private final Map<Integer, Tier> tiers;

    public Collection(String id, String name, Material material, String mmoitemId) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.mmoitemId = mmoitemId;
        this.tiers = new TreeMap<>(); // Sorted by tier level
    }

    public void addTier(Tier tier) {
        tiers.put(tier.getLevel(), tier);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Material getMaterial() { return material; }
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
