package id.seria.collection.managers;

import id.seria.collection.SeriaCollectionPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final SeriaCollectionPlugin plugin;
    private FileConfiguration collectionsConfig; // Legacy or main order
    private FileConfiguration messagesConfig;
    private FileConfiguration guisConfig;
    private FileConfiguration minionsConfig;
    private final List<FileConfiguration> collectionFiles = new ArrayList<>();

    public ConfigManager(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        this.collectionsConfig = loadConfig("collections.yml");
        this.messagesConfig = loadConfig("messages.yml");
        this.guisConfig = loadConfig("guis.yml");
        this.minionsConfig = loadConfig("minions.yml");
        
        loadCollectionFolder();
    }

    private void loadCollectionFolder() {
        collectionFiles.clear();
        File folder = new File(plugin.getDataFolder(), "collections");
        if (!folder.exists()) {
            folder.mkdirs();
            // Simpan file contoh jika folder baru dibuat
            plugin.saveResource("collections/farming.yml", false);
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                collectionFiles.add(YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        this.collectionsConfig = loadConfig("collections.yml");
        this.messagesConfig = loadConfig("messages.yml");
        this.guisConfig = loadConfig("guis.yml");
        this.minionsConfig = loadConfig("minions.yml");
        loadCollectionFolder();
        plugin.getCollectionManager().loadCollections();
    }

    public List<FileConfiguration> getCollectionFiles() {
        return collectionFiles;
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

    public FileConfiguration getMinionsConfig() {
        return minionsConfig;
    }
}
