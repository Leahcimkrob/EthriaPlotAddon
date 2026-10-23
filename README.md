# EthriaPlotAddon

[![Version](https://img.shields.io/badge/Version-1.6-brightgreen.svg)](https://github.com/leahcimkrob/EthriaPlotAddon/releases)
[![Java](https://img.shields.io/badge/Java-25+-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![Paper](https://img.shields.io/badge/Paper-26.2+-blue.svg)](https://papermc.io/)
[![PlotSquared](https://img.shields.io/badge/PlotSquared-7.6+-green.svg)](https://github.com/IntellectualSites/PlotSquared)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Ein leistungsstarkes Minecraft-Plugin für PlotSquared-Server mit Entity-Zählung, Plot-Management und Admin-Tools.

*A powerful Minecraft plugin for PlotSquared servers with entity counting, plot management, and admin tools.*

---

## 🚀 Features

### 🔢 Entity Counting
- Count specific entities on plots: `/plotaddon count sheep`
- Multi-language support: `/plotcount schaf` (German) or `/plotcount sheep` (English)
- Admin overview: `/plotaddon count all`
- Merged plot support with automatic detection

### 📊 Plot Management  
- **Plot Check**: `/plotaddon check` - Shows plot owner and online status
- **Live Updates**: `/plotaddon check on` - Permanent plot info in ActionBar (Admin-only)
- **Stop Updates**: `/plotaddon check off` - Disable live plot updates (Admin-only)
- **Extended Info**: `/plotaddon check -v` - Shows trusted players and merge info
- **Real-time Status**: "Online now" vs "Last seen 2 days ago"
- **Plot Info**: `/plotinfo` - View plot records, clickable locations, and full notes in a book
- **Plot Info Storage**: SQLite or MySQL database support
- **Plot Check Storage**: Local SQLite or shared MySQL storage for player online history

### 🔧 Administration
- **Configuration Reload**: `/plotaddon reload`
- **Dynamic Help**: `/plotaddon help` - Shows only available commands
- **Custom Aliases**: Configure in `config.yml`

### 🛡️ Permission System
- **Granular Control**: Per-entity or per-group permissions
- **Player/Admin Separation**: Different permission sets
- **Smart Tab-Completion**: Only shows allowed entities

---

## 📦 Quick Start

1. **Download** the latest release
2. **Place** in your `plugins/` folder  
3. **Restart** your server
4. **Configure** permissions (see [Wiki](https://github.com/leahcimkrob/EthriaPlotAddon/wiki))
5. **Use** `/plotaddon help` in-game

---

## 🎮 Example Usage

```bash
# Count entities
/plotaddon count sheep          # Count sheep on plot
/plotcount kuh                  # Count cows (German name)
/plotaddon count all            # All entities (Admin only)

# Plot information
/plotaddon check                # Show plot owner
/plotaddon check -v             # Extended plot info
/plotaddon check on             # Enable live plot updates (Admin-only)
/plotaddon check off            # Disable live plot updates (Admin-only)
/plotaddon plotinfo             # View notes for your current plot
/plotaddon plotinfo add         # Add a note to your current plot
/plotaddon plotinfo list Alex   # List a player's notes (permission required)

# Administration
/plotaddon reload               # Reload config
/plotaddon help                 # Show help
```

---

## 🔐 Permissions

### Player
- `ethriaplotaddon.count.own` - Count on own plots
- `ethriaplotaddon.count.group.animals` - Count animals
- `ethriaplotaddon.plotcheck.use` - Check plot info
- `ethriaplotaddon.plotinfo.view` - View plot info for owned plots

### Admin
- `ethriaplotaddon.admin` - Full access + live plot updates
- `ethriaplotaddon.count.reload` - Reload config
- `ethriaplotaddon.plotinfo.edit` - Add, edit, and view plot info entries
- `ethriaplotaddon.plotinfo.list` - List plot info entries for a player

**Full permission list**: [Wiki Permissions](https://github.com/leahcimkrob/EthriaPlotAddon/wiki/en-PERMISSIONS)

---

## 🔗 Dependencies

### Required
- **Java 25+** - Required runtime
- **Paper 26.2+** - Server software
- **PlotSquared 7.6+** - Plot management
- **PlaceholderAPI** - Required plugin dependency

*ActionBar rendering uses vanilla Minecraft; CMI is not needed.*

---

## 🌟 New in v1.6

- ✅ **Plot Check Offline Times** - The plugin stores last-online data locally or in a shared database, allowing Plot Check to show a player's status across servers because Paper's player history is server-specific.

## 🌟 New in v1.5

- ✅ **Plot Info Change Notifications** - Owners are notified on join about new or edited notes on their plots
- ✅ **Seen Tracking** - Each plot info entry records whether its change notification was shown
- ✅ **Grouped Change List** - Notifications list only changed entries and group them by world
- ✅ **Database Migration** - Existing SQLite and MySQL databases gain the `seen` column automatically

## 🌟 New in v1.4

- ✅ **Plot Info Records** - Store and view notes linked to plots
- ✅ **SQLite / MySQL** - Choose a local SQLite database or MySQL storage
- ✅ **Plot Info Admin Tools** - Edit entries and list notes with `/plotinfo list`
- ✅ **Clickable Plot Info** - Teleport to recorded locations and open full notes in a book

## 🌟 Previously added in v1.3

- ✅ **Live Plot Updates** - Real-time plot info in ActionBar (Vanilla Minecraft)
- ✅ **Auto-Config Updates** - Automatically adds new config values on plugin update
- ✅ **Auto-Language Updates** - Automatically adds new message translations  
- ✅ **Enhanced Commands** - `/plotaddon check on` and `/plotaddon check off` (Admin-only)
- ✅ **Extended Plot Info** - `/plotaddon check -v` shows trusted players, merge status, and more
- ✅ **No External Dependencies** - Uses vanilla Minecraft ActionBar (no CMI needed)
- ✅ **Better Tab Completion** - Support for all new parameters with permission-based filtering
- ✅ **Improved Permissions** - Separate permissions for plot checking and admin live-updates
- ✅ **Multi-language** - German and English support with automatic updates
- ✅ **Robust Error Handling** - Better stability and user feedback
- ✅ **Configurable Update Intervals** - Customize ActionBar refresh rate

---

## 📚 Documentation

- **[🏠 Wiki Home](https://github.com/leahcimkrob/EthriaPlotAddon/wiki)** - Complete documentation
- **[📋 Commands](https://github.com/leahcimkrob/EthriaPlotAddon/wiki/en-Commands)** - All available commands  
- **[🔐 Permissions](https://github.com/leahcimkrob/EthriaPlotAddon/wiki/en-Permissions)** - Permission system

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Leahcimkrob/EthriaPlotAddon/issues)
- **Wiki**: [Documentation](https://github.com/Leahcimkrob/EthriaPlotAddon/wiki)

---

## 📝 License

MIT License - see [LICENSE](LICENSE) file

---

*Made with ❤️ for the Minecraft community*


