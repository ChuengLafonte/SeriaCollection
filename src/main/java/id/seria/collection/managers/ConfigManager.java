package id.seria.collection.managers;

import id.seria.collection.SeriaCollectionPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final SeriaCollectionPlugin plugin;
    private FileConfiguration collectionsConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration guisConfig;
    private File collectionsFile;
    private File messagesFile;
    private File guisFile;

    public ConfigManager(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        
        collectionsFile = new File(plugin.getDataFolder(), "collections.yml");
        if (!collectionsFile.exists()) {
            plugin.saveResource("collections.yml", false);
        }
        collectionsConfig = YamlConfiguration.loadConfiguration(collectionsFile);

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        guisFile = new File(plugin.getDataFolder(), "guis.yml");
        if (!guisFile.exists()) {
            plugin.saveResource("guis.yml", false);
        }
        guisConfig = YamlConfiguration.loadConfiguration(guisFile);
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        collectionsConfig = YamlConfiguration.loadConfiguration(collectionsFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        guisConfig = YamlConfiguration.loadConfiguration(guisFile);
        plugin.getCollectionManager().loadCollections(); // Refresh collections in memory
    }

    public FileConfiguration getCollectionsConfig() {
        return collectionsConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getGuisConfig() {
        return guisConfig;
    }

    public String getMessage(String path) {
        return messagesConfig.getString(path, path);
    }
}
