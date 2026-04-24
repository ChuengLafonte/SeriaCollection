package id.seria.collection.listeners;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Collection;
import io.lumine.mythic.lib.api.crafting.event.MythicCraftItemEvent;
import io.lumine.mythic.lib.api.crafting.recipes.MythicRecipeBlueprint;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class MMOItemsCraftListener implements Listener {

    private final SeriaCollectionPlugin plugin;

    public MMOItemsCraftListener(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (!(e.getView().getPlayer() instanceof Player player)) return;
        
        ItemStack result = e.getInventory().getResult();
        if (result == null || result.getType().isAir()) return;

        Type type = null;
        String id = null;

        // Try getting from NBT first
        NBTItem nbt = NBTItem.get(result);
        if (nbt.hasTag("MMOITEMS_ITEM_TYPE")) {
            type = MMOItems.getType(nbt);
            id = MMOItems.getID(nbt);
        }

        // Fallback: Check Recipe Key (Tembak API MMOItems via NamespacedKey)
        if (type == null && e.getRecipe() instanceof org.bukkit.Keyed keyed) {
            org.bukkit.NamespacedKey key = keyed.getKey();
            if (key.getNamespace().equalsIgnoreCase("mmoitems")) {
                String[] parts = key.getKey().split("_");
                if (parts.length >= 3) {
                    // Format: type_id_number (e.g. shaped_miscellaneous_enchanted_oak_log_1)
                    type = MMOItems.plugin.getTypes().get(parts[1].toUpperCase());
                    // Reconstruct ID if it contains underscores
                    StringBuilder idBuilder = new StringBuilder();
                    for (int i = 2; i < parts.length - 1; i++) {
                        if (i > 2) idBuilder.append("_");
                        idBuilder.append(parts[i]);
                    }
                    id = idBuilder.toString().toUpperCase();
                    // player.sendMessage("§7[Debug] Identified via Recipe Key: " + type.getId() + " - " + id);
                }
            }
        }

        if (type == null || id == null) return;

        MMOItemTemplate template = MMOItems.plugin.getTemplates().getTemplate(type, id);
        if (template == null) return;

        // Use the stat instance to get the data (Check for CUSTOM_SCOLLECT_TIER)
        ItemStat<?, ?> stat = MMOItems.plugin.getStats().get("CUSTOM_SCOLLECT_TIER");
        if (stat == null) {
            return;
        }

        // Build the item momentarily to read its stats
        MMOItem mmoItem = template.newBuilder(0, null).build();
        if (!mmoItem.hasData(stat)) {
            return;
        }

        String requirement = ((StringData) mmoItem.getData(stat)).toString();

        if (requirement == null || requirement.isEmpty()) return;

        // Format: "collection_id - level"
        String[] parts = requirement.split(" - ");
        if (parts.length < 2) return;

        String collectionId = parts[0].trim().toLowerCase();
        int requiredTier;
        try {
            requiredTier = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException ex) {
            return;
        }

        Collection collection = plugin.getCollectionManager().getCollection(collectionId);
        if (collection == null) return;

        int currentTier = plugin.getPlayerDataManager().getTierLevel(player.getUniqueId(), collectionId);

        if (currentTier < requiredTier) {
            // Block the item from appearing for standard crafting
            e.getInventory().setResult(new ItemStack(Material.AIR));
        }

        // Delay 1 tick to also intercept MythicLib's custom recipe injection
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack lateResult = e.getInventory().getResult();
            if (lateResult == null || lateResult.getType().isAir()) return;
            
            NBTItem lateNbt = NBTItem.get(lateResult);
            if (!lateNbt.hasTag("MMOITEMS_ITEM_ID")) return;
            
            Type lateType = MMOItems.getType(lateNbt);
            String lateId = MMOItems.getID(lateNbt);
            
            ItemStat<?, ?> lateStat = MMOItems.plugin.getStats().get("CUSTOM_SCOLLECT_TIER");
            if (lateStat == null) return;
            
            MMOItemTemplate lateTemplate = MMOItems.plugin.getTemplates().getTemplate(lateType, lateId);
            if (lateTemplate == null) return;
            
            MMOItem lateMmoItem = lateTemplate.newBuilder(0, null).build();
            if (!lateMmoItem.hasData(lateStat)) return;

            String lateReq = ((StringData) lateMmoItem.getData(lateStat)).toString();
            String[] lateParts = lateReq.split(" - ");
            if (lateParts.length < 2) return;

            String lateColId = lateParts[0].trim().toLowerCase();
            int lateTier = Integer.parseInt(lateParts[1].trim());

            int lateCurrentTier = plugin.getPlayerDataManager().getTierLevel(player.getUniqueId(), lateColId);
            
            if (lateCurrentTier < lateTier) {
                e.getInventory().setResult(new ItemStack(Material.AIR));
                player.updateInventory(); // Force client update
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        
        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType().isAir()) return;

        NBTItem nbt = NBTItem.get(result);
        if (!nbt.hasTag("MMOITEMS_ITEM_ID")) return;

        Type type = MMOItems.getType(nbt);
        String id = MMOItems.getID(nbt);
        
        ItemStat<?, ?> stat = MMOItems.plugin.getStats().get("CUSTOM_SCOLLECT_TIER");
        if (stat == null) return;

        MMOItem mmoItem = MMOItems.plugin.getTemplates().getTemplate(type, id).newBuilder(0, null).build();
        if (!mmoItem.hasData(stat)) return;

        String requirement = ((StringData) mmoItem.getData(stat)).toString();
        String[] parts = requirement.split(" - ");
        if (parts.length < 2) return;

        String collectionId = parts[0].trim().toLowerCase();
        int requiredTier = Integer.parseInt(parts[1].trim());

        int currentTier = plugin.getPlayerDataManager().getTierLevel(player.getUniqueId(), collectionId);

        if (currentTier < requiredTier) {
            e.setCancelled(true);
        }
    }



    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;
        
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            // Check cursor if result is being picked up?
            // item = e.getCursor(); 
            // if (item == null || item.getType().isAir()) return;
            return;
        }

        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasTag("MMOITEMS_ITEM_ID")) return;

        Type type = MMOItems.getType(nbt);
        String id = MMOItems.getID(nbt);
        
        ItemStat<?, ?> stat = MMOItems.plugin.getStats().get("CUSTOM_SCOLLECT_TIER");
        if (stat == null) return;

        MMOItem mmoItem = MMOItems.plugin.getTemplates().getTemplate(type, id).newBuilder(0, null).build();
        if (!mmoItem.hasData(stat)) return;

        String requirement = ((StringData) mmoItem.getData(stat)).toString();
        String[] parts = requirement.split(" - ");
        if (parts.length < 2) return;

        String collectionId = parts[0].trim().toLowerCase();
        int requiredTier = Integer.parseInt(parts[1].trim());

        int currentTier = plugin.getPlayerDataManager().getTierLevel(player.getUniqueId(), collectionId);

        if (currentTier < requiredTier) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMythicCraft(MythicCraftItemEvent e) {
        Player player = (Player) e.getTrigger().getWhoClicked();
        MythicRecipeBlueprint blueprint = e.getCache().getOperation();
        if (blueprint == null || blueprint.getNk() == null) return;
        
        String keyStr = blueprint.getNk().getKey();
        String[] parts = keyStr.split("_");
        if (parts.length < 3) return;

        Type type = MMOItems.plugin.getTypes().get(parts[1].toUpperCase());
        if (type == null) return;

        StringBuilder idBuilder = new StringBuilder();
        for (int i = 2; i < parts.length - 1; i++) {
            if (i > 2) idBuilder.append("_");
            idBuilder.append(parts[i]);
        }
        String id = idBuilder.toString().toUpperCase();

        ItemStat<?, ?> stat = MMOItems.plugin.getStats().get("CUSTOM_SCOLLECT_TIER");
        if (stat == null) return;

        MMOItemTemplate template = MMOItems.plugin.getTemplates().getTemplate(type, id);
        if (template == null) return;

        MMOItem mmoItem = template.newBuilder(0, null).build();
        if (!mmoItem.hasData(stat)) return;

        String requirement = ((StringData) mmoItem.getData(stat)).toString();
        String[] partsReq = requirement.split(" - ");
        if (partsReq.length < 2) return;

        String collectionId = partsReq[0].trim().toLowerCase();
        int requiredTier = Integer.parseInt(partsReq[1].trim());

        int currentTier = plugin.getPlayerDataManager().getTierLevel(player.getUniqueId(), collectionId);

        if (currentTier < requiredTier) {
            e.setCancelled(true);
        }
    }
}
