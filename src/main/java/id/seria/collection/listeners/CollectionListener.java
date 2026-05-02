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
        
        plugin.getPlayerDataManager().handleCollectionGain(player, e.getItem());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;

        // If dropItems is false, it means a plugin (like BlockRegen) is handling the drops.
        // We calculate what would have dropped and award points now.
        if (!e.isDropItems()) {
            java.util.Collection<ItemStack> drops = e.getBlock().getDrops(player.getInventory().getItemInMainHand());
            for (ItemStack drop : drops) {
                if (drop == null || drop.getType().isAir()) continue;
                plugin.getPlayerDataManager().handleCollectionGain(player, drop);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        // Taint the ITEM ENTITY when dropped by players, not the ItemStack metadata.
        plugin.getPlayerDataManager().taintEntity(e.getItemDrop());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent e) {
        // Tag item entities from containers as tainted
        if (e.getBlock().getState() instanceof Container) {
            for (org.bukkit.entity.Item itemEntity : e.getItems()) {
                plugin.getPlayerDataManager().taintEntity(itemEntity);
            }
        }
    }

}
