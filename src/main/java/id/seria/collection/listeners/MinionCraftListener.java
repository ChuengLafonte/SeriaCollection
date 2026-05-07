package id.seria.collection.listeners;

import com.sarry20.topminion.event.minion.MinionUpgradeEvent;
import id.seria.collection.SeriaCollectionPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MinionCraftListener implements Listener {

    private final SeriaCollectionPlugin plugin;

    public MinionCraftListener(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMinionUpgrade(MinionUpgradeEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        // Based on TopMinion API, event.getMinionObj() returns the minion instance
        String minionId = event.getMinionObj().getType();
        int newTier = event.getMinionObj().getLevel();

        SeriaCollectionPlugin.debug("MinionUpgrade detected: " + player.getName() + " crafted " + minionId + " Tier " + newTier);
        
        plugin.getPlayerDataManager().handleMinionCraft(player, minionId, newTier);
    }
}
