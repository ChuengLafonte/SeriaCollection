package id.seria.collection.commands;

import id.seria.collection.SeriaCollectionPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CollectCommand implements CommandExecutor {

    private final SeriaCollectionPlugin plugin;

    public CollectCommand(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        plugin.getGuiManager().openMainMenu(player);
        return true;
    }
}
