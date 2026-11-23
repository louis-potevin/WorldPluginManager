package fr.phundir.wplmanager.config;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loads {@code messages.yml} and formats its entries.
 * Keys missing from the server's file fall back to the version bundled in the jar.
 */
public final class Messages {
    private static final String FILE_NAME = "messages.yml";

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        InputStream bundled = plugin.getResource(FILE_NAME);
        if (bundled != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(bundled, StandardCharsets.UTF_8)));
        }
    }

    /**
     * Returns the colored message for {@code key}.
     *
     * @param placeholders alternating names and values, e.g. {@code "plugin", "WorldEdit", "world", "world_nether"}
     */
    public String get(String key, String... placeholders) {
        String message = config.getString(key);
        if (message == null) {
            message = key;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Returns a multi-line message (item lore...): a YAML list, or a string with {@code \n} line breaks.
     *
     * @param placeholders alternating names and values, as in {@link #get}
     */
    public List<String> lines(String key, String... placeholders) {
        List<String> raw = config.isList(key)
                ? config.getStringList(key)
                : Arrays.asList(get(key).split("\n"));
        List<String> lines = new ArrayList<>(raw.size());
        for (String line : raw) {
            for (int i = 0; i + 1 < placeholders.length; i += 2) {
                line = line.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
            lines.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return lines;
    }

    public void send(CommandSender recipient, String key, String... placeholders) {
        recipient.sendMessage(get(key, placeholders));
    }
}
