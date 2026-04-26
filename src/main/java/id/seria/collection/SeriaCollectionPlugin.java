package id.seria.collection;

import id.seria.collection.commands.CollectCommand;
import id.seria.collection.commands.AdminCommand;
import id.seria.collection.listeners.CollectionListener;
import id.seria.collection.listeners.MMOItemsCraftListener;
import id.seria.collection.listeners.MenuListener;
import id.seria.collection.integration.mmoitems.SCollectRequirementStat;
import id.seria.collection.managers.*;
import id.seria.collection.placeholders.CollectionPlaceholderExpansion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class SeriaCollectionPlugin extends JavaPlugin {

    private static SeriaCollectionPlugin instance;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    public static NamespacedKey DROPPED_ITEM_KEY;
    
    private ConfigManager configManager;
    private CollectionManager collectionManager;
    private PlayerDataManager playerDataManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        instance = this;
        
        DROPPED_ITEM_KEY = new NamespacedKey(this, "dropped_item");
        
        // Ensure folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        Logger logger = getLogger();
        
        try {
            logger.info("Starting initialization sequence...");

            // Initialize Managers
            logger.info("Loading ConfigManager...");
            this.configManager = new ConfigManager(this);
            this.configManager.loadConfigs();

            // Removed local DatabaseManager init

            logger.info("Initializing PlayerDataManager, CollectionManager, and GuiManager...");
            this.playerDataManager = new PlayerDataManager(this);
            this.collectionManager = new CollectionManager(this);
            this.guiManager = new GuiManager(this);

            // Register Listeners
            logger.info("Registering Listeners...");
            getServer().getPluginManager().registerEvents(new CollectionListener(this), this);
            getServer().getPluginManager().registerEvents(new MenuListener(), this);
            
            // Register MMOItems Integration
            if (Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
                // Only register if it doesn't exist yet (prevents error on reload)
                if (MMOItems.plugin.getStats().get("SCOLLECT_TIER") == null) {
                    MMOItems.plugin.getStats().register(new SCollectRequirementStat());
                    logger.info("Custom stat SCOLLECT_TIER registered!");
                } else {
                    logger.info("Custom stat SCOLLECT_TIER already exists, skipping registration.");
                }
                
                getServer().getPluginManager().registerEvents(new MMOItemsCraftListener(this), this);
                logger.info("MMOItems integration listener registered!");
            }

            // Register Commands
            logger.info("Registering Commands...");
            if (getCommand("collect") != null) {
                getCommand("collect").setExecutor(new CollectCommand(this));
            } else {
                logger.severe("Command /collect not found in plugin.yml!");
            }
            
            AdminCommand adminCommand = new AdminCommand(this);
            if (getCommand("seriacollection") != null) {
                getCommand("seriacollection").setExecutor(adminCommand);
                getCommand("seriacollection").setTabCompleter(adminCommand);
            } else {
                logger.severe("Command /seriacollection not found in plugin.yml!");
            }

            // Register PAPI
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new CollectionPlaceholderExpansion(this).register();
                logger.info("PlaceholderAPI expansion registered!");
            }

            logger.info("SeriaCollection enabled successfully!");
        } catch (Throwable t) {
            logger.severe("CRITICAL ERROR DURING INITIALIZATION: " + t.getMessage());
            t.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Database cleanup handled by SeriaCore
    }

    public static SeriaCollectionPlugin getInstance() {
        return instance;
    }

    public static MiniMessage getMiniMessage() {
        return MINI_MESSAGE;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    // Removed getDatabaseManager as it's now centralized in SeriaCore

    public CollectionManager getCollectionManager() {
        return collectionManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}
