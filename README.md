# EthriaPlotCount

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Paper](https://img.shields.io/badge/Paper-1.21.8+-blue.svg)](https://papermc.io/)
[![PlotSquared](https://img.shields.io/badge/PlotSquared-7.5.9+-green.svg)](https://github.com/IntellectualSites/PlotSquared)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Ein leistungsstarkes Minecraft-Plugin zum Zählen von Entities auf PlotSquared-Plots mit umfangreichen Berechtigungen, Mehrsprachigkeit und Admin-Tools.

*A powerful Minecraft plugin for counting entities on PlotSquared plots with comprehensive permissions, multi-language support, and admin tools.*

---

## 🇩🇪 Deutsch

### 📋 Funktionen

- **🔢 Entity-Zählung**: Zähle spezifische Entity-Typen auf deinem Plot
- **🏘️ Merge-Plot-Unterstützung**: Berücksichtigt automatisch gemergete Plots
- **👑 Admin-Tools**: Zähle alle Entity-Typen auf einmal mit `/plotcount all`
- **🌍 Mehrsprachig**: Deutsche und englische Spracheunterstützung
- **🔐 Umfangreiche Berechtigungen**: Granulare Kontrolle über Entity-Zugriff
- **⌨️ Tab-Completion**: Intelligente Vorschläge basierend auf Berechtigungen
- **🎨 Anpassbares Design**: Konfigurierbares Chat-Prefix und Nachrichten
- **🐛 Debug-System**: Optionale Debug-Logs für Troubleshooting

### 🚀 Befehle

| Befehl | Beschreibung | Berechtigung |
|--------|--------------|--------------|
| `/plotcount <entity>` | Zählt spezifische Entities | `ethriaplotcount.use` |
| `/plotcount all` | Zählt alle Entity-Typen (Admin) | `ethriaplotcount.admin` |
| `/plotcount *` | Alternative zu `all` | `ethriaplotcount.admin` |
| `/plotcount help` | Zeigt Hilfe | `ethriaplotcount.use` |
| `/plotcount reload` | Lädt Config neu | `ethriaplotcount.reload` |

#### Unterstützte Aliases
Konfigurierbare Aliases in `config.yml`:
- `/pc` 
- `/count`
- `/entitycount`
- `/zählen`

### 🔐 Berechtigungen

#### Basis-Berechtigungen
- `ethriaplotcount.use` - Grundlegende Plugin-Nutzung
- `ethriaplotcount.admin` - Admin-Befehle (`all`, `*`)
- `ethriaplotcount.reload` - Config neu laden

#### Entity-Gruppen-Berechtigungen
- `ethriaplotcount.group.animals` - Tiere zählen (Schafe, Kühe, etc.)
- `ethriaplotcount.group.mobs` - Monster zählen (Zombies, Skelette, etc.)
- `ethriaplotcount.group.entities` - Objekte zählen (Rüstungsständer, Itemframes, etc.)
- `ethriaplotcount.group.vehicles` - Fahrzeuge zählen (Boote, Loren, etc.)
- `ethriaplotcount.group.items` - Items zählen (Dropped Items, etc.)
- `ethriaplotcount.group.projectiles` - Projektile zählen (Pfeile, Tränke, etc.)
- `ethriaplotcount.group.*` - Alle Gruppen

#### Spezifische Entity-Berechtigungen
- `ethriaplotcount.entity.sheep` - Nur Schafe zählen
- `ethriaplotcount.entity.cow` - Nur Kühe zählen
- `ethriaplotcount.entity.*` - Alle Entities

#### Plot-Zugriff-Berechtigungen
- `ethriaplotcount.own` - Eigene/vertraute/geaddete Plots
- `ethriaplotcount.other` - Fremde Plots

### 🎨 Beispiel-Ausgaben

```
[EthriaCount] » Zähle Schafe auf diesem Plot...
[EthriaCount] » Ergebnis: 20 Schafe auf diesem Plot gefunden.

[EthriaCount] » === Entities auf diesem 4er Merge ===
[EthriaCount] » Schafe: 20
[EthriaCount] » Kühe: 8
[EthriaCount] » Rüstungsständer: 3
[EthriaCount] » === Gesamt: 31 Entities ===
```

### 🛠️ Installation

1. **Voraussetzungen**:
   - Paper 1.21.8+
   - PlotSquared 7.5.9+
   - Java 21+

2. **Installation**:
   - Plugin-JAR in den `plugins/` Ordner legen
   - Server neustarten
   - Config in `plugins/EthriaPlotCount/config.yml` anpassen

3. **Konfiguration**:
```yaml
# Spracheinstellung
language: de-de  # oder en-us

# Command-Aliases
command_aliases:
  - pc
  - count
  - entitycount
  - zählen

# Einstellungen
settings:
  include_merged_plots: true
  max_count_limit: 1000
  debug_enabled: false
```

### 🌟 Features im Detail

#### Deutsche/Englische Entity-Namen
```bash
/plotcount sheep    # Funktioniert
/plotcount Schaf    # Funktioniert auch!
/plotcount cow      # Funktioniert
/plotcount Kuh      # Funktioniert auch!
```

#### Intelligentes Tab-Completion
- Zeigt nur Entities an, für die du Berechtigung hast
- Unterstützt deutsche und englische Namen
- Admin-spezifische Befehle werden nur Admins angezeigt

#### Merge-Plot-Unterstützung
- Zählt automatisch alle gemergeten Plot-Teile
- Zeigt deutlich an, wenn es ein Merge ist
- Konfigurierbar ein-/ausschaltbar

---

## 🇬🇧 English

### 📋 Features

- **🔢 Entity Counting**: Count specific entity types on your plot
- **🏘️ Merged Plot Support**: Automatically includes merged plots
- **👑 Admin Tools**: Count all entity types at once with `/plotcount all`
- **🌍 Multi-Language**: German and English language support
- **🔐 Comprehensive Permissions**: Granular control over entity access
- **⌨️ Tab-Completion**: Smart suggestions based on permissions
- **🎨 Customizable Design**: Configurable chat prefix and messages
- **🐛 Debug System**: Optional debug logging for troubleshooting

### 🚀 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/plotcount <entity>` | Count specific entities | `ethriaplotcount.use` |
| `/plotcount all` | Count all entity types (Admin) | `ethriaplotcount.admin` |
| `/plotcount *` | Alternative to `all` | `ethriaplotcount.admin` |
| `/plotcount help` | Show help | `ethriaplotcount.use` |
| `/plotcount reload` | Reload config | `ethriaplotcount.reload` |

#### Supported Aliases
Configurable aliases in `config.yml`:
- `/pc`
- `/count` 
- `/entitycount`
- `/zählen`

### 🔐 Permissions

#### Base Permissions
- `ethriaplotcount.use` - Basic plugin usage
- `ethriaplotcount.admin` - Admin commands (`all`, `*`)
- `ethriaplotcount.reload` - Reload config

#### Entity Group Permissions
- `ethriaplotcount.group.animals` - Count animals (sheep, cows, etc.)
- `ethriaplotcount.group.mobs` - Count monsters (zombies, skeletons, etc.)
- `ethriaplotcount.group.entities` - Count objects (armor stands, item frames, etc.)
- `ethriaplotcount.group.vehicles` - Count vehicles (boats, minecarts, etc.)
- `ethriaplotcount.group.items` - Count items (dropped items, etc.)
- `ethriaplotcount.group.projectiles` - Count projectiles (arrows, potions, etc.)
- `ethriaplotcount.group.*` - All groups

#### Specific Entity Permissions
- `ethriaplotcount.entity.sheep` - Count only sheep
- `ethriaplotcount.entity.cow` - Count only cows
- `ethriaplotcount.entity.*` - All entities

#### Plot Access Permissions
- `ethriaplotcount.own` - Own/trusted/added plots
- `ethriaplotcount.other` - Foreign plots

### 🎨 Example Output

```
[EthriaCount] » Counting sheep on this plot...
[EthriaCount] » Result: 20 sheep found on this plot.

[EthriaCount] » === Entities on this 4-plot merge ===
[EthriaCount] » Sheep: 20
[EthriaCount] » Cows: 8
[EthriaCount] » Armor Stands: 3
[EthriaCount] » === Total: 31 Entities ===
```

### 🛠️ Installation

1. **Requirements**:
   - Paper 1.21.8+
   - PlotSquared 7.5.9+
   - Java 21+

2. **Setup**:
   - Place plugin JAR in `plugins/` folder
   - Restart server
   - Configure in `plugins/EthriaPlotCount/config.yml`

3. **Configuration**:
```yaml
# Language setting
language: en-us  # or de-de

# Command aliases
command_aliases:
  - pc
  - count
  - entitycount

# Settings  
settings:
  include_merged_plots: true
  max_count_limit: 1000
  debug_enabled: false
```

### 🌟 Advanced Features

#### Bilingual Entity Names
```bash
/plotcount sheep    # Works
/plotcount Schaf    # Also works!
/plotcount cow      # Works
/plotcount Kuh      # Also works!
```

#### Smart Tab-Completion
- Shows only entities you have permission for
- Supports German and English names
- Admin-specific commands only shown to admins

#### Merged Plot Support
- Automatically counts all merged plot parts
- Clearly indicates when it's a merge
- Configurable on/off

---

## 📊 Entity Categories

### 🐾 Animals
Sheep, Cows, Pigs, Chickens, Horses, Llamas, Cats, Dogs, etc.

### 👹 Mobs  
Zombies, Skeletons, Creepers, Spiders, Endermen, etc.

### 🏗️ Objects
Armor Stands, Item Frames, Paintings, etc.

### 🚗 Vehicles
Boats, Minecarts, Chest Boats, etc.

### 📦 Items
Dropped Items, Experience Orbs, etc.

### 🏹 Projectiles
Arrows, Potions, Snowballs, Fireballs, etc.

---

## 🔧 Configuration

### config.yml
```yaml
# Language (de-de or en-us)
language: de-de

# Command aliases
command_aliases:
  - pc
  - count  
  - entitycount
  - zählen

# Settings
settings:
  count_invisible: true
  count_fixed: true
  include_merged_plots: true
  max_count_limit: 1000
  count_dropped_items: false
  debug_enabled: false
  debug_boundaries: false
```

### Custom Messages
All messages can be customized in:
- `plugins/EthriaPlotCount/lang/de-de/messages.yml`
- `plugins/EthriaPlotCount/lang/en-us/messages.yml`

Including the chat prefix:
```yaml
prefix: "&8[&6EthriaCount&8]&7 » "
```

---

## 🐛 Debug & Troubleshooting

### Enable Debug Mode
```yaml
settings:
  debug_enabled: true      # General debug info → debug.log
  debug_boundaries: true   # Plot boundary debug → debug.log
```

### Debug Output
Debug information is written to `plugins/EthriaPlotCount/debug.log`:
```
[2025-11-26 18:30:15] [DEBUG] === EthriaPlotCount Debug-Session gestartet ===
[2025-11-26 18:30:20] [DEBUG] Input: 'Schaf' -> Konvertiert zu: 'sheep'
[2025-11-26 18:30:20] [DEBUG] Plot gefunden: 1;2 in Welt plotworld
[2025-11-26 18:30:21] [DEBUG] Zählstatistik: 4 Chunks geprüft, 127 Entities untersucht, 20 in Grenzen, 20 gezählt
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Leahcimkrob/EthriaPlotCount/issues)
- **Discord**: [Your Discord Server](https://discord.gg/yourserver)
- **Documentation**: [Wiki](https://github.com/Leahcimkrob/EthriaPlotCount/wiki)

---

## ⭐ Credits

- **PlotSquared**: [IntellectualSites](https://github.com/IntellectualSites/PlotSquared)
- **Paper**: [PaperMC](https://papermc.io/)

---

*Made with ❤️ for the Minecraft community*
