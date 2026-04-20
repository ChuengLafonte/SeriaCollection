package id.seria.collection.inventory;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.models.Collection;
import id.seria.collection.models.Tier;
import id.seria.collection.utils.GuiUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

    public CollectionDetailMenu(SeriaCollectionPlugin plugin, Collection collection) {
        this.plugin = plugin;
        this.collection = collection;
        this.inventory = Bukkit.createInventory(this, 54, SeriaCollectionPlugin.getMiniMessage().deserialize("<dark_gray>Collection: " + collection.getName()));
    }

    public void open(Player player) {
        setupItems(player);
        player.openInventory(inventory);
    }

    private void setupItems(Player player) {
        inventory.clear();
        
        // Fill glass
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.empty());
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 54; i++) inventory.setItem(i, glass);

        // Top Icon at Slot 4
        long totalAmount = plugin.getPlayerDataManager().getAmount(player.getUniqueId(), collection.getId());
        ItemStack topIcon = new ItemStack(collection.getMaterial());
        ItemMeta topMeta = topIcon.getItemMeta();
        topMeta.displayName(SeriaCollectionPlugin.getMiniMessage().deserialize("<yellow>" + collection.getName() + " Collection"));
        List<Component> topLore = new ArrayList<>();
        topLore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray>View all your " + collection.getName() + " Collection"));
        topLore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray>progress and rewards!"));
        topLore.add(Component.empty());
        topLore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray>Total Collected: <yellow>" + String.format("%,d", totalAmount)));
        topMeta.lore(topLore);
        topIcon.setItemMeta(topMeta);
        inventory.setItem(4, topIcon);

        // Footer buttons
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(SeriaCollectionPlugin.getMiniMessage().deserialize("<red>Go Back"));
        back.setItemMeta(backMeta);
        inventory.setItem(48, back);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(SeriaCollectionPlugin.getMiniMessage().deserialize("<red>Close"));
        close.setItemMeta(closeMeta);
        inventory.setItem(49, close);

        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        
        int i = 0;
        int amount = (int) totalAmount;
        for (Tier tier : collection.getTiers().values()) {
            if (i >= slots.length) break;
            
            boolean unlocked = amount >= tier.getRequirement();
            ItemStack item = new ItemStack(unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
            
            ItemMeta meta = item.getItemMeta();
            // line 1: <Green/Red> Tier Roman
            meta.displayName(SeriaCollectionPlugin.getMiniMessage().deserialize((unlocked ? "<green>" : "<red>") + collection.getName() + " " + GuiUtils.toRoman(tier.getLevel())));
            
            List<Component> lore = new ArrayList<>();
            
            // Progress Section
            int currentTierAmount = amount;
            if (currentTierAmount > tier.getRequirement()) currentTierAmount = tier.getRequirement();
            int percent = (int) (((double) currentTierAmount / tier.getRequirement()) * 100);
            
            lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray>Progress: <green>" + percent + "%"));
            
            String progressBar = GuiUtils.getProgressBar(currentTierAmount, tier.getRequirement(), 20, '-', "<green>", "<gray>");
            lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize(progressBar + " <yellow>" + String.format("%,d", currentTierAmount) + "/" + String.format("%,d", tier.getRequirement())));
            
            lore.add(Component.empty());
            lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<white>Rewards:"));
            
            // Rewards Section
            if (unlocked) {
                // If unlocked, use standard reward names or display-rewards
                if (tier.getDisplayRewards() != null && !tier.getDisplayRewards().isEmpty()) {
                    for (String dr : tier.getDisplayRewards()) {
                        lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<white> " + dr));
                    }
                } else {
                    for (String reward : tier.getRewards()) {
                        lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<white> - " + reward));
                    }
                }
            } else {
                // If locked, hide details to match Hypixel's curiosity or show grayed out
                if (tier.getDisplayRewards() != null && !tier.getDisplayRewards().isEmpty()) {
                    for (String dr : tier.getDisplayRewards()) {
                        lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray> " + dr));
                    }
                } else {
                    lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize("<gray> ???"));
                }
            }
            
            lore.add(Component.empty());
            lore.add(SeriaCollectionPlugin.getMiniMessage().deserialize(unlocked ? "<green>UNLOCKED" : "<red>LOCKED"));
            
            meta.lore(lore);
            item.setItemMeta(meta);
            
            inventory.setItem(slots[i], item);
            i++;
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
            if (event.getSlot() == 48) {
                Player player = (Player) event.getWhoClicked();
                new id.seria.collection.inventory.CollectionMainMenu(plugin).open(player);
            } else if (event.getSlot() == 49) {
                event.getWhoClicked().closeInventory();
            }
        }
    }
}
