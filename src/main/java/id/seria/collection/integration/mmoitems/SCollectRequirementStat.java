package id.seria.collection.integration.mmoitems;

import net.Indyuce.mmoitems.stat.type.StringStat;
import org.bukkit.Material;

/**
 * Custom MMOItems stat that defines the SeriaCollection requirement.
 * Format: "collection_id - level" (e.g. "oak_log - 1")
 */
public class SCollectRequirementStat extends StringStat {

    public SCollectRequirementStat() {
        super("SCOLLECT_TIER", 
              Material.BOOK, 
              "Collection Requirement", 
              new String[] { "§7The required collection tier to", "§7craft this item.", "§7Format: §fcollection_id - level" }, 
              new String[] { "all" });
    }
}
