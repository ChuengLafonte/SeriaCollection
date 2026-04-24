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
            sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray>/scollect reset <player> <collection_id|all>"));
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
                plugin.getPlayerDataManager().forceSetAmount(target, collId, amount);
                
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

        if (args[0].equalsIgnoreCase("forcetier") && args.length >= 4) {
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
                int tierLevel = Integer.parseInt(args[3]);
                id.seria.collection.models.Tier tier = collection.getTier(tierLevel);
                if (tier == null) {
                    sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize("<red>Tier " + tierLevel + " tidak tersedia untuk koleksi ini!"));
                    return true;
                }

                plugin.getPlayerDataManager().forceSetAmount(target, collId, tier.getRequirement());
                
                String msg = plugin.getConfigManager().getMessage("forcetier-success");
                if (msg.equals("forcetier-success")) {
                    msg = "<prefix><red>Missing message: forcetier-success";
                }
                msg = msg.replace("%player%", target.getName())
                        .replace("%item%", collection.getName())
                        .replace("%tier%", String.valueOf(tierLevel));
                sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(msg));
            } catch (NumberFormatException e) {
                sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize("<red>Tier level must be a number!"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reset") && args.length >= 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(plugin.getConfigManager().getMessage("player-not-found")));
                return true;
            }

            String collId = args[2];
            if (collId.equalsIgnoreCase("all")) {
                plugin.getPlayerDataManager().resetAllCollections(target);
                String msg = plugin.getConfigManager().getMessage("reset-all-success")
                        .replace("%player%", target.getName());
                sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(msg));
                return true;
            }

            Collection collection = plugin.getCollectionManager().getCollection(collId);
            if (collection == null) {
                sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(plugin.getConfigManager().getMessage("invalid-collection")));
                return true;
            }

            plugin.getPlayerDataManager().resetCollection(target, collId);
            String msg = plugin.getConfigManager().getMessage("reset-success")
                    .replace("%player%", target.getName())
                    .replace("%item%", collection.getName());
            sender.sendMessage(SeriaCollectionPlugin.getMiniMessage().deserialize(msg));
            return true;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "set", "forcetier", "reset").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("forcetier") || args[0].equalsIgnoreCase("reset"))) {
            return null; // Return null for player list
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("forcetier") || args[0].equalsIgnoreCase("reset"))) {
            List<String> suggestions = new ArrayList<>();
            if (args[0].equalsIgnoreCase("reset")) {
                suggestions.add("all");
            }
            suggestions.addAll(plugin.getCollectionManager().getCategories().values().stream()
                    .flatMap(c -> c.getCollections().keySet().stream())
                    .collect(Collectors.toList()));
            
            return suggestions.stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("forcetier")) {
            Collection collection = plugin.getCollectionManager().getCollection(args[2]);
            if (collection != null) {
                return collection.getTiers().keySet().stream()
                        .map(String::valueOf)
                        .filter(s -> s.startsWith(args[3]))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
