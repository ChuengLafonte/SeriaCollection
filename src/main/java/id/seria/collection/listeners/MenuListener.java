package id.seria.collection.listeners;

import id.seria.collection.inventory.CollectionDetailMenu;
import id.seria.collection.inventory.CollectionMainMenu;
import id.seria.collection.inventory.CategoryMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof CollectionMainMenu) {
            ((CollectionMainMenu) holder).onInventoryClick(event);
        } else if (holder instanceof CategoryMenu) {
            ((CategoryMenu) holder).onInventoryClick(event);
        } else if (holder instanceof CollectionDetailMenu) {
            ((CollectionDetailMenu) holder).onInventoryClick(event);
        }
    }
}
