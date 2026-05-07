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
        
        // OPTIMIZATION: Check taint first before doing anything else
        // This makes the listener near-zero cost for already-processed items (Tree, Farm, Mining, Combat)
        if (plugin.getPlayerDataManager().isTainted(e.getItem())) {
            return;
        }

        plugin.getPlayerDataManager().handleCollectionGain(player, e.getItem());
        
        // Taint after gain to prevent double counting if dropped again
        plugin.getPlayerDataManager().taintEntity(e.getItem());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;

        java.util.Collection<ItemStack> drops = e.getBlock().getDrops(player.getInventory().getItemInMainHand());
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir()) continue;
            plugin.getPlayerDataManager().handleCollectionGain(player, drop);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        // 1. Award points for all drops directly to the killer
        // 2. Taint the ItemStacks so they are ignored by pickup listener
        for (ItemStack drop : e.getDrops()) {
            if (drop == null || drop.getType().isAir()) continue;
            plugin.getPlayerDataManager().handleCollectionGain(killer, drop);
            plugin.getPlayerDataManager().taintItem(drop);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        plugin.getPlayerDataManager().taintEntity(e.getItemDrop());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDrop(org.bukkit.event.block.BlockDropItemEvent e) {
        for (org.bukkit.entity.Item itemEntity : e.getItems()) {
            plugin.getPlayerDataManager().taintEntity(itemEntity);
        }
    }

}
