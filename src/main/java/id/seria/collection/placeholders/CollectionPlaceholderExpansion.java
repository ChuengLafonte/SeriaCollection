package id.seria.collection.placeholders;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Collection;
import id.seria.collection.models.Tier;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CollectionPlaceholderExpansion extends PlaceholderExpansion {

    private final SeriaCollectionPlugin plugin;

    public CollectionPlaceholderExpansion(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "seriacollection";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Seria";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister it on reload
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        // %seriacollection_level_<id>%
        if (params.startsWith("level_")) {
            String collId = params.replace("level_", "");
            Collection collection = plugin.getCollectionManager().getCollection(collId);
            if (collection == null) return "0";
            
            int amount = plugin.getPlayerDataManager().getAmount(player.getUniqueId(), collId);
            int currentLevel = 0;
            for (Tier tier : collection.getTiers().values()) {
                if (amount >= tier.getRequirement()) {
                    currentLevel = tier.getLevel();
                } else {
                    break;
                }
            }
            return String.valueOf(currentLevel);
        }

        // %seriacollection_amount_<id>%
        if (params.startsWith("amount_")) {
            String collId = params.replace("amount_", "");
            return String.valueOf(plugin.getPlayerDataManager().getAmount(player.getUniqueId(), collId));
        }

        return null;
    }
}
