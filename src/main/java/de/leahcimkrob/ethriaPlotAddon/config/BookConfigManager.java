package de.leahcimkrob.ethriaPlotAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
/** Loads the MiniMessage templates used to render plot records in a written book. */
public final class BookConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private String language;

    public BookConfigManager(JavaPlugin plugin, String language) {
        this.plugin = plugin;
        this.language = language;
        reload();
    }

    public void setLanguage(String language) {
        this.language = language;
        reload();
    }

    public void reload() {
        String selectedLanguage = "en-us".equalsIgnoreCase(language) ? "en-us" : "de-de";
        String resourcePath = "lang/" + selectedLanguage + "/book.yml";
        File langDirectory = new File(plugin.getDataFolder(), "lang" + File.separator + selectedLanguage);
        File file = new File(langDirectory, "book.yml");
        if (!file.exists()) {
            langDirectory.mkdirs();
            if (plugin.getResource(resourcePath) != null) {
                plugin.saveResource(resourcePath, false);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public String getTitle() {
        return config.getString("book.title", "<gold>Plotinfo");
    }

    public String getAuthor() {
        return config.getString("book.author", "EthriaPlotAddon");
    }

    public String getDetailsPage() {
        return config.getString("book.details_page", "<gold>Plotinhaber:</gold> <white><owner></white>\n<gold>Ausführender:</gold> <white><executor></white>");
    }

    public String getNotePage() {
        return config.getString("book.note_page", "<gold>Bemerkung:</gold>\n<white><note></white>");
    }

    public String getNoteContinuationPage() {
        return config.getString("book.note_continuation_page", "<white><note></white>");
    }
}
