package id.seria.collection.inventory;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Category;
import id.seria.collection.models.Collection;
import id.seria.collection.models.Tier;
import id.seria.collection.utils.GuiUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CategoryMenu implements InventoryHolder, Listener {

    private final SeriaCollectionPlugin plugin;
    private final Category category;
    private final Inventory inventory;
    private final ConfigurationSection config;

    public CategoryMenu(SeriaCollectionPlugin plugin, Category category) {
        this.plugin = plugin;
        this.category = category;
        this.config = plugin.getConfigManager().getGuisConfig().getConfigurationSection("category-menu");
        
        int size = config.getInt("size", 54);
        String catName = category.getName().replace("<white>", "").replace("<green>", "");
        String title = config.getString("title", "%category% Collections").replace("%category%", catName);
        
        this.inventory = Bukkit.createInventory(this, size, GuiUtils.format(title));
    }

    public void open(Player player) {
        setupItems(player);
        player.openInventory(inventory);
    }

    private void setupItems(Player player) {
        inventory.clear();
        
        // Fill glass (Filler)
        ConfigurationSection fillerCfg = config.getConfigurationSection("filler");
        if (fillerCfg != null) {
            Material fillerMat = Material.valueOf(fillerCfg.getString("material", "GRAY_STAINED_GLASS_PANE"));
            String fillerName = fillerCfg.getString("name", " ");
            ItemStack filler = new ItemStack(fillerMat);
            ItemMeta fillerMeta = filler.getItemMeta();
            fillerMeta.displayName(GuiUtils.format(fillerName));
            filler.setItemMeta(fillerMeta);
            for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
        }

        String catName = category.getName().replace("<white>", "").replace("<green>", "");

        // Top Icon (Info)
        ConfigurationSection infoCfg = config.getConfigurationSection("items.info");
        if (infoCfg != null) {
            ItemStack info = new ItemStack(category.getIcon());
            ItemMeta infoMeta = info.getItemMeta();
            infoMeta.displayName(GuiUtils.format(infoCfg.getString("name", "").replace("%category%", catName)));
            
            List<Component> infoLore = new ArrayList<>();
            int catTotal = category.getCollections().size();
            int catUnlocked = 0;
            for (Collection coll : category.getCollections().values()) {
                if (plugin.getPlayerDataManager().getAmount(player.getUniqueId(), coll.getId()) > 0) {
                    catUnlocked++;
                }
            }
            int catPercent = (catTotal == 0) ? 0 : (catUnlocked * 100 / catTotal);

            for (String line : infoCfg.getStringList("lore")) {
                infoLore.add(GuiUtils.format(line
                    .replace("%category%", catName)
                    .replace("%percentage%", String.valueOf(catPercent))
                    .replace("%unlocked%", String.valueOf(catUnlocked))
                    .replace("%total%", String.valueOf(catTotal))));
            }
            
            infoMeta.lore(infoLore);
            info.setItemMeta(infoMeta);
            inventory.setItem(infoCfg.getInt("slot", 4), info);
        }

        // Back button
        ConfigurationSection backCfg = config.getConfigurationSection("items.back");
        if (backCfg != null) {
            ItemStack back = new ItemStack(Material.valueOf(backCfg.getString("material", "ARROW")));
            ItemMeta backMeta = back.getItemMeta();
            backMeta.displayName(GuiUtils.format(backCfg.getString("name", "<red>Go Back")));
            back.setItemMeta(backMeta);
            inventory.setItem(backCfg.getInt("slot", 48), back);
        }

        // Close button
        ConfigurationSection closeCfg = config.getConfigurationSection("items.close");
        if (closeCfg != null) {
            ItemStack close = new ItemStack(Material.valueOf(closeCfg.getString("material", "BARRIER")));
            ItemMeta closeMeta = close.getItemMeta();
            closeMeta.displayName(GuiUtils.format(closeCfg.getString("name", "<red>Close")));
            close.setItemMeta(closeMeta);
            inventory.setItem(closeCfg.getInt("slot", 49), close);
        }

        // Collections
        List<Integer> slots = config.getIntegerList("items.collections.slots");
        List<String> collectionLore = config.getStringList("items.collections.lore");
        List<String> maxedLore = config.getStringList("items.collections.maxed-lore");
        
        int i = 0;
        for (Collection collection : category.getCollections().values()) {
            if (i >= slots.size()) break;
            
            int amount = plugin.getPlayerDataManager().getAmount(player.getUniqueId(), collection.getId());
            Tier nextTier = collection.getNextTier(amount);
            
            ItemStack item = new ItemStack(collection.getMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(GuiUtils.format("<green>" + collection.getName()));
            
            List<Component> lore = new ArrayList<>();
            List<String> sourceLines = (nextTier != null) ? collectionLore : maxedLore;
            
            for (String line : sourceLines) {
                String processed = line.replace("%amount%", String.valueOf(amount))
                        .replace("%amount_formatted%", String.format("%,d", amount));
                
                if (nextTier != null) {
                    int percent = (int) (((double) amount / nextTier.getRequirement()) * 100);
                    String progressBar = GuiUtils.getProgressBar(amount, nextTier.getRequirement(), 20, '-', "<green>", "<gray>");
                    processed = processed
                        .replace("%next_tier%", GuiUtils.toRoman(nextTier.getLevel()))
                        .replace("%requirement%", String.valueOf(nextTier.getRequirement()))
                        .replace("%requirement_formatted%", String.format("%,d", nextTier.getRequirement()))
                        .replace("%percentage%", String.valueOf(percent))
                        .replace("%progress_bar%", progressBar);
                } else {
                    // For maxed collections, still replace progress placeholders with full values
                    processed = processed
                        .replace("%next_tier%", "MAX")
                        .replace("%percentage%", "100")
                        .replace("%progress_bar%", GuiUtils.getProgressBar(100, 100, 20, '-', "<green>", "<gray>"));
                }
                lore.add(GuiUtils.format(processed));
            }
            
            meta.lore(lore);
            item.setItemMeta(meta);
            
            inventory.setItem(slots.get(i), item);
            i++;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof CategoryMenu) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            
            int backSlot = config.getInt("items.back.slot", 48);
            int closeSlot = config.getInt("items.close.slot", 49);
            
            if (event.getSlot() == backSlot) {
                new CollectionMainMenu(plugin).open(player);
                return;
            }
            if (event.getSlot() == closeSlot) {
                player.closeInventory();
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            for (Collection coll : category.getCollections().values()) {
                if (clicked.getType() == coll.getMaterial()) {
                    new CollectionDetailMenu(plugin, coll).open(player);
                    break;
                }
            }
        }
    }
}
