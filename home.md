# EthriaPlotCount Wiki

Willkommen zur offiziellen Dokumentation von **EthriaPlotCount** - dem leistungsstarken Minecraft-Plugin zum Zählen von Entities auf PlotSquared-Plots.

*Welcome to the official documentation for **EthriaPlotCount** - the powerful Minecraft plugin for counting entities on PlotSquared plots.*

---

## 🏠 Quick Start

### Was ist EthriaPlotCount?
EthriaPlotCount ist ein speziell für PlotSquared entwickeltes Plugin, das es Spielern ermöglicht, verschiedene Entity-Typen auf ihren Plots zu zählen. Das Plugin bietet umfangreiche Berechtigungen, Mehrsprachigkeit und Admin-Tools für eine professionelle Server-Verwaltung.

### Hauptfunktionen
- 🔢 **Präzise Entity-Zählung** auf PlotSquared-Plots
- 🏘️ **Merge-Plot-Unterstützung** für zusammengefasste Plots
- 👑 **Admin-Tools** für Serververwalter
- 🌍 **Mehrsprachigkeit** (Deutsch/Englisch)
- 🔐 **Granulare Berechtigungen** für jeden Entity-Typ
- ⌨️ **Intelligente Tab-Completion**
- 🎨 **Vollständig anpassbar**

---

## 📚 Wiki-Seiten

### 🎯 Grundlagen
- **[Installation & Setup](Installation)**
- **[Erste Schritte](Getting-Started)**
- **[Befehle & Aliases](Commands)**
- **[Konfiguration](Configuration)**

### 🔐 Berechtigungen
- **[Permissions Übersicht](Permissions)**
- **[Entity-Gruppen](Entity-Groups)**
- **[Plot-Zugriff](Plot-Access)**
- **[Admin-Berechtigungen](Admin-Permissions)**

### 🌟 Features
- **[Entity-Kategorien](Entity-Categories)**
- **[Mehrsprachigkeit](Multi-Language)**
- **[Tab-Completion](Tab-Completion)**
- **[Merge-Plot-Unterstützung](Merged-Plots)**

### 🛠️ Administration
- **[Server-Setup](Server-Setup)**
- **[Debug & Troubleshooting](Debugging)**
- **[Performance-Optimierung](Performance)**
- **[FAQ](FAQ)**

### 🎨 Anpassung
- **[Custom Messages](Custom-Messages)**
- **[Chat-Prefix](Chat-Prefix)**
- **[Entity-Übersetzungen](Entity-Translations)**

---

## 🚀 Schnellstart-Guide

### 1. Installation
```bash
# Plugin herunterladen und in plugins/ Ordner legen
# Server neustarten
# Automatische Konfigurationserstellung
```

### 2. Basis-Berechtigungen vergeben
```yaml
# Beispiel für LuckPerms
lp group default permission set ethriaplotcount.use true
lp group default permission set ethriaplotcount.own true
lp group default permission set ethriaplotcount.group.animals true
```

### 3. Erste Commands testen
```bash
/plotcount sheep    # Zähle Schafe
/pc cow             # Zähle Kühe (Alias)
/plotcount help     # Zeige Hilfe
```

---

## 📖 Beispiele

### Grundlegende Nutzung
```
Spieler: /plotcount sheep
Server:  [EthriaCount] » Zähle Schafe auf diesem Plot...
Server:  [EthriaCount] » Ergebnis: 20 Schafe auf diesem Plot gefunden.
```

### Admin-Nutzung
```
Admin:   /plotcount all
Server:  [EthriaCount] » === Entities auf diesem 4er Merge ===
Server:  [EthriaCount] » Schafe: 20
Server:  [EthriaCount] » Kühe: 8
Server:  [EthriaCount] » === Gesamt: 28 Entities ===
```

### Deutsche Entity-Namen
```
Spieler: /plotcount Schaf    # Funktioniert!
Spieler: /pc Kuh             # Funktioniert auch!
```

---

## 🔗 Wichtige Links

- **[GitHub Repository](https://github.com/Leahcimkrob/EthriaPlotCount)**
- **[Releases & Downloads](https://github.com/Leahcimkrob/EthriaPlotCount/releases)**
- **[Issues & Bug Reports](https://github.com/Leahcimkrob/EthriaPlotCount/issues)**
- **[Discord Support](https://discord.gg/yourserver)**

---

## 📊 System-Anforderungen

| Komponente | Mindestanforderung | Empfohlen |
|------------|-------------------|-----------|
| **Java** | 21+ | 21+ |
| **Paper** | 1.21.8+ | Neueste Version |
| **PlotSquared** | 7.5.9+ | Neueste Version |
| **RAM** | 512MB | 1GB+ |

---

## 🏆 Features auf einen Blick

### ✅ Entity-Management
- Zählung aller Minecraft-Entities
- Kategorisierung in 6 Hauptgruppen
- Unterstützung für Custom-Entities
- Merge-Plot-Integration

### ✅ Benutzerfreundlichkeit  
- Intuitive Commands
- Tab-Completion mit Permissions
- Deutsche + Englische Übersetzungen
- Anpassbare Chat-Nachrichten

### ✅ Administration
- Granulare Berechtigungen
- Performance-optimiert
- Debug-System
- Vollständige Konfigurierbarkeit

### ✅ Integration
- PlotSquared-nativ
- Permission-Plugin-kompatibel
- Multi-World-Support
- API für Entwickler

---

*Diese Wiki wird regelmäßig aktualisiert. Bei Fragen oder Problemen nutzen Sie bitte die GitHub Issues oder den Discord-Support.*

---

## 🆘 Sofort-Hilfe

### ❓ Plugin funktioniert nicht?
1. Prüfen Sie die [System-Anforderungen](#-system-anforderungen)
2. Überprüfen Sie die [Installation](Installation)
3. Aktivieren Sie [Debug-Mode](Debugging)
4. Erstellen Sie ein [GitHub Issue](https://github.com/yourusername/EthriaPlotCount/issues)

### ❓ Berechtigung-Probleme?
1. Lesen Sie die [Permissions-Dokumentation](Permissions)
2. Prüfen Sie die [Plot-Zugriff-Einstellungen](Plot-Access)
3. Testen Sie mit Admin-Berechtigung

### ❓ Entity wird nicht gezählt?
1. Prüfen Sie die [Entity-Kategorien](Entity-Categories)
2. Überprüfen Sie Entity-spezifische Berechtigungen
3. Aktivieren Sie Debug-Logs

---

*Made with ❤️ for the Minecraft community*
