package id.seria.collection.commands;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Collection;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final SeriaCollectionPlugin plugin;

    public AdminCommand(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("seriacollection.admin")) {
            sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize("<yellow>SeriaCollection Admin Commands:"));
            sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray>/scollect reload"));
            sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray>/scollect set <player> <collection_id> <amount>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().reloadConfigs();
            sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(plugin.getConfigManager().getMessage("reload-success")));
            return true;
        }

        if (args[0].equalsIgnoreCase("set") && args.length >= 4) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(plugin.getConfigManager().getMessage("player-not-found")));
                return true;
            }

            String collId = args[2];
            Collection collection = plugin.getCollectionManager().getCollection(collId);
            if (collection == null) {
                sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(plugin.getConfigManager().getMessage("invalid-collection")));
                return true;
            }

            try {
                int amount = Integer.parseInt(args[3]);
                plugin.getPlayerDataManager().addAmount(target, collId, amount - plugin.getPlayerDataManager().getAmount(target.getUniqueId(), collId));
                
                String msg = plugin.getConfigManager().getMessage("set-success")
                        .replace("%player%", target.getName())
                        .replace("%item%", collection.getName())
                        .replace("%amount%", String.valueOf(amount));
                sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(msg));
            } catch (NumberFormatException e) {
                sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize("<red>Amount must be a number!"));
            }
            return true;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "set").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return null; // Return null for player list
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return new ArrayList<>(plugin.getCollectionManager().getCategories().values().stream()
                    .flatMap(c -> c.getCollections().keySet().stream())
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList()));
        }

        return new ArrayList<>();
    }
}
