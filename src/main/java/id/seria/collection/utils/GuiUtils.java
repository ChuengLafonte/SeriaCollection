package id.seria.collection.utils;

import net.kyori.adventure.text.Component;
import id.seria.collection.SeriaCollectionPlugin;
import java.util.TreeMap;

public class GuiUtils {

    private static final TreeMap<Integer, String> ROMAN_MAP = new TreeMap<>();

    static {
        ROMAN_MAP.put(1000, "M");
        ROMAN_MAP.put(900, "CM");
        ROMAN_MAP.put(500, "D");
        ROMAN_MAP.put(400, "CD");
        ROMAN_MAP.put(100, "C");
        ROMAN_MAP.put(90, "XC");
        ROMAN_MAP.put(50, "L");
        ROMAN_MAP.put(40, "XL");
        ROMAN_MAP.put(10, "X");
        ROMAN_MAP.put(9, "IX");
        ROMAN_MAP.put(5, "V");
        ROMAN_MAP.put(4, "IV");
        ROMAN_MAP.put(1, "I");
    }

    public static String toRoman(int number) {
        int l = ROMAN_MAP.floorKey(number);
        if (number == l) {
            return ROMAN_MAP.get(number);
        }
        return ROMAN_MAP.get(l) + toRoman(number - l);
    }

    public static String getProgressBar(int current, int max, int totalBars, char symbol, String completedColor, String notCompletedColor) {
        float percent = (float) current / max;
        int progressBars = (int) (totalBars * percent);

        return completedColor + String.valueOf(symbol).repeat(Math.max(0, progressBars))
                + notCompletedColor + String.valueOf(symbol).repeat(Math.max(0, totalBars - progressBars));
    }

    public static String getProgressBar(int current, int max) {
        // Style: ----------------------------
        // Return only the bar. User can add numbers in YAML.
        return getProgressBar(current, max, 25, '-', "<green>", "<dark_gray>");
    }

    public static org.bukkit.inventory.ItemStack createCustomHead(String value) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
        
        try {
            com.destroystokyo.paper.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(java.util.UUID.randomUUID());
            profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", value));
            meta.setPlayerProfile(profile);
        } catch (Exception ignored) {}
        
        item.setItemMeta(meta);
        return item;
    }

    public static Component format(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        String cleaned = text.replaceAll("(?i)&[0-9a-fk-or]", "");
        return SeriaCollectionPlugin.getMiniMessage().deserialize("<!italic>" + cleaned);
    }

    public static String stripLegacy(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)&[0-9a-fk-or]", "");
    }

    public static String stripColor(String text) {
        if (text == null || text.isEmpty()) return "";
        // Gunakan cara Bukkit yang paling aman untuk membersihkan semua warna
        return org.bukkit.ChatColor.stripColor(text);
    }

    public static org.bukkit.inventory.ItemStack createItem(org.bukkit.Material material, String name, String... lore) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.displayName(format(name));
        java.util.List<Component> components = new java.util.ArrayList<>();
        for (String l : lore) components.add(format(l));
        meta.lore(components);
        item.setItemMeta(meta);
        return item;
    }

    public static org.bukkit.inventory.ItemStack createItem(org.bukkit.Material material, String name, java.util.List<Component> lore) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.displayName(format(name));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
