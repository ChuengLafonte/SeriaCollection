package id.seria.collection.inventory;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.managers.RecipeBookManager;
import id.seria.collection.utils.GuiUtils;
import id.seria.crafting.recipes.SeriaRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

public class RecipeBookMainMenu implements InventoryHolder {

    private final SeriaCollectionPlugin plugin;
    private Inventory inventory;

    public RecipeBookMainMenu(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        int size = plugin.getConfigManager().getGuisConfig().getInt("recipe-book.main-menu.size", 27);
        this.inventory = Bukkit.createInventory(this, size, GuiUtils.format(plugin.getConfigManager().getGuisConfig().getString("recipe-book.main-menu.title", "<dark_gray>Recipe Book")));
        Inventory inv = this.inventory;
        
        // Filler
        ItemStack filler = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        // 1. Load Info Item
        org.bukkit.configuration.ConfigurationSection infoSec = plugin.getConfigManager().getGuisConfig().getConfigurationSection("recipe-book.main-menu.info");
        if (infoSec != null) {
            int slot = infoSec.getInt("slot");
            if (slot < size) {
                inv.setItem(slot, createInfoItem(player, infoSec));
            }
        }

        // 2. Load Categories from Config
        org.bukkit.configuration.ConfigurationSection categories = plugin.getConfigManager().getGuisConfig().getConfigurationSection("recipe-book.main-menu.categories");
        if (categories != null) {
            for (String key : categories.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection catSec = categories.getConfigurationSection(key);
                int slot = catSec.getInt("slot");
                String matStr = catSec.getString("material", "BARRIER");
                String name = catSec.getString("name");
                String desc = catSec.getString("description");
                
                ItemStack item;
                if (matStr.equalsIgnoreCase("PLAYER_HEAD") && catSec.contains("head-value")) {
                    item = GuiUtils.createCustomHead(catSec.getString("head-value"));
                } else {
                    item = new ItemStack(Material.valueOf(matStr.toUpperCase()));
                }

                if (slot < size) {
                    inv.setItem(slot, decorateCategoryItem(player, key.toUpperCase(), item, name, desc));
                }
            }
        }

        // Close button
        int closeSlot = plugin.getConfigManager().getGuisConfig().getInt("recipe-book.main-menu.close-slot", 22);
        if (closeSlot < size) {
            inv.setItem(closeSlot, GuiUtils.createItem(Material.BARRIER, "<red>Close"));
        }

        player.openInventory(inv);
    }

    private ItemStack createInfoItem(Player player, org.bukkit.configuration.ConfigurationSection sec) {
        String matStr = sec.getString("material", "BOOK");
        ItemStack item = new ItemStack(Material.valueOf(matStr.toUpperCase()));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(GuiUtils.format(sec.getString("name", "Recipe Book")));

        // Calculate Overall Progress
        List<SeriaRecipe> all = plugin.getRecipeBookManager().getAllRecipes();
        int total = all.size();
        int unlocked = 0;
        for (SeriaRecipe r : all) {
            if (plugin.getRecipeBookManager().isUnlocked(player, r)) unlocked++;
        }
        double percentage = total == 0 ? 100.0 : (double) unlocked / total * 100.0;

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        List<String> configLore = sec.getStringList("description");
        for (String line : configLore) {
            lore.add(GuiUtils.format(line.replace("%percentage%", String.format("%.1f", percentage))
                    .replace("%unlocked%", String.valueOf(unlocked))
                    .replace("%total%", String.valueOf(total))));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack decorateCategoryItem(Player player, String categoryId, ItemStack item, String name, String description) {
        RecipeBookManager.RecipeProgress progress = plugin.getRecipeBookManager().getProgress(player, categoryId);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(GuiUtils.format(name));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(GuiUtils.format(description));
        lore.add(GuiUtils.format(""));
        
        double percent = progress.getPercentage();
        lore.add(GuiUtils.format("<gray>Recipes Unlocked: <yellow>" + String.format("%.1f", percent) + "%"));
        lore.add(GuiUtils.format(GuiUtils.getProgressBar(progress.unlocked, progress.total, 20, '⬛', "<green>", "<gray>") + " <yellow>" + progress.unlocked + "/" + progress.total));
        lore.add(GuiUtils.format(""));
        lore.add(GuiUtils.format("<yellow>Click to view!"));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
