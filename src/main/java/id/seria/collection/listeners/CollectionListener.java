package id.seria.collection.listeners;

import id.seria.collection.SeriaCollectionPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.block.Container;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public class CollectionListener implements Listener {

    private final SeriaCollectionPlugin plugin;

    public CollectionListener(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.getPlayerDataManager().loadPlayerData(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getPlayerDataManager().unloadPlayerData(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        
        ItemStack item = e.getItem().getItemStack();
        plugin.getPlayerDataManager().handleCollectionGain(player, item);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        // Taint items when dropped by players
        plugin.getPlayerDataManager().taintItem(e.getItemDrop().getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent e) {
        // Tag items from containers as tainted
        if (e.getBlock().getState() instanceof Container) {
            for (Item itemEntity : e.getItems()) {
                ItemStack stack = itemEntity.getItemStack();
                plugin.getPlayerDataManager().taintItem(stack);
                itemEntity.setItemStack(stack);
            }
        }
    }

}
