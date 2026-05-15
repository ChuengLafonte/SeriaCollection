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

        // %seriacollection_level_<id>% or %seriacollection_tier_<id>%
        if (params.startsWith("level_") || params.startsWith("tier_")) {
            String collId = params.replace("level_", "").replace("tier_", "").toLowerCase();
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
            String collId = params.replace("amount_", "").toLowerCase();
            return String.valueOf(plugin.getPlayerDataManager().getAmount(player.getUniqueId(), collId));
        }

        // %seriacollection_name_<id>%
        if (params.startsWith("name_")) {
            String collId = params.replace("name_", "").toLowerCase();
            Collection collection = plugin.getCollectionManager().getCollection(collId);
            return (collection != null) ? collection.getName() : "Unknown";
        }

        // %seriacollection_progress_bar_<id>%
        if (params.startsWith("progress_bar_")) {
            String collId = params.replace("progress_bar_", "").toLowerCase();
            Collection collection = plugin.getCollectionManager().getCollection(collId);
            if (collection == null) return "";

            int amount = plugin.getPlayerDataManager().getAmount(player.getUniqueId(), collId);
            Tier nextTier = collection.getNextTier(amount);
            if (nextTier == null) return id.seria.collection.utils.GuiUtils.getProgressBar(100, 100);

            return id.seria.collection.utils.GuiUtils.getProgressBar(amount, nextTier.getRequirement());
        }

        // %seriacollection_percent_<id>%
        if (params.startsWith("percent_")) {
            String collId = params.replace("percent_", "").toLowerCase();
            Collection collection = plugin.getCollectionManager().getCollection(collId);
            if (collection == null) return "0";

            int amount = plugin.getPlayerDataManager().getAmount(player.getUniqueId(), collId);
            Tier nextTier = collection.getNextTier(amount);
            if (nextTier == null) return "100";

            return String.valueOf((int) (((double) amount / nextTier.getRequirement()) * 100));
        }

        // %seriacollection_requirement_<id>%
        if (params.startsWith("requirement_")) {
            String collId = params.replace("requirement_", "").toLowerCase();
            Collection collection = plugin.getCollectionManager().getCollection(collId);
            if (collection == null) return "0";

            int amount = plugin.getPlayerDataManager().getAmount(player.getUniqueId(), collId);
            Tier nextTier = collection.getNextTier(amount);
            return (nextTier != null) ? String.valueOf(nextTier.getRequirement()) : "0";
        }

        return null;
    }
}
