package id.seria.collection.inventory;

import id.seria.collection.SeriaCollectionPlugin;
import id.seria.collection.utils.GuiUtils;
import id.seria.collection.integration.topminions.TopMinionsHook;
import com.sarry20.topminion.models.minionConfig.MinionConfigClass;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class MinionCraftedMenu implements InventoryHolder {

    private final SeriaCollectionPlugin plugin;
    private final Player player;
    private int page;
    private final Inventory inventory;

    public MinionCraftedMenu(SeriaCollectionPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, 54, "Crafted Minions");
        setupItems();
    }

    public void open() {
        player.openInventory(inventory);
    }

    private void setupItems() {
        inventory.clear();
        
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(GuiUtils.format(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        // Information Book
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        bookMeta.displayName(GuiUtils.format("<yellow>Minion Crafting Progression"));
        
        int uniqueCrafts = plugin.getPlayerDataManager().getTotalUniqueMinionCrafts(player.getUniqueId());
        int bonusSlots = 0;
        int nextGoal = 0;
        int nextBonus = 0;

        ConfigurationSection minionsConfig = plugin.getConfigManager().getMinionsConfig();
        ConfigurationSection milestones = minionsConfig.getConfigurationSection("milestones");
        if (milestones != null) {
            List<String> keys = new ArrayList<>(milestones.getKeys(false));
            keys.sort((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
            
            for (String key : keys) {
                int req = Integer.parseInt(key);
                if (uniqueCrafts >= req) {
                    bonusSlots = milestones.getInt(key);
                } else {
                    nextGoal = req;
                    nextBonus = milestones.getInt(key);
                    break;
                }
            }
        }

        List<Component> bookLore = new ArrayList<>();
        bookLore.add(GuiUtils.format("<gray>Tracks all unique minions you have crafted."));
        bookLore.add(GuiUtils.format(""));
        bookLore.add(GuiUtils.format("<gray>Unique Minions Crafted: <yellow>" + uniqueCrafts));
        bookLore.add(GuiUtils.format("<gray>Current Bonus Slots: <green>+" + bonusSlots));
        bookLore.add(GuiUtils.format(""));
        if (nextGoal > 0) {
            int needed = nextGoal - uniqueCrafts;
            bookLore.add(GuiUtils.format("<gray>Next Goal: <yellow>" + nextGoal + " <gray>Unique Crafts"));
            bookLore.add(GuiUtils.format("<gray>Reward: <green>+" + nextBonus + " <gray>Bonus Slots"));
            bookLore.add(GuiUtils.format("<gray>Progress: <yellow>" + uniqueCrafts + "<gray>/<yellow>" + nextGoal));
            
            String bar = GuiUtils.getProgressBar(uniqueCrafts, nextGoal);
            bookLore.add(GuiUtils.format(bar));
            bookLore.add(GuiUtils.format(""));
            bookLore.add(GuiUtils.format("<yellow>Craft " + needed + " more unique minions to unlock!"));
        }
        
        bookMeta.lore(bookLore);
        book.setItemMeta(bookMeta);
        inventory.setItem(49, book);

        // Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(GuiUtils.format("<red>Kembali"));
        back.setItemMeta(backMeta);
        inventory.setItem(50, back);

        // YAML-Driven Minions List
        ConfigurationSection displayConfigs = minionsConfig.getConfigurationSection("displays");
        if (displayConfigs != null) {
            List<String> minionKeys = new ArrayList<>(displayConfigs.getKeys(false));
            int start = page * 28;
            int slotIndex = 10;

            for (int i = start; i < minionKeys.size() && slotIndex < 44; i++) {
                if (slotIndex % 9 == 0) slotIndex++;
                if (slotIndex % 9 == 8) slotIndex += 2;

                String minionKey = minionKeys.get(i);
                ConfigurationSection d = displayConfigs.getConfigurationSection(minionKey);
                
                // Use internal-id if specified, otherwise use the key
                String minionId = d.getString("internal-id", minionKey);
                
                ItemStack item;
                String matStr = d.getString("material", "PLAYER_HEAD");
                if (matStr.equalsIgnoreCase("PLAYER_HEAD") && d.contains("head-value")) {
                    item = GuiUtils.createCustomHead(d.getString("head-value"));
                } else {
                    item = new ItemStack(Material.valueOf(matStr.toUpperCase()));
                }

                ItemMeta meta = item.getItemMeta();
                meta.displayName(GuiUtils.format("<yellow>" + d.getString("display-name", minionKey)));
                
                List<Component> lore = new ArrayList<>();
                lore.add(GuiUtils.format(""));
                
                int maxTier = d.getInt("max-tiers", 12);
                for (int tier = 1; tier <= maxTier; tier++) {
                    // Use minionId (which could be oak_log) for the check
                    boolean crafted = plugin.getPlayerDataManager().hasCraftedMinion(player.getUniqueId(), minionId, tier);
                    String roman = GuiUtils.toRoman(tier);
                    lore.add(GuiUtils.format((crafted ? "<green>✔" : "<red>✖") + " Tier " + roman));
                }
                
                lore.add(GuiUtils.format(""));
                lore.add(GuiUtils.format("<yellow>Click to view recipes"));
                
                meta.lore(lore);
                item.setItemMeta(meta);
                inventory.setItem(slotIndex, item);
                slotIndex++;
            }

            // Pagination logic
            if (page > 0) {
                ItemStack prev = new ItemStack(Material.ARROW);
                ItemMeta pMeta = prev.getItemMeta();
                pMeta.displayName(GuiUtils.format("<green>Previous Page"));
                prev.setItemMeta(pMeta);
                inventory.setItem(45, prev);
            }
            if (start + 28 < minionKeys.size()) {
                ItemStack next = new ItemStack(Material.ARROW);
                ItemMeta nMeta = next.getItemMeta();
                nMeta.displayName(GuiUtils.format("<green>Next Page"));
                next.setItemMeta(nMeta);
                inventory.setItem(53, next);
            }
        }
    }

    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == 45 && page > 0) {
            page--;
            setupItems();
        } else if (event.getRawSlot() == 53) {
            ConfigurationSection displayConfigs = plugin.getConfigManager().getMinionsConfig().getConfigurationSection("displays");
            if (displayConfigs != null) {
                if ((page + 1) * 28 < displayConfigs.getKeys(false).size()) {
                    page++;
                    setupItems();
                }
            }
        } else if (event.getRawSlot() == 50) {
            new CollectionMainMenu(plugin).open(player);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
