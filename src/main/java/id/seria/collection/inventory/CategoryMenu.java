package id.seria.collection.inventory;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Category;
import id.seria.collection.models.Collection;
import id.seria.collection.models.Tier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CategoryMenu implements InventoryHolder, Listener {

    private final SeriaCollectionPlugin plugin;
    private final Category category;
    private final Inventory inventory;

    public CategoryMenu(SeriaCollectionPlugin plugin, Category category) {
        this.plugin = plugin;
        this.category = category;
        this.inventory = Bukkit.createInventory(this, 54, SeriaCollectionPlugin.getMiniMessage().deserialize(category.getName()));
    }

    public void open(Player player) {
        setupItems(player);
        player.openInventory(inventory);
    }

    private void setupItems(Player player) {
        inventory.clear();
        
        // Fill glass
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.empty());
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 54; i++) inventory.setItem(i, glass);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(SeriaCollectionPlugin.getMiniMessage().deserialize("<red>Go Back"));
        back.setItemMeta(backMeta);
        inventory.setItem(49, back);

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        
        int i = 0;
        for (Collection collection : category.getCollections().values()) {
            if (i >= slots.length) break;
            
            int amount = plugin.getPlayerDataManager().getAmount(player.getUniqueId(), collection.getId());
            Tier nextTier = collection.getNextTier(amount);
            
            ItemStack item = new ItemStack(collection.getMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(SeriaCollectionPlugin.getMiniMessage().deserialize("<yellow>" + collection.getName()));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray>Progress: <yellow>" + amount));
            
            if (nextTier != null) {
                lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray>Next Tier: <white>" + nextTier.getLevel()));
                lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray>Requirement: <yellow>" + amount + " <gray>/ <white>" + nextTier.getRequirement()));
                lore.add(Component.empty());
                lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<green>Click to view details!"));
            } else {
                lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<green>MAXED OUT!"));
            }
            
            meta.lore(lore);
            item.setItemMeta(meta);
            
            inventory.setItem(slots[i], item);
            i++;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof CategoryMenu) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            
            if (event.getSlot() == 49) {
                new CollectionMainMenu(plugin).open(player);
                return;
            }

            // Find collection by material
            for (Collection coll : category.getCollections().values()) {
                if (clicked.getType() == coll.getMaterial()) {
                    new CollectionDetailMenu(plugin, coll).open(player);
                    break;
                }
            }
        }
    }
}
