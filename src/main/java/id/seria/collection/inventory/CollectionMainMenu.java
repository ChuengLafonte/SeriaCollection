package id.seria.collection.inventory;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Category;
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
import java.util.Map;

public class CollectionMainMenu implements InventoryHolder, Listener {

    private final SeriaCollectionPlugin plugin;
    private final Inventory inventory;
    private final ConfigurationSection config;

    public CollectionMainMenu(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getGuisConfig().getConfigurationSection("main-menu");
        int size = config.getInt("size", 54);
        String title = config.getString("title", "Collections");
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

        Map<String, Category> categories = plugin.getCollectionManager().getCategories();
        
        // Calculate total stats
        int totalCollections = 0;
        int unlockedCollections = 0;
        for (Category cat : categories.values()) {
            for (id.seria.collection.models.Collection coll : cat.getCollections().values()) {
                totalCollections++;
                if (plugin.getPlayerDataManager().getAmount(player.getUniqueId(), coll.getId()) > 0) {
                    unlockedCollections++;
                }
            }
        }
        int totalPercent = (totalCollections == 0) ? 0 : (unlockedCollections * 100 / totalCollections);

        // Info Icon
        ConfigurationSection infoCfg = config.getConfigurationSection("items.info");
        if (infoCfg != null) {
            ItemStack infoItem = new ItemStack(Material.valueOf(infoCfg.getString("material", "PAINTING")));
            ItemMeta infoMeta = infoItem.getItemMeta();
            infoMeta.displayName(GuiUtils.format(infoCfg.getString("name", "")));
            
            List<Component> infoLore = new ArrayList<>();
            for (String line : infoCfg.getStringList("lore")) {
                infoLore.add(GuiUtils.format(line
                    .replace("%percentage%", String.valueOf(totalPercent))
                    .replace("%unlocked%", String.valueOf(unlockedCollections))
                    .replace("%total%", String.valueOf(totalCollections))));
            }
            infoMeta.lore(infoLore);
            infoItem.setItemMeta(infoMeta);
            inventory.setItem(infoCfg.getInt("slot", 4), infoItem);
        }

        // Close button
        ConfigurationSection closeCfg = config.getConfigurationSection("items.close");
        if (closeCfg != null) {
            ItemStack closeItem = new ItemStack(Material.valueOf(closeCfg.getString("material", "BARRIER")));
            ItemMeta closeMeta = closeItem.getItemMeta();
            closeMeta.displayName(GuiUtils.format(closeCfg.getString("name", "<red>Close")));
            closeItem.setItemMeta(closeMeta);
            inventory.setItem(closeCfg.getInt("slot", 49), closeItem);
        }

        // Categories
        List<Integer> slots = config.getIntegerList("items.categories.slots");
        List<String> footerLines = config.getStringList("items.categories.footer");
        
        int i = 0;
        for (Category category : categories.values()) {
            if (i >= slots.size()) break;
            
            ItemStack item = new ItemStack(category.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(GuiUtils.format("<green>" + category.getName().replace("<white>", "").replace("<green>", "")));
            
            List<Component> lore = new ArrayList<>();
            for (String line : category.getLore()) {
                lore.add(GuiUtils.format(line));
            }
            
            // Category Progress
            int catTotal = category.getCollections().size();
            int catUnlocked = 0;
            for (id.seria.collection.models.Collection coll : category.getCollections().values()) {
                if (plugin.getPlayerDataManager().getAmount(player.getUniqueId(), coll.getId()) > 0) {
                    catUnlocked++;
                }
            }
            int catPercent = (catTotal == 0) ? 0 : (catUnlocked * 100 / catTotal);
            
            for (String line : footerLines) {
                String bar = GuiUtils.getProgressBar(catUnlocked, catTotal);
                lore.add(GuiUtils.format(line
                    .replace("%percentage%", String.valueOf(catPercent))
                    .replace("%unlocked%", String.valueOf(catUnlocked))
                    .replace("%total%", String.valueOf(catTotal))
                    .replace("%progress_bar%", bar)));
            }
            
            meta.lore(lore);
            item.setItemMeta(meta);
            
            inventory.setItem(slots.get(i), item);
            i++;
        }

        // Crafted Minions Button
        ConfigurationSection minionCfg = config.getConfigurationSection("items.crafted-minions");
        if (minionCfg != null) {
            ItemStack minionItem;
            String matStr = minionCfg.getString("material", "PLAYER_HEAD");
            if (matStr.equalsIgnoreCase("PLAYER_HEAD") && minionCfg.contains("head-value")) {
                minionItem = GuiUtils.createCustomHead(minionCfg.getString("head-value"));
            } else {
                minionItem = new ItemStack(Material.valueOf(matStr));
            }
            
            ItemMeta minionMeta = minionItem.getItemMeta();
            minionMeta.displayName(GuiUtils.format(minionCfg.getString("name", "")));
            
            List<Component> minionLore = new ArrayList<>();
            for (String line : minionCfg.getStringList("lore")) {
                minionLore.add(GuiUtils.format(line));
            }
            minionMeta.lore(minionLore);
            minionItem.setItemMeta(minionMeta);
            inventory.setItem(minionCfg.getInt("slot", 23), minionItem);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof CollectionMainMenu) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            
            int closeSlot = config.getInt("items.close.slot", 49);
            if (event.getSlot() == closeSlot) {
                player.closeInventory();
                return;
            }

            Map<String, Category> categories = plugin.getCollectionManager().getCategories();
            for (Category category : categories.values()) {
                if (clicked.getType() == category.getIcon()) {
                    new CategoryMenu(plugin, category).open(player);
                    return;
                }
            }

            int minionSlot = config.getInt("items.crafted-minions.slot", 23);
            if (event.getSlot() == minionSlot) {
                new MinionCraftedMenu(plugin, player, 0).open();
                return;
            }
        }
    }
}
