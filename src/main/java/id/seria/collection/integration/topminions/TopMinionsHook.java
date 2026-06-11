package id.seria.collection.integration.topminions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Method;

public class TopMinionsHook {

    private static Class<?> spawnItemManagerClass;
    private static Method getSpawnerItemMethod;
    private static boolean initialized = false;

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("TopMinion");
    }

    private static void initReflection() {
        if (initialized) return;
        initialized = true;
        if (isAvailable()) {
            try {
                spawnItemManagerClass = Class.forName("com.sarry20.topminion.models.spawnItem.SpawnItemManager");
                getSpawnerItemMethod = spawnItemManagerClass.getMethod("getSpawnerItem", String.class, String.class, String.class);
            } catch (Exception ignored) {}
        }
    }

    public static ItemStack getSpawnerItem(String type, String mat, String lvl) {
        if (!isAvailable()) return null;
        initReflection();
        if (getSpawnerItemMethod != null) {
            try {
                return (ItemStack) getSpawnerItemMethod.invoke(null, type, mat, lvl);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Memberikan permission milestone minion kepada player.
     * Permission format: seria.topminion.<level>
     * @param player Player yang menerima reward
     * @param level Level milestone (1-10)
     */
    public static void setMilestonePermission(Player player, int level) {
        if (!isAvailable() || level <= 0) return;
        
        String permission = "seria.topminion." + level;
        String command = "lp user " + player.getName() + " permission set " + permission + " true";
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
