package id.seria.collection.listeners;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.inventory.CategoryMenu;
import id.seria.collection.inventory.CollectionDetailMenu;
import id.seria.collection.inventory.CollectionMainMenu;
import id.seria.collection.inventory.MinionCraftedMenu;
import id.seria.collection.inventory.RecipeBookMainMenu;
import id.seria.collection.inventory.RecipeDetailMenu;
import id.seria.collection.inventory.RecipeListMenu;
import id.seria.collection.utils.GuiUtils;
import id.seria.crafting.recipes.SeriaRecipe;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MenuListener implements Listener {

    private final SeriaCollectionPlugin plugin = SeriaCollectionPlugin.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder == null) return;

        if (holder instanceof CollectionMainMenu ||
            holder instanceof CategoryMenu ||
            holder instanceof CollectionDetailMenu || 
            holder instanceof MinionCraftedMenu || 
            holder instanceof RecipeBookMainMenu || 
            holder instanceof RecipeListMenu || 
            holder instanceof RecipeDetailMenu) {
            
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            if (holder instanceof RecipeBookMainMenu) {
                handleMainMenuClick(player, clickedItem);
            } else if (holder instanceof RecipeListMenu) {
                handleListMenuClick(player, (RecipeListMenu) holder, clickedItem, event.getView().getTitle());
            } else if (holder instanceof RecipeDetailMenu) {
                handleDetailMenuClick(player, (RecipeDetailMenu) holder, clickedItem);
            } else if (holder instanceof CollectionMainMenu) {
                ((CollectionMainMenu) holder).onInventoryClick(event);
            } else if (holder instanceof CategoryMenu) {
                ((CategoryMenu) holder).onInventoryClick(event);
            } else if (holder instanceof CollectionDetailMenu) {
                handleCollectionDetailClick(player, (CollectionDetailMenu) holder, clickedItem, event.getSlot());
            } else if (holder instanceof MinionCraftedMenu) {
                ((MinionCraftedMenu) holder).onInventoryClick(event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof CollectionMainMenu ||
            holder instanceof CategoryMenu ||
            holder instanceof CollectionDetailMenu || 
            holder instanceof MinionCraftedMenu || 
            holder instanceof RecipeBookMainMenu || 
            holder instanceof RecipeListMenu || 
            holder instanceof RecipeDetailMenu) {
            
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    private void handleMainMenuClick(Player player, ItemStack item) {
        if (item.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = GuiUtils.stripColor(item.getItemMeta().getDisplayName());
            if (displayName.endsWith("Recipes")) {
                String category = displayName.replace(" Recipes", "").trim().toUpperCase();
                new RecipeListMenu(plugin, category).open(player, category, 0);
            }
        }
    }

    private void handleListMenuClick(Player player, RecipeListMenu menu, ItemStack item, String title) {
        String cleanTitle = GuiUtils.stripColor(title);
        String category = "OTHERS";
        int currentPage = 0;
        int maxPage = 1;
        
        try {
            // Parse title: "| (1/2) FARMING"
            String[] parts = cleanTitle.split(" ");
            category = parts[parts.length - 1].toUpperCase(); 
            String pageInfo = parts[1].replace("(", "").replace(")", ""); // "1/2"
            currentPage = Integer.parseInt(pageInfo.split("/")[0]) - 1;
            maxPage = Integer.parseInt(pageInfo.split("/")[1]);
        } catch (Exception ignored) {}

        if (item.getType() == Material.BARRIER) {
            new RecipeBookMainMenu(plugin).open(player);
            return;
        }

        if (item.getType() == Material.ARROW) {
            String name = GuiUtils.stripColor(item.getItemMeta().getDisplayName());
            if (name.contains("Next")) {
                if (currentPage + 1 < maxPage) {
                    menu.open(player, category, currentPage + 1);
                }
            } else if (name.contains("Previous")) {
                if (currentPage > 0) {
                    menu.open(player, category, currentPage - 1);
                }
            }
            return;
        }

        if (item.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        SeriaCollectionPlugin.debug("ListMenu clicked item: " + item.getType() + " in category: " + category);

        SeriaRecipe recipe = plugin.getRecipeBookManager().getFirstRecipeForItem(item);
        if (recipe != null) {
            SeriaCollectionPlugin.debug("Opening RecipeDetailMenu for: " + recipe.getMmoId());
            if (plugin.getRecipeBookManager().isUnlocked(player, recipe)) {
                new RecipeDetailMenu(plugin, category).open(player, recipe);
            } else {
                SeriaCollectionPlugin.debug("Recipe is locked for player.");
            }
        } else {
            SeriaCollectionPlugin.debug("Failed to find recipe for clicked item.");
        }
    }

    private void handleCollectionDetailClick(Player player, CollectionDetailMenu menu, ItemStack item, int slot) {
        // Navigasi Standar
        if (item.getType() == Material.ARROW) {
            new CollectionMainMenu(plugin).open(player);
            return;
        } else if (item.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Cari resep berdasarkan reward di tier ini
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<net.kyori.adventure.text.Component> lore = item.getItemMeta().lore();
            if (lore == null) return;

            for (net.kyori.adventure.text.Component line : lore) {
                String plain = GuiUtils.stripColor(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line));
                // Jika line mengandung "Recipe", coba cari resepnya
                if (plain.toLowerCase().contains("recipe")) {
                    // Cari resep di RecipeBookManager berdasarkan nama item
                    // Contoh line: " Enchanted Wheat Recipe" -> ambil "Enchanted Wheat"
                    String itemName = plain.replace("Recipe", "").replace("-", "").trim();
                    for (SeriaRecipe recipe : plugin.getRecipeBookManager().getAllRecipes()) {
                        ItemStack output = plugin.getRecipeBookManager().getOutputItem(recipe);
                        if (output != null) {
                            String outName = GuiUtils.stripColor(output.getItemMeta().getDisplayName());
                            if (outName.equalsIgnoreCase(itemName)) {
                                if (plugin.getRecipeBookManager().isUnlocked(player, recipe)) {
                                    new RecipeDetailMenu(plugin, "OTHERS").open(player, recipe);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleDetailMenuClick(Player player, RecipeDetailMenu menu, ItemStack item) {
        if (item.getType() == Material.ARROW) {
            String name = GuiUtils.stripColor(item.getItemMeta().getDisplayName());
            if (name.equals("Back")) {
                new RecipeListMenu(plugin, menu.getCategory()).open(player);
            } else if (name.equals("Next Recipe")) {
                menu.open(player, menu.getRecipes(), menu.getCurrentIndex() + 1);
            } else if (name.equals("Previous Recipe")) {
                menu.open(player, menu.getRecipes(), menu.getCurrentIndex() - 1);
            }
            return;
        }

        if (item.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // Jika item yang diklik adalah ingredient atau result
        SeriaRecipe subRecipe = plugin.getRecipeBookManager().getFirstRecipeForItem(item);
        if (subRecipe != null) {
            if (plugin.getRecipeBookManager().isUnlocked(player, subRecipe)) {
                new RecipeDetailMenu(plugin, menu.getCategory()).open(player, subRecipe);
            }
        }
    }
}
