package id.seria.collection.inventory;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Category;
import id.seria.collection.models.Collection;
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

public class RecipeDetailMenu implements InventoryHolder {

    private final SeriaCollectionPlugin plugin;
    private Inventory inventory;
    private List<SeriaRecipe> recipes;
    private int currentIndex;
    private String category;

    public RecipeDetailMenu(SeriaCollectionPlugin plugin, String category) {
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

    public List<SeriaRecipe> getRecipes() {
        return recipes;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void open(Player player, SeriaRecipe recipe) {
        // Cari semua resep yang menghasilkan item yang sama
        List<SeriaRecipe> matching = plugin.getRecipeBookManager().getRecipesByOutput(plugin.getRecipeBookManager().getOutputItem(recipe));
        
        // Security Fix: Filter hanya yang sudah di-unlock
        List<SeriaRecipe> unlockedOnly = new ArrayList<>();
        for (SeriaRecipe r : matching) {
            if (plugin.getRecipeBookManager().isUnlocked(player, r)) {
                unlockedOnly.add(r);
            }
        }
        
        open(player, unlockedOnly, 0);
    }

    public void open(Player player, List<SeriaRecipe> recipes, int index) {
        if (recipes == null || recipes.isEmpty()) return;
        this.recipes = recipes;
        this.currentIndex = index;
        SeriaRecipe recipe = recipes.get(index);

        String resultName = "";
        if (recipe.isMinionRecipe()) {
            resultName = RecipeBookManager.formatName(recipe.getMinionMaterial()) + " Minion " + GuiUtils.toRoman(recipe.getMinionLevel());
        } else {
            resultName = RecipeBookManager.formatName(recipe.getMmoId());
        }

        String title = plugin.getConfigManager().getGuisConfig().getString("recipe-book.detail-menu.title", "<dark_gray>Recipe: %item%")
                .replace("%item%", resultName);
        
        // Tambahkan (1/2) ke judul jika ada lebih dari satu resep
        if (recipes.size() > 1) {
            title += " <gray>(" + (index + 1) + "/" + recipes.size() + ")";
        }

        this.inventory = Bukkit.createInventory(this, 54, GuiUtils.format(title));
        Inventory inv = this.inventory;
        
        // Filler
        ItemStack filler = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // Recipe Grid
        int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        ItemStack[] ingredients = plugin.getRecipeBookManager().getIngredientItems(recipe);
        
        for (int i = 0; i < 9; i++) {
            ItemStack ingredient = ingredients[i];
            if (ingredient == null || ingredient.getType().isAir()) {
                inv.setItem(gridSlots[i], null);
            } else {
                // Tambahkan petunjuk jika item bisa diklik untuk melihat resepnya
                ItemStack display = ingredient.clone();
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
                    SeriaRecipe subRecipe = plugin.getRecipeBookManager().getFirstRecipeForItem(ingredient);
                    if (subRecipe != null) {
                        lore.add(GuiUtils.format(""));
                        if (plugin.getRecipeBookManager().isUnlocked(player, subRecipe)) {
                            lore.add(GuiUtils.format("<yellow>Click to view recipe!"));
                        } else {
                            String collName = subRecipe.getRequireCollectionId();
                            Collection coll = plugin.getCollectionManager().getCollection(collName);
                            if (coll != null) collName = coll.getName();
                            
                            lore.add(GuiUtils.format("<red>Unlock " + collName + " " + GuiUtils.toRoman(subRecipe.getRequireCollectionTier())));
                        }
                    }
                    meta.lore(lore);
                    display.setItemMeta(meta);
                }
                inv.setItem(gridSlots[i], display);
            }
        }

        // Result Slot
        inv.setItem(24, plugin.getRecipeBookManager().getOutputItem(recipe));

        // Navigation
        if (recipes.size() > 1) {
            if (index > 0) {
                inv.setItem(18, GuiUtils.createItem(Material.ARROW, "<yellow>Previous Recipe"));
            }
            if (index < recipes.size() - 1) {
                inv.setItem(26, GuiUtils.createItem(Material.ARROW, "<yellow>Next Recipe"));
            }
        }

        // Back button
        inv.setItem(49, GuiUtils.createItem(Material.ARROW, "<yellow>Back"));

        player.openInventory(inv);
    }
}
