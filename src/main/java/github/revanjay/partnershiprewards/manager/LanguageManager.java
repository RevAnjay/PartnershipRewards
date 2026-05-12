package github.revanjay.partnershiprewards.manager;

import github.revanjay.partnershiprewards.PartnershipRewards;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static github.revanjay.partnershiprewards.PartnershipRewards.colorize;

public class LanguageManager {
    private final PartnershipRewards plugin;
    private FileConfiguration langConfig;
    private String currentLang;

    public LanguageManager(PartnershipRewards plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        this.currentLang = plugin.getConfig().getString("language", "en");
        File langFile = new File(plugin.getDataFolder(), "lang/" + currentLang + ".yml");

        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            if (plugin.getResource("lang/" + currentLang + ".yml") != null) {
                plugin.saveResource("lang/" + currentLang + ".yml", false);
            }
        }

        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
        } else {
            langConfig = new YamlConfiguration();
            plugin.getLogger().warning("Language file lang/" + currentLang + ".yml not found!");
        }

        InputStream defLangStream = plugin.getResource("lang/" + currentLang + ".yml");
        if (defLangStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defLangStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defConfig);
        }
    }

    /**
     * Gets a message and prepends the prefix if prefix is true.
     */
    public String getMessage(String path, boolean usePrefix) {
        String message = langConfig.getString(path, "&cMissing language key: " + path);
        if (usePrefix) {
            String prefix = langConfig.getString("prefix", "&d&lPartnership &8» &r");
            return colorize(prefix + message);
        }
        return colorize(message);
    }
    
    /**
     * Gets a message without prefix by default.
     */
    public String getMessage(String path) {
        return getMessage(path, false);
    }
}
