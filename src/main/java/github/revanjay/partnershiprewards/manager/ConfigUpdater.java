package github.revanjay.partnershiprewards.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ConfigUpdater {

    public static void updateConfig(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream == null) {
            return;
        }

        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
        boolean changed = false;

        List<String> toRemove = new ArrayList<>();
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key) && !defaultConfig.contains(key, true)) {
                toRemove.add(key);
            }
        }

        for (String key : toRemove) {
            config.set(key, null);
            plugin.getLogger().info("Removed obsolete config key: " + key);
            changed = true;
        }

        for (String key : defaultConfig.getKeys(true)) {
            if (!config.contains(key, true)) {
                config.set(key, defaultConfig.get(key));
                if (defaultConfig.getComments(key) != null && !defaultConfig.getComments(key).isEmpty()) {
                    config.setComments(key, defaultConfig.getComments(key));
                }
                plugin.getLogger().info("Added new config key: " + key);
                changed = true;
            }
        }

        if (changed) {
            plugin.saveConfig();
            plugin.getLogger().info("config.yml has been updated automatically!");
        }
    }
}
