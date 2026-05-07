package id.seria.collection.commands;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.inventory.RecipeBookMainMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RecipeCommand implements CommandExecutor {

    private final SeriaCollectionPlugin plugin;

    public RecipeCommand(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Hanya pemain yang bisa menggunakan command ini!");
            return true;
        }

        new RecipeBookMainMenu(plugin).open(player);
        return true;
    }
}
