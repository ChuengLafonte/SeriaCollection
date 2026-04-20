package id.seria.collection;

import id.seria.collection.commands.CollectCommand;
import id.seria.collection.commands.AdminCommand;
import id.seria.collection.listeners.CollectionListener;
import id.seria.collection.managers.*;
import id.seria.collection.placeholders.CollectionPlaceholderExpansion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class SeriaCollectionPlugin extends JavaPlugin {

    private static SeriaCollectionPlugin instance;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private CollectionManager collectionManager;
    private PlayerDataManager playerDataManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        instance = this;
        
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

            logger.info("Initializing DatabaseManager...");
            this.databaseManager = new DatabaseManager(this);
            this.databaseManager.initialize();

            logger.info("Initializing PlayerDataManager, CollectionManager, and GuiManager...");
            this.playerDataManager = new PlayerDataManager(this);
            this.collectionManager = new CollectionManager(this);
            this.guiManager = new GuiManager(this);

            // Register Listeners
            logger.info("Registering Listeners...");
            getServer().getPluginManager().registerEvents(new CollectionListener(this), this);
            getServer().getPluginManager().registerEvents(new id.seria.collection.listeners.MenuListener(), this);

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
        if (databaseManager != null) {
            databaseManager.close();
        }
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

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

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
