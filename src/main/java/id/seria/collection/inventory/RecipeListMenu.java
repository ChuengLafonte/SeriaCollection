package id.seria.collection.inventory;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Collection;
import id.seria.collection.utils.GuiUtils;
import id.seria.crafting.recipes.SeriaRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.inventory.InventoryHolder;

public class RecipeListMenu implements InventoryHolder {

    private final SeriaCollectionPlugin plugin;
    private final String category;
    private Inventory inventory;

    public RecipeListMenu(SeriaCollectionPlugin plugin, String category) {
        this.plugin = plugin;
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        open(player, category, 0);
    }

    public void open(Player player, String category, int page) {
        List<SeriaRecipe> allRecipes = plugin.getRecipeBookManager().getRecipesByCategory().getOrDefault(category.toUpperCase(), new ArrayList<>());
        
        // Filter agar unik berdasarkan MMO ID agar tidak ada duplikat di menu
        List<SeriaRecipe> recipes = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (SeriaRecipe r : allRecipes) {
            String uniqueKey = r.getMmoId() + ":" + r.getMinionType() + ":" + r.getMinionLevel();
            if (seenIds.add(uniqueKey)) {
                recipes.add(r);
            }
        }

        int maxPage = (int) Math.ceil(recipes.size() / 28.0);
        if (maxPage == 0) maxPage = 1;

        String title = plugin.getConfigManager().getGuisConfig().getString("recipe-book.list-menu.title", "<dark_grey><b>|</b> <dark_grey>(%page%/%max_page%) %category%")
                .replace("%page%", String.valueOf(page + 1))
                .replace("%max_page%", String.valueOf(maxPage))
                .replace("%category%", category);
        
        this.inventory = Bukkit.createInventory(this, 54, GuiUtils.format(title));
        Inventory inv = this.inventory;
        
        ItemStack filler = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        
        int start = page * 28;
        int slot = 10;
        
        for (int i = start; i < recipes.size() && slot < 44; i++) {
            while (slot % 9 == 0 || slot % 9 == 8) slot++;
            if (slot >= 44) break;

            SeriaRecipe recipe = recipes.get(i);
            boolean unlocked = plugin.getRecipeBookManager().isUnlocked(player, recipe);
            
            ItemStack display = plugin.getRecipeBookManager().getOutputItem(recipe);
            if (unlocked && display != null) {
                display = display.clone();
                ItemMeta meta = display.getItemMeta();
                List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
                lore.add(GuiUtils.format(""));
                lore.add(GuiUtils.format("<yellow>Click to view recipes!"));
                meta.lore(lore);
                display.setItemMeta(meta);
            } else if (unlocked && display == null) {
                display = GuiUtils.createItem(Material.BARRIER, "<red>ERROR: Item Missing", 
                    "<gray>Resep ID: <white>" + recipe.getMmoId(),
                    "<gray>Pastikan item ini ada di MMOItems!");
            } else {
                String collName = recipe.getRequireCollectionId();
                Collection coll = plugin.getCollectionManager().getCollection(collName);
                if (coll != null) collName = coll.getName();
                
                display = GuiUtils.createItem(Material.COAL, "<red>???", List.of(
                    GuiUtils.format("<gray>Unlock this recipe by reaching:"),
                    GuiUtils.format("<red>" + collName + " Tier " + recipe.getRequireCollectionTier())
                ));
            }

            inv.setItem(slot, display);
            slot++;
        }

        inv.setItem(48, GuiUtils.createItem(Material.ARROW, "<yellow>Previous Page"));
        inv.setItem(49, GuiUtils.createItem(Material.BARRIER, "<red>Back to Menu"));
        inv.setItem(50, GuiUtils.createItem(Material.ARROW, "<yellow>Next Page"));

        player.openInventory(inv);
    }
}
