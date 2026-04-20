package id.seria.collection.managers;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.inventory.CollectionMainMenu;
import org.bukkit.entity.Player;

public class GuiManager {

    private final SeriaCollectionPlugin plugin;

    public GuiManager(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        new CollectionMainMenu(plugin).open(player);
    }
}
