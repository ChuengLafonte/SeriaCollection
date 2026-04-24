package id.seria.collection.inventory;

import id.seria.collection.SeriaCollectionPlugin;
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

public class CollectionDetailMenu implements InventoryHolder, Listener {

    private final SeriaCollectionPlugin plugin;
    private final Collection collection;
    private final Inventory inventory;
    private final ConfigurationSection config;

    public CollectionDetailMenu(SeriaCollectionPlugin plugin, Collection collection) {
        this.plugin = plugin;
        this.collection = collection;
        this.config = plugin.getConfigManager().getGuisConfig().getConfigurationSection("collection-detail");
        
        int size = config.getInt("size", 54);
        String title = config.getString("title", "<dark_gray>Collection: %collection%")
                .replace("%collection%", collection.getName());
        
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

        long totalAmount = plugin.getPlayerDataManager().getAmount(player.getUniqueId(), collection.getId());

        // Top Icon (Info)
        ConfigurationSection infoCfg = config.getConfigurationSection("items.info");
        if (infoCfg != null) {
            ItemStack info = new ItemStack(collection.getMaterial());
            ItemMeta infoMeta = info.getItemMeta();
            infoMeta.displayName(GuiUtils.format(infoCfg.getString("name", "").replace("%collection%", collection.getName())));
            
            List<Component> infoLore = new ArrayList<>();
            for (String line : infoCfg.getStringList("lore")) {
                infoLore.add(GuiUtils.format(line
                    .replace("%collection%", collection.getName())
                    .replace("%amount%", String.valueOf(totalAmount))
                    .replace("%amount_formatted%", String.format("%,d", totalAmount))));
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

        // Tiers (Fixed path: items.tiers)
        ConfigurationSection tiersCfg = config.getConfigurationSection("items.tiers");
        if (tiersCfg != null) {
            List<Integer> slots = tiersCfg.getIntegerList("slots");
            Material unlockedMat = Material.valueOf(tiersCfg.getString("unlocked-material", "LIME_STAINED_GLASS_PANE"));
            Material lockedMat = Material.valueOf(tiersCfg.getString("locked-material", "RED_STAINED_GLASS_PANE"));
            
            int i = 0;
            int amount = (int) totalAmount;
            
            for (Tier tier : collection.getTiers().values()) {
                if (i >= slots.size()) break;
                
                boolean unlocked = amount >= tier.getRequirement();
                ItemStack item = new ItemStack(unlocked ? unlockedMat : lockedMat);
                item.setAmount(tier.getLevel()); // Set amount to tier level (e.g. 2 for Tier II)
                ItemMeta meta = item.getItemMeta();
                
                String statusColor = unlocked ? "<green>" : "<red>";
                String statusText = unlocked ? tiersCfg.getString("unlocked-status", "TERBUKA") 
                                             : tiersCfg.getString("locked-status", "TERKUNCI");
                
                meta.displayName(GuiUtils.format(tiersCfg.getString("name", "%status_color%%collection% %tier%")
                        .replace("%status_color%", statusColor)
                        .replace("%collection%", collection.getName())
                        .replace("%tier%", GuiUtils.toRoman(tier.getLevel()))));
                
                List<Component> lore = new ArrayList<>();
                int currentTierAmount = Math.min(amount, tier.getRequirement());
                int percent = (int) (((double) currentTierAmount / tier.getRequirement()) * 100);
                String progressBar = GuiUtils.getProgressBar(currentTierAmount, tier.getRequirement(), 20, '-', "<green>", "<gray>");

                for (String line : tiersCfg.getStringList("lore")) {
                    if (line.equalsIgnoreCase("%status_text%")) {
                        lore.add(GuiUtils.format(statusText));
                    } else if (line.contains("%rewards_list%")) {
                        // Dynamic Rewards injection
                        if (unlocked) {
                            if (tier.getDisplayRewards() != null && !tier.getDisplayRewards().isEmpty()) {
                                for (String dr : tier.getDisplayRewards()) lore.add(GuiUtils.format("<white> " + dr));
                            } else {
                                for (String reward : tier.getRewards()) lore.add(GuiUtils.format("<white> - " + reward));
                            }
                        } else {
                            if (tier.getDisplayRewards() != null && !tier.getDisplayRewards().isEmpty()) {
                                for (String dr : tier.getDisplayRewards()) lore.add(GuiUtils.format("<gray> " + dr));
                            } else {
                                lore.add(GuiUtils.format("<gray> ???"));
                            }
                        }
                    } else {
                        lore.add(GuiUtils.format(line
                            .replace("%percentage%", String.valueOf(percent))
                            .replace("%progress_bar%", progressBar)
                            .replace("%amount%", String.valueOf(currentTierAmount))
                            .replace("%amount_formatted%", String.format("%,d", currentTierAmount))
                            .replace("%requirement%", String.valueOf(tier.getRequirement()))
                            .replace("%requirement_formatted%", String.format("%,d", tier.getRequirement()))));
                    }
                }
                
                meta.lore(lore);
                item.setItemMeta(meta);
                inventory.setItem(slots.get(i), item);
                i++;
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof CollectionDetailMenu) {
            event.setCancelled(true);
            int backSlot = config.getInt("items.back.slot", 48);
            int closeSlot = config.getInt("items.close.slot", 49);
            
            if (event.getSlot() == backSlot) {
                Player player = (Player) event.getWhoClicked();
                new id.seria.collection.inventory.CollectionMainMenu(plugin).open(player);
            } else if (event.getSlot() == closeSlot) {
                event.getWhoClicked().closeInventory();
            }
        }
    }
}
