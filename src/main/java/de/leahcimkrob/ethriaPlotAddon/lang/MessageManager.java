package de.leahcimkrob.ethriaPlotAddon.lang;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MessageManager {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
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
        String message = messagesConfig.getString("messages." + key, "<red>Message not found: " + key);
        if (key.equals("prefix")) return toLegacyString(miniMessage.deserialize(message));
        if (!isSystemMessage(key)) {
            String prefix = messagesConfig.getString("messages.prefix", "<dark_gray>[<gold>EthriaCount</gold>]<gray> » ");
            message = prefix + message;
        }
        return toLegacyString(miniMessage.deserialize(message));
    }

    public Component getComponent(String key) {
        // Spezialbehandlung für das prefix selbst
        if (key.equals("prefix")) {
            return miniMessage.deserialize(messagesConfig.getString("messages.prefix", "<dark_gray>[<gold>EthriaCount</gold>]<gray> » "));
        }

        String message = messagesConfig.getString("messages." + key, "<red>Message not found: " + key);

        // Füge Prefix vor jede Nachricht hinzu (außer bei System-Nachrichten)
        if (!isSystemMessage(key)) {
            // Hardcoded Fallback falls YAML-Prefix nicht lädt
            String prefix = messagesConfig.getString("messages.prefix", "<dark_gray>[<gold>EthriaCount</gold>]<gray> » ");
            message = prefix + message;
        }

        return miniMessage.deserialize(message);
    }

    public String getRawMessage(String key) {
        return messagesConfig.getString("messages." + key, "");
    }

    public String getMessage(String key, Map<String, String> replacements) {
        String message = messagesConfig.getString("messages." + key, "<red>Message not found: " + key);
        if (key.equals("prefix")) return toLegacyString(miniMessage.deserialize(message, resolver(replacements)));
        if (!isSystemMessage(key)) {
            String prefix = messagesConfig.getString("messages.prefix", "<dark_gray>[<gold>EthriaCount</gold>]<gray> » ");
            message = prefix + message;
        }
        return toLegacyString(miniMessage.deserialize(message, resolver(replacements)));
    }

    private String toLegacyString(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public Component getComponent(String key, Map<String, String> replacements) {
        return getComponent(key, replacements, true);
    }

    public Component getComponent(String key, Map<String, String> replacements, boolean withPrefix) {
        // Spezialbehandlung für das prefix selbst
        if (key.equals("prefix")) {
            String prefix = messagesConfig.getString("messages.prefix", "<dark_gray>[<gold>EthriaCount</gold>]<gray> » ");
            return miniMessage.deserialize(prefix, resolver(replacements));
        }

        String message = messagesConfig.getString("messages." + key, "<red>Message not found: " + key);

        // Migrate legacy plot-count templates from existing user language files.
        if (key.equals("plotinfo_plot_count_command")) {
            message = message
                    .replace("%plotsquared_plot_count_this%", "<plotcount>")
                    .replace("%plotsquared_plot_count_plotwelt%", "<plotcount>")
                    .replace("%plotsquared_allowed_plot_count%", "<allowedcount>");
            message = normalizeLegacyFormatting(message);
        }

        // Füge Prefix vor jede Nachricht hinzu (außer bei System-Nachrichten)
        if (withPrefix && !isSystemMessage(key)) {
            String prefixKey = key.startsWith("plotinfo_") ? "prefix_plotinfo" : "prefix";
            String fallbackPrefix = key.startsWith("plotinfo_")
                    ? "<dark_gray>[<gold>PlotInfo</gold>]<gray> » "
                    : "<dark_gray>[<gold>EthriaCount</gold>]<gray> » ";
            String prefix = messagesConfig.getString("messages." + prefixKey,
                    messagesConfig.getString("messages.prefix", fallbackPrefix));
            message = prefix + message;
        }

        return miniMessage.deserialize(message, resolver(replacements));
    }

    private TagResolver resolver(Map<String, String> replacements) {
        TagResolver.Builder builder = TagResolver.builder();
        replacements.forEach((key, value) -> builder.resolver(Placeholder.unparsed(
                key.toLowerCase(java.util.Locale.ROOT), value == null ? "" : value)));
        return builder.build();
    }

    private String normalizeLegacyFormatting(String message) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?i)&([0-9a-fk-or])").matcher(message);
        StringBuffer converted = new StringBuffer();
        while (matcher.find()) {
            String tag = switch (matcher.group(1).toLowerCase(java.util.Locale.ROOT)) {
                case "0" -> "black";
                case "1" -> "dark_blue";
                case "2" -> "dark_green";
                case "3" -> "dark_aqua";
                case "4" -> "dark_red";
                case "5" -> "dark_purple";
                case "6" -> "gold";
                case "7" -> "gray";
                case "8" -> "dark_gray";
                case "9" -> "blue";
                case "a" -> "green";
                case "b" -> "aqua";
                case "c" -> "red";
                case "d" -> "light_purple";
                case "e" -> "yellow";
                case "f" -> "white";
                case "k" -> "obfuscated";
                case "l" -> "bold";
                case "m" -> "strikethrough";
                case "n" -> "underlined";
                case "o" -> "italic";
                case "r" -> "reset";
                default -> throw new IllegalStateException();
            };
            matcher.appendReplacement(converted, java.util.regex.Matcher.quoteReplacement("<" + tag + ">"));
        }
        matcher.appendTail(converted);
        return converted.toString();
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
               key.equals("plotsquared_found") ||
               key.equals("plotinfo_header") ||
               key.equals("plotinfo_entry");
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
