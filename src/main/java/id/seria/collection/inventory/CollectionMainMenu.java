package id.seria.collection.inventory;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Category;
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
import java.util.Map;

public class CollectionMainMenu implements InventoryHolder, Listener {

    private final SeriaCollectionPlugin plugin;
    private final Inventory inventory;

    public CollectionMainMenu(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, SeriaCollectionPlugin.getMiniMessage().deserialize("<dark_gray>Collection Menu"));
        
        // Register this instance as a listener temporarily or use a global manager
        // In SeriaFarm style, they often register GUI classes in onEnable. 
        // I will follow the pattern of creating a base menu if possible, but for now 
        // let's just make it work using local registration or a static check in a global listener.
    }

    public void open(Player player) {
        setupItems();
        player.openInventory(inventory);
    }

    private void setupItems() {
        inventory.clear();
        
        // Fill glass
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.empty());
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 54; i++) inventory.setItem(i, glass);

        Map<String, Category> categories = plugin.getCollectionManager().getCategories();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        
        int i = 0;
        for (Category category : categories.values()) {
            if (i >= slots.length) break;
            
            ItemStack item = new ItemStack(category.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(SeriaCollectionPlugin.getMiniMessage().deserialize(category.getName()));
            
            List<Component> lore = new ArrayList<>();
            for (String line : category.getLore()) {
                lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize(line));
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
        if (event.getInventory().getHolder() instanceof CollectionMainMenu) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            
            // Find category by item name (simple approach) or slot mapping
            Map<String, Category> categories = plugin.getCollectionManager().getCategories();
            for (Category category : categories.values()) {
                if (clicked.getType() == category.getIcon()) {
                    new CategoryMenu(plugin, category).open(player);
                    break;
                }
            }
        }
    }
}
