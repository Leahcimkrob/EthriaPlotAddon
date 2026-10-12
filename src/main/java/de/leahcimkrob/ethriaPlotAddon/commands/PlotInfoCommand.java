package de.leahcimkrob.ethriaPlotAddon.commands;

import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.events.post.PostPlotClearEvent;
import com.plotsquared.core.events.post.PostPlotDeleteEvent;
import com.google.common.eventbus.Subscribe;
import de.leahcimkrob.ethriaPlotAddon.EthriaPlotAddon;
import de.leahcimkrob.ethriaPlotAddon.database.DatabaseManager.PlotRecord;
import de.leahcimkrob.ethriaPlotAddon.database.DatabaseManager.PlotRecordEntry;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlotInfoCommand implements SubcommandExecutor, Listener {
    public static final String VIEW_PERMISSION = "ethriaplotaddon.plotinfo.view";
    public static final String EDIT_PERMISSION = "ethriaplotaddon.plotinfo.edit";
    public static final String LIST_PERMISSION = "ethriaplotaddon.plotinfo.list";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final EthriaPlotAddon plugin;
    private final ConcurrentHashMap<UUID, Long> pendingDeletes = new ConcurrentHashMap<>();

    public PlotInfoCommand(EthriaPlotAddon plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        PlotSquared.get().getEventDispatcher().registerListener(this);
    }

    @Subscribe
    public void onPlotDeleted(PostPlotDeleteEvent event) {
        deletePlotRecords(event.getPlot());
    }

    @Subscribe
    public void onPlotCleared(PostPlotClearEvent event) {
        deletePlotRecords(event.getPlot());
    }

    private void deletePlotRecords(Plot plot) {
        if (plot == null) return;
        Plot basePlot = canonicalPlot(plot);
        String server = serverName();
        String world = basePlot.getWorldName();
        int plotX = basePlot.getId().getX();
        int plotY = basePlot.getId().getY();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int deleted = plugin.getDatabaseManager().deletePlotRecords(server, world, plotX, plotY);
                if (deleted > 0) {
                    plugin.getLogger().info("Plotinfos gelöscht: " + server + "/" + world + "/" + plotX + ";" + plotY
                            + " (" + deleted + " Einträge)");
                }
            } catch (SQLException exception) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Plotinfos nach Plot-Löschen/Leeren konnten nicht entfernt werden.", exception);
            }
        });
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length > 0 && "book".equalsIgnoreCase(args[0])) return openRecordBook(player, args);
        if (args.length > 0 && "list".equalsIgnoreCase(args[0])) return listPlayerRecords(player, args);
        if (args.length > 0 && "add".equalsIgnoreCase(args[0])) return addRecord(player, args);
        if (args.length > 0 && "edit".equalsIgnoreCase(args[0])) return editRecord(player, args);
        if (args.length > 0 && "delete".equalsIgnoreCase(args[0])) return requestDelete(player, args);
        if (args.length > 0 && "confirm".equalsIgnoreCase(args[0])) return confirmDelete(player);
        if (args.length > 0 && "cancel".equalsIgnoreCase(args[0])) return cancelDelete(player);
        if (args.length > 1) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_usage", java.util.Map.of("usage", getUsage())));
            return true;
        }
        boolean requestedOtherPlot = args.length == 1;
        if (requestedOtherPlot && !player.hasPermission(EDIT_PERMISSION)) {
            player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
            return true;
        }
        Plot plot = requestedOtherPlot ? findPlotInCurrentWorld(player, args[0])
                : de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration.getPlayerPlot(player);
        if (plot == null) {
            if (!requestedOtherPlot) {
                sendPlotCountMessage(player, true);
                return true;
            }
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_wrong_world"));
            return true;
        }
        plot = canonicalPlot(plot);
        boolean editor = player.hasPermission(EDIT_PERMISSION);
        boolean ownPlot = isPlotOwner(plot, player.getUniqueId());
        if (!editor && !ownPlot) {
            sendPlotCountMessage(player, true);
            return true;
        }

        Set<Plot> mergedPlots = plot.getConnectedPlots();
        int plotCount = Math.max(1, mergedPlots.size());
        player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_header", java.util.Map.of(
                "plotId", plot.getId().toString(), "plotCount", Integer.toString(plotCount))));
        if (editor && !ownPlot) {
            UUID ownerId = plot.getOwner();
            if (ownerId != null) {
                String ownerName = resolveUserName(null, ownerId);
                Player owner = Bukkit.getPlayer(ownerId);
                if (owner == null) {
                    int ownerPlotCount = PlotSquared.platform().plotAreaManager()
                            .getPlotAreasSet(player.getWorld().getName()).stream()
                            .mapToInt(area -> area.getPlotCount(ownerId))
                            .sum();
                    player.sendMessage(plugin.getMessageManager().getComponent(
                            "plotinfo_plot_count_command_admin_offline", java.util.Map.of(
                                    "spieler", ownerName,
                                    "plotCount", Integer.toString(ownerPlotCount)), false));
                } else {
                    sendPlotCountMessage(player, false, "plotinfo_plot_count_command_admin", ownerId, ownerName);
                }
            }
        } else {
            sendPlotCountMessage(player, false);
        }

        try {
            List<PlotRecordEntry> entries = plugin.getDatabaseManager().getPlotRecordEntries(
                    serverName(), plot.getWorldName(), plot.getId().getX(), plot.getId().getY());
            if (!editor) {
                entries = entries.stream()
                        .filter(entry -> player.getUniqueId().equals(entry.record().ownerId()))
                        .toList();
            }
            if (entries.isEmpty()) {
                player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_none"));
                return true;
            }
            player.sendMessage(Component.text("-------------------------------------", NamedTextColor.DARK_GRAY));
            for (int i = 0; i < entries.size(); i++) {
                sendRecord(player, i + 1, entries.get(i), editor, false);
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Plotinfo konnte nicht geladen werden.", exception);
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_load_failed",
                    java.util.Map.of("error", exception.getMessage() == null ? "SQL error" : exception.getMessage())));
        }
        return true;
    }

    private void sendPlotCountMessage(Player player, boolean withPrefix) {
        sendPlotCountMessage(player, withPrefix, "plotinfo_plot_count_command", player.getUniqueId(), null);
    }

    private void sendPlotCountMessage(Player player, boolean withPrefix, String messageKey,
                                      UUID subjectId, String ownerName) {
        String currentWorldName = player.getWorld().getName();
        var playerManager = PlotSquared.platform().playerManager();
        var onlinePlotPlayer = playerManager.getPlayerIfExists(subjectId);
        if (onlinePlotPlayer == null) {
            plugin.getLogger().warning("Online-PlotSquared-Profil konnte für " + player.getName() + " nicht ermittelt werden.");
            return;
        }
        int plotCount = onlinePlotPlayer.getPlotCount(currentWorldName);
        sendPlotCountLine(player, withPrefix, messageKey, ownerName, plotCount,
                onlinePlotPlayer.getAllowedPlots());
    }

    private void sendPlotCountLine(Player player, boolean withPrefix, String messageKey,
                                   String ownerName, int plotCount, int allowedPlots) {
        java.util.Map<String, String> replacements = new java.util.HashMap<>();
        replacements.put("plotCount", Integer.toString(plotCount));
        replacements.put("allowedCount", allowedPlots == Integer.MAX_VALUE || allowedPlots == -1
                ? "∞" : allowedPlots < 0 ? "?" : Integer.toString(allowedPlots));
        if (ownerName != null) replacements.put("spieler", ownerName);
        player.sendMessage(plugin.getMessageManager().getComponent(messageKey, replacements, withPrefix));
    }

    private boolean listPlayerRecords(Player player, String[] args) {
        if (!player.hasPermission(LIST_PERMISSION)) {
            player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
            return true;
        }
        if (args.length != 2) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_list_usage"));
            return true;
        }
        String playerName = args[1];
        String worldName = player.getWorld().getName();
        try {
            List<PlotRecordEntry> entries = plugin.getDatabaseManager().getPlotRecordEntriesByOwner(
                    serverName(), worldName, playerName);
            if (entries.isEmpty()) {
                player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_list_none",
                        java.util.Map.of("spieler", playerName, "world", worldName)));
                return true;
            }
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_list_header",
                    java.util.Map.of("spieler", playerName, "world", worldName,
                            "count", Integer.toString(entries.size()))));
            player.sendMessage(Component.text("-------------------------------------", NamedTextColor.DARK_GRAY));
            for (int i = 0; i < entries.size(); i++) {
                sendRecord(player, i + 1, entries.get(i), true, true);
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Plotinfo-Liste konnte nicht geladen werden.", exception);
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_load_failed",
                    java.util.Map.of("error", exception.getMessage() == null ? "SQL error" : exception.getMessage())));
        }
        return true;
    }

    private void sendRecord(Player player, int number, PlotRecordEntry entry, boolean editor, boolean includePlotId) {
        PlotRecord record = entry.record();
        String date = DATE_FORMAT.format(record.recordedAt().atZone(ZoneId.systemDefault()));
        String location = record.x() + ", " + record.y() + ", " + record.z();
        int noteLimit = includePlotId ? 15 : 20;
        String note = record.note() == null || record.note().isBlank()
                ? "(keine Bemerkung)" : truncateForChat(record.note(), noteLimit);
        String controls = editor
                ? plugin.getMessageManager().getRawMessage("plotinfo_entry_edit_controls")
                        .replace("{entryId}", Long.toString(entry.id()))
                : "";
        String messageKey = includePlotId ? "plotinfo_list_entry" : "plotinfo_entry";
        String template = controls + plugin.getMessageManager().getRawMessage(messageKey)
                .replace("{x}", Integer.toString(record.x()))
                .replace("{y}", Integer.toString(record.y()))
                .replace("{z}", Integer.toString(record.z()))
                .replace("{entryId}", Long.toString(entry.id()))
                .replace("{plotId}", record.plotX() + ";" + record.plotY());
        player.sendMessage(MiniMessage.miniMessage().deserialize(template,
                Placeholder.unparsed("number", Integer.toString(number)),
                Placeholder.unparsed("plotid", record.plotX() + ";" + record.plotY()),
                Placeholder.unparsed("location", location),
                Placeholder.unparsed("note", note),
                Placeholder.unparsed("date", date)));
    }

    private String truncateForChat(String value, int maxLength) {
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 1) + "…";
    }

    private boolean openRecordBook(Player player, String[] args) {
        if (args.length != 2) return true;
        try {
            long id = Long.parseLong(args[1]);
            PlotRecordEntry entry = plugin.getDatabaseManager().getPlotRecordEntry(id);
            if (entry == null) {
                player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_record_missing"));
                return true;
            }
            PlotRecord record = entry.record();
            Plot currentPlot = canonicalPlot(
                    de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration.getPlayerPlot(player));
            if (!canAccessRecord(player, currentPlot, record)) {
                player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_record_denied"));
                return true;
            }

            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) book.getItemMeta();
            var bookConfig = plugin.getBookConfigManager();
            MiniMessage miniMessage = MiniMessage.miniMessage();
            meta.title(miniMessage.deserialize(bookConfig.getTitle()));
            meta.author(miniMessage.deserialize(bookConfig.getAuthor()));

            String owner = resolveUserName(record.ownerName(), record.ownerId());
            String executor = resolveUserName(record.teamlerName(), record.teamlerId());
            String note = valueOrUnknown(record.note());
            int firstPageNoteLength = Math.min(160, note.codePointCount(0, note.length()));
            String firstPageNote = takeCodePoints(note, firstPageNoteLength);
            String firstPage = bookConfig.getDetailsPage() + "\n\n" + bookConfig.getNotePage();
            meta.addPages(miniMessage.deserialize(firstPage,
                            Placeholder.unparsed("owner", owner),
                            Placeholder.unparsed("executor", executor),
                            Placeholder.unparsed("note", firstPageNote)));
            String remainingNote = dropCodePoints(note, firstPageNoteLength);
            while (!remainingNote.isEmpty()) {
                String pageText = takeCodePoints(remainingNote, 220);
                meta.addPages(miniMessage.deserialize(bookConfig.getNoteContinuationPage(),
                        Placeholder.unparsed("note", pageText)));
                remainingNote = dropCodePoints(remainingNote, pageText.codePointCount(0, pageText.length()));
            }
            book.setItemMeta(meta);
            player.openBook(book);
        } catch (SQLException | IllegalArgumentException exception) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_open_failed"));
        }
        return true;
    }

    private String takeCodePoints(String value, int limit) {
        int end = value.offsetByCodePoints(0, Math.min(limit, value.codePointCount(0, value.length())));
        return value.substring(0, end);
    }

    private String dropCodePoints(String value, int count) {
        int start = value.offsetByCodePoints(0, Math.min(count, value.codePointCount(0, value.length())));
        return value.substring(start);
    }

    private boolean addRecord(Player player, String[] args) {
        if (!player.hasPermission(EDIT_PERMISSION)) {
            player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
            return true;
        }
        if (args.length > 1) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_add_usage"));
            return true;
        }
        Plot currentPlot = de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration.getPlayerPlot(player);
        if (currentPlot == null) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_not_on_plot"));
            return true;
        }
        Plot plot = canonicalPlot(currentPlot);
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Plotinfo hinzufügen"))
                        .inputs(List.of(DialogInput.text("note", 300,
                                Component.text("Bemerkung (max. 256 Zeichen)"), true, "", 256, null)))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(Component.text("Speichern"), null, 150,
                                DialogAction.customClick((response, audience) -> {
                                    if (audience instanceof Player clicker) {
                                        saveRecord(clicker, plot, response.getText("note"));
                                    }
                                }, ClickCallback.Options.builder().uses(1).build())),
                        ActionButton.create(Component.text("Abbrechen"), null, 150, null))));
        player.showDialog(dialog);
        return true;
    }

    private void saveRecord(Player player, Plot openedPlot, String note) {
        String cleanedNote = note == null ? "" : note.strip();
        if (cleanedNote.length() > 256) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_note_too_long"));
            return;
        }
            Plot plot = canonicalPlot(
                    de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration.getPlayerPlot(player));
        if (!samePlot(plot, openedPlot)) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_moved"));
            return;
        }
        UUID ownerId = plot.getOwners().stream().findFirst().orElse(null);
        String ownerName = ownerId == null ? null : Bukkit.getOfflinePlayer(ownerId).getName();
        try {
            org.bukkit.Location position = player.getLocation();
            plugin.getDatabaseManager().insert(PlotRecord.now(serverName(), plot.getWorldName(),
                    plot.getId().getX(), plot.getId().getY(), ownerId, ownerName,
                    player.getUniqueId(), player.getName(), position.getBlockX(), position.getBlockY(),
                    position.getBlockZ(), cleanedNote));
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_saved"));
        } catch (SQLException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Plotinfo konnte nicht gespeichert werden.", exception);
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_save_failed"));
        }
    }

    private boolean editRecord(Player player, String[] args) {
        if (!player.hasPermission(EDIT_PERMISSION)) {
            player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_edit_usage"));
            return true;
        }
        try {
            long id = Long.parseLong(args[1]);
            PlotRecordEntry entry = plugin.getDatabaseManager().getPlotRecordEntry(id);
            Plot plot = canonicalPlot(
                    de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration.getPlayerPlot(player));
            if (entry == null || !canAccessRecord(player, plot, entry.record())) {
                player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_record_denied"));
                return true;
            }
            if (args.length == 2) {
                openEditDialog(player, entry);
            } else {
                plugin.getDatabaseManager().updateNote(id, join(args, 2));
                player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_updated"));
            }
        } catch (SQLException | IllegalArgumentException exception) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_update_failed"));
        }
        return true;
    }

    private void openEditDialog(Player player, PlotRecordEntry entry) {
        String note = entry.record().note() == null ? "" : entry.record().note();
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(plugin.getMessageManager().getRawMessage("plotinfo_edit_dialog_title")))
                        .inputs(List.of(DialogInput.text("note", 300,
                                Component.text(plugin.getMessageManager().getRawMessage("plotinfo_edit_dialog_label")),
                                true, note, 256, null)))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(Component.text("Speichern"), null, 150,
                                DialogAction.customClick((response, audience) -> {
                                    if (audience instanceof Player editor) {
                                        saveEditedNote(editor, entry.id(), response.getText("note"));
                                    }
                                }, ClickCallback.Options.builder().uses(1).build())),
                        ActionButton.create(Component.text("Abbrechen"), null, 150, null))));
        player.showDialog(dialog);
    }

    private void saveEditedNote(Player player, long id, String note) {
        if (!player.hasPermission(EDIT_PERMISSION)) return;
        try {
            PlotRecordEntry current = plugin.getDatabaseManager().getPlotRecordEntry(id);
            Plot plot = canonicalPlot(
                    de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration.getPlayerPlot(player));
            if (current == null || !canAccessRecord(player, plot, current.record())) {
                player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_record_denied"));
                return;
            }
            plugin.getDatabaseManager().updateNote(id, note == null ? "" : note.strip());
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_updated"));
        } catch (SQLException | IllegalArgumentException exception) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_update_failed"));
        }
    }

    private boolean requestDelete(Player player, String[] args) {
        if (!player.hasPermission(EDIT_PERMISSION)) {
            player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
            return true;
        }
        if (args.length != 2) return true;
        try {
            long id = Long.parseLong(args[1]);
            PlotRecordEntry entry = plugin.getDatabaseManager().getPlotRecordEntry(id);
            Plot plot = canonicalPlot(
                    de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration.getPlayerPlot(player));
            if (entry == null || !canAccessRecord(player, plot, entry.record())) {
                player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_record_denied"));
                return true;
            }
            pendingDeletes.put(player.getUniqueId(), id);
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_delete_confirm",
                    java.util.Map.of("entryid", Long.toString(id))));
        } catch (SQLException | NumberFormatException exception) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_record_missing"));
        }
        return true;
    }

    private boolean confirmDelete(Player player) {
        Long id = pendingDeletes.remove(player.getUniqueId());
        if (id == null) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_delete_nothing_pending"));
            return true;
        }
        if (!player.hasPermission(EDIT_PERMISSION)) {
            player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
            return true;
        }
        try {
            PlotRecordEntry entry = plugin.getDatabaseManager().getPlotRecordEntry(id);
            Plot plot = canonicalPlot(
                    de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration.getPlayerPlot(player));
            if (entry == null || !canAccessRecord(player, plot, entry.record())) {
                player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_record_denied"));
                return true;
            }
            if (plugin.getDatabaseManager().deletePlotRecord(id)) {
                player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_deleted"));
            } else {
                player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_record_missing"));
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Plotinfo-Eintrag konnte nicht gelöscht werden.", exception);
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_delete_failed"));
        }
        return true;
    }

    private boolean cancelDelete(Player player) {
        pendingDeletes.remove(player.getUniqueId());
        player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_delete_cancelled"));
        return true;
    }

    @EventHandler
    public void onPendingDeleteChat(AsyncChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!pendingDeletes.containsKey(playerId)) return;
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).strip();
        if (!message.equalsIgnoreCase("confirm") && !message.equalsIgnoreCase("cancel")) return;
        event.setCancelled(true);
        Player chatPlayer = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (message.equalsIgnoreCase("confirm")) confirmDelete(chatPlayer);
            else cancelDelete(chatPlayer);
        });
    }

    private static String join(String[] args, int start) {
        return String.join(" ", java.util.Arrays.copyOfRange(args, start, args.length));
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unbekannt" : value;
    }

    private boolean samePlot(Plot current, Plot expected) {
        current = canonicalPlot(current);
        expected = canonicalPlot(expected);
        return current != null && expected != null
                && current.getWorldName().equals(expected.getWorldName())
                && current.getId().equals(expected.getId());
    }

    private boolean samePlot(Plot current, PlotRecord expected) {
        current = canonicalPlot(current);
        return current != null && expected != null && isCurrentServer(expected.server())
                && current.getWorldName().equals(expected.world())
                && current.getId().getX() == expected.plotX()
                && current.getId().getY() == expected.plotY();
    }

    private boolean canAccessRecord(Player player, Plot currentPlot, PlotRecord record) {
        if (record == null || !isCurrentServer(record.server())
                || !record.world().equals(player.getWorld().getName())) return false;
        if (player.hasPermission(EDIT_PERMISSION)) return true;
        return samePlot(currentPlot, record)
                && isPlotOwner(currentPlot, player.getUniqueId())
                && player.getUniqueId().equals(record.ownerId());
    }

    private boolean isPlotOwner(Plot plot, UUID playerId) {
        plot = canonicalPlot(plot);
        return plot != null && plot.getOwners().contains(playerId);
    }

    private Plot canonicalPlot(Plot plot) {
        return plot == null ? null : plot.getBasePlot(false);
    }

    private Plot findPlotInCurrentWorld(Player player, String plotIdText) {
        String[] coordinates = plotIdText.split("[;,]", -1);
        if (coordinates.length != 2) return null;
        try {
            PlotId plotId = PlotId.of(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]));
            for (PlotArea area : PlotSquared.get().getPlotAreaManager().getPlotAreasSet(player.getWorld().getName())) {
                Plot plot = area.getPlot(plotId);
                if (plot != null) return canonicalPlot(plot);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private String serverName() {
        String configured = plugin.getConfig().getString("database.server_id", "auto");
        if (configured == null || configured.isBlank() || configured.equalsIgnoreCase("auto")) {
            return Bukkit.getServer().getName();
        }
        return configured;
    }

    private static String resolveUserName(String name, UUID id) {
        if (id != null && (name == null || name.isBlank() || name.equalsIgnoreCase(id.toString()))) {
            String currentName = Bukkit.getOfflinePlayer(id).getName();
            if (currentName != null) return currentName;
        }
        return valueOrUnknown(name);
    }

    private boolean isCurrentServer(String recordServer) {
        return recordServer != null && recordServer.equals(serverName());
    }

    @Override public boolean hasPermission(Player player) {
        return player.hasPermission(VIEW_PERMISSION) || player.hasPermission(EDIT_PERMISSION)
                || player.hasPermission(LIST_PERMISSION);
    }
    @Override public String getDescription() { return "Zeigt chronologische Einträge zum Plot"; }
    @Override public String getUsage() { return "/plotaddon plotinfo [x;y] (Editor)"; }
}
