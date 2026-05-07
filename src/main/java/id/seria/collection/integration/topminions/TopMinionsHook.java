package id.seria.collection.integration.topminions;

import com.sarry20.topminion.models.minionConfig.MinionConfigClass;
import com.sarry20.topminion.models.minionConfig.MinionConfigClassManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TopMinionsHook {

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("TopMinion");
    }

    public static List<MinionConfigClass> getMinionTypes() {
        if (!isAvailable()) return new ArrayList<>();
        return MinionConfigClassManager.getConfigClassList();
    }

    /**
     * Memberikan permission milestone minion kepada player.
     * Permission format: seria.topminion.<level>
     * @param player Player yang menerima reward
     * @param level Level milestone (1-10)
     */
    public static void setMilestonePermission(Player player, int level) {
        if (!isAvailable() || level <= 0) return;
        
        // Sebelum memberikan level yang baru, kita harus menghapus level sebelumnya (opsional tapi disarankan)
        // Namun LuckPerms 'set' biasanya cukup jika kita ingin menimpa. 
        // Jika kita ingin player hanya punya satu level tertinggi, kita bisa hapus level level-1.
        
        String permission = "seria.topminion." + level;
        String command = "lp user " + player.getName() + " permission set " + permission + " true";
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
