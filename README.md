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
- **Documentation**: [Wiki](https://github.com/Leahcimkrob/EthriaPlotCount/wiki)

---

## ⭐ Credits

- **PlotSquared**: [IntellectualSites](https://github.com/IntellectualSites/PlotSquared)
- **Paper**: [PaperMC](https://papermc.io/)

---

*Made with ❤️ for the Minecraft community*
