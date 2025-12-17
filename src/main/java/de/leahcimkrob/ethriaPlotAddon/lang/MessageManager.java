package de.leahcimkrob.ethriaPlotAddon.lang;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MessageManager {
    private final JavaPlugin plugin;
    private String currentLanguage;
    private FileConfiguration messagesConfig;
    private FileConfiguration entitiesConfig;

    public MessageManager(JavaPlugin plugin, String language) {
        this.plugin = plugin;
        this.currentLanguage = language;
        loadLanguageFiles();
    }

    public void setLanguage(String language) {
        this.currentLanguage = language;
        loadLanguageFiles();
    }

    private void loadLanguageFiles() {
        plugin.getLogger().info("Lade Sprachdateien für: " + currentLanguage);

        // Lade Nachrichten
        messagesConfig = loadLanguageFile("messages.yml");
        plugin.getLogger().info("Messages config geladen: " + (messagesConfig != null));

        // Lade Entity-Übersetzungen
        entitiesConfig = loadLanguageFile("entities.yml");
        plugin.getLogger().info("Entities config geladen: " + (entitiesConfig != null));

        if (entitiesConfig != null) {
            // Debug: Zeige verfügbare Schlüssel
            plugin.getLogger().info("Verfügbare Entity-Kategorien: " + entitiesConfig.getKeys(false));
        }
    }

    private FileConfiguration loadLanguageFile(String fileName) {
        // Erstelle Plugin-Datenverzeichnis falls es nicht existiert
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File langDir = new File(plugin.getDataFolder(), "lang" + File.separator + currentLanguage);
        File langFile = new File(langDir, fileName);

        plugin.getLogger().info("Versuche zu laden: " + langFile.getAbsolutePath());

        // Erstelle Sprach-Verzeichnis falls es nicht existiert
        if (!langDir.exists()) {
            boolean created = langDir.mkdirs();
            plugin.getLogger().info("Language directory erstellt: " + created + " - " + langDir.getAbsolutePath());
        }

        // Automatisches Update-System für Language-Dateien
        boolean fileUpdated = updateLanguageFileWithDefaults(langFile, fileName);

        // Wenn Datei nicht existiert, kopiere Standard-Datei
        if (!langFile.exists()) {
            String resourcePath = "lang/" + currentLanguage + "/" + fileName;
            plugin.getLogger().info("Datei existiert nicht, versuche Resource zu kopieren: " + resourcePath);

            InputStream resource = plugin.getResource(resourcePath);
            if (resource != null) {
                try {
                    plugin.saveResource(resourcePath, false);
                    plugin.getLogger().info("Resource erfolgreich kopiert: " + resourcePath);
                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Kopieren der Resource: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                plugin.getLogger().warning("Resource nicht gefunden: " + resourcePath);
                // Fallback auf Deutsch falls Sprache nicht existiert
                if (!currentLanguage.equals("de-de")) {
                    plugin.getLogger().warning("Language file " + resourcePath + " not found, falling back to de-de");
                    return loadFallbackLanguageFile(fileName);
                }
            }
        }

        // Versuche die Datei zu laden
        if (langFile.exists()) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                plugin.getLogger().info("YAML-Datei geladen: " + fileName + ", Keys: " + config.getKeys(false));
                return config;
            } catch (Exception e) {
                plugin.getLogger().severe("Fehler beim Laden der YAML-Datei " + fileName + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().severe("Sprachdatei konnte nicht erstellt werden: " + langFile.getAbsolutePath());
        }

        // Fallback: Lade direkt aus dem JAR
        plugin.getLogger().info("Versuche direkten Fallback aus JAR-Resource: " + fileName);
        return loadFallbackLanguageFile(fileName);
    }

    private FileConfiguration loadFallbackLanguageFile(String fileName) {
        String fallbackPath = "lang/de-de/" + fileName;
        plugin.getLogger().info("Versuche Fallback-Resource zu laden: " + fallbackPath);

        InputStream resource = plugin.getResource(fallbackPath);
        if (resource != null) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, "UTF-8"));
                plugin.getLogger().info("Fallback YAML geladen: " + fileName + ", Keys: " + config.getKeys(false));
                return config;
            } catch (Exception e) {
                plugin.getLogger().severe("Fehler beim Laden der Fallback-YAML " + fileName + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().severe("Fallback-Resource nicht gefunden: " + fallbackPath);
        }

        // Letzter Fallback: Leere Konfiguration
        plugin.getLogger().warning("Erstelle leere Konfiguration als letzter Fallback für: " + fileName);
        return new YamlConfiguration();
    }

    public String getMessage(String key) {
        // Spezialbehandlung für das prefix selbst
        if (key.equals("prefix")) {
            String prefix = messagesConfig.getString("messages.prefix", "&8[&6EthriaCount&8]&7 » ");
            return ChatColor.translateAlternateColorCodes('&', prefix);
        }

        String message = messagesConfig.getString("messages." + key, "&cMessage not found: " + key);

        // Füge Prefix vor jede Nachricht hinzu (außer bei System-Nachrichten)
        if (!isSystemMessage(key)) {
            // Hardcoded Fallback falls YAML-Prefix nicht lädt
            String prefix = messagesConfig.getString("messages.prefix", "&8[&6EthriaCount&8]&7 » ");
            message = prefix + message;
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String key, Map<String, String> replacements) {
        // Spezialbehandlung für das prefix selbst
        if (key.equals("prefix")) {
            String prefix = messagesConfig.getString("messages.prefix", "&8[&6EthriaCount&8]&7 » ");
            return ChatColor.translateAlternateColorCodes('&', prefix);
        }

        String message = messagesConfig.getString("messages." + key, "&cMessage not found: " + key);

        // Replace placeholders
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        // Füge Prefix vor jede Nachricht hinzu (außer bei System-Nachrichten)
        if (!isSystemMessage(key)) {
            // Hardcoded Fallback falls YAML-Prefix nicht lädt
            String prefix = messagesConfig.getString("messages.prefix", "&8[&6EthriaCount&8]&7 » ");
            message = prefix + message;
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String key, String placeholder, String value) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put(placeholder, value);
        return getMessage(key, replacements);
    }

    /**
     * Prüft ob eine Nachricht eine System-Nachricht ist, die kein Prefix benötigt
     */
    private boolean isSystemMessage(String key) {
        // Nur echte System-Log-Nachrichten (für Server-Konsole) ohne Prefix
        return key.equals("plugin_enabled") ||
               key.equals("plugin_disabled") ||
               key.equals("plotsquared_not_found") ||
               key.equals("plotsquared_found");
    }

    public String getEntityName(String entityType) {
        if (entitiesConfig == null) {
            plugin.getLogger().warning("entitiesConfig ist null! Verwende Standard-Formatierung für: " + entityType);
            return formatEntityName(entityType);
        }

        String lowerEntityType = entityType.toLowerCase();
        String name = null;

        // Durchsuche alle Kategorien nach englischen Namen
        String[] categories = {"animals", "mobs", "entities", "vehicles", "items", "projectiles"};

        for (String category : categories) {
            name = entitiesConfig.getString(category + "." + lowerEntityType);
            if (name != null) {
                return name;
            }
        }

        // Falls nicht gefunden, verwende Standard-Formatierung
        return formatEntityName(entityType);
    }

    /**
     * Neue Methode: Konvertiert deutsche Entity-Namen zu englischen Namen
     */
    public String getEnglishEntityName(String germanName) {
        if (entitiesConfig == null) {
            return germanName; // Fallback: unverändert zurückgeben
        }

        String lowerGermanName = germanName.toLowerCase();
        String[] categories = {"animals", "mobs", "entities", "vehicles", "items", "projectiles"};

        // Durchsuche alle Kategorien nach deutschen Namen (umgekehrte Suche)
        for (String category : categories) {
            if (entitiesConfig.contains(category)) {
                for (String englishKey : entitiesConfig.getConfigurationSection(category).getKeys(false)) {
                    String deutscherWert = entitiesConfig.getString(category + "." + englishKey);
                    if (deutscherWert != null && deutscherWert.toLowerCase().equals(lowerGermanName)) {
                        plugin.getLogger().info("🔄 Rückübersetzung: " + germanName + " -> " + englishKey + " (Kategorie: " + category + ")");
                        return englishKey; // Gib den englischen Schlüssel zurück
                    }
                }
            }
        }

        // Keine Rückübersetzung gefunden - könnte bereits englisch sein
        return germanName;
    }

    private String formatEntityName(String entityType) {
        // Formatiere den Namen (erste Buchstabe groß, _ durch Leerzeichen ersetzen)
        String name = entityType.toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    formatted.append(word.substring(1));
                }
                formatted.append(" ");
            }
        }
        return formatted.toString().trim();
    }

    public String getGroupName(String groupType) {
        return entitiesConfig.getString("groups." + groupType.toLowerCase(), groupType);
    }

    /**
     * Aktualisiert Language-Dateien automatisch mit fehlenden Werten aus der Standard-Datei
     */
    private boolean updateLanguageFileWithDefaults(File langFile, String fileName) {
        if (!langFile.exists()) {
            return false; // Datei wird später erstellt
        }

        try {
            // Lade Default-Language-Datei aus der JAR
            String resourcePath = "lang/" + currentLanguage + "/" + fileName;
            InputStream defaultStream = plugin.getResource(resourcePath);
            if (defaultStream == null) {
                return false;
            }

            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, java.nio.charset.StandardCharsets.UTF_8)
            );

            YamlConfiguration existingConfig = YamlConfiguration.loadConfiguration(langFile);

            boolean fileUpdated = false;

            // Prüfe alle Schlüssel in der Default-Datei
            for (String key : defaultConfig.getKeys(true)) {
                if (!existingConfig.contains(key)) {
                    // Fehlender Wert - füge ihn hinzu
                    existingConfig.set(key, defaultConfig.get(key));
                    fileUpdated = true;
                    plugin.getLogger().info("Language-Datei automatisch aktualisiert: " + fileName + " -> " + key);
                }
            }

            // Speichere Datei falls Änderungen gemacht wurden
            if (fileUpdated) {
                existingConfig.save(langFile);
                plugin.getLogger().info("Language-Datei wurde automatisch mit neuen Werten aktualisiert: " + fileName);
            }

            defaultStream.close();
            return fileUpdated;

        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim automatischen Language-Update für " + fileName + ": " + e.getMessage());
            return false;
        }
    }
}
