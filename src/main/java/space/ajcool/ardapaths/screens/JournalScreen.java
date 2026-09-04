package space.ajcool.ardapaths.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.Journal;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.PlayerTeleportPacket;
import space.ajcool.ardapaths.paths.rendering.ProximityRenderer;
import space.ajcool.ardapaths.screens.layout.ScreenLayout;
import space.ajcool.ardapaths.screens.widgets.JournalListEntry;
import space.ajcool.ardapaths.screens.widgets.JournalListWidget;
import space.ajcool.ardapaths.screens.widgets.TextWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Screen displaying the player's journal of proximity messages and chapter starts.
 * Shows a scrollable list of visited waypoints and events, with teleport buttons for each entry.
 */
@Environment(value = EnvType.CLIENT)
public class JournalScreen extends ArdaPathsScreen {

    /**
     * Constructs a new JournalScreen instance.
     */
    protected JournalScreen() {
        super(Component.literal(Component.translatable("ardapaths.client.journal.screen.title").getString()));
    }

    /**
     * Initializes the journal screen.
     */
    @Override
    protected void init() {

        super.init();

        int totalUiWidth = 500;

        int center = width / 2;

        List<JournalListEntry> entries = new ArrayList<>();
        List<Journal.Entry> journalEntries = new ArrayList<>(Journal.getEntries());
        Collections.reverse(journalEntries);

        for (Journal.Entry entry : journalEntries) {

            switch (entry.type()) {
                case CHAPTER_START -> entries.add(new JournalListEntry(
                        Component.translatable("ardapaths.client.journal.screen.entry.type.chapter"),
                        Component.literal(entry.text()),
                        Component.translatable("ardapaths.client.journal.screen.teleport"),
                        entry.color(),
                        button -> handleTeleportRequest(entry.pathId(), entry.chapterId(), entry.teleportPacket())
                ));
                case PROXIMITY_MESSAGE -> entries.add(new JournalListEntry(
                        Component.translatable("ardapaths.client.journal.screen.entry.type.entry"),
                        Component.literal(entry.text()),
                        Component.translatable("ardapaths.client.journal.screen.teleport"),
                        entry.color(),
                        button -> handleTeleportRequest(entry.pathId(), entry.chapterId(), entry.teleportPacket())
                ));
            }
        }

        int rowWidth = totalUiWidth - 40;
        int totalContentHeight = 0;
        for (JournalListEntry entry : entries) {
            totalContentHeight += entry.getHeight(rowWidth);
        }
        // Clamp list height to screen bounds
        int maxListHeight = height - 120;
        int listHeight = Math.min(totalContentHeight + 8, maxListHeight);

        int titleY = 0;
        int listTop = titleY + 25;
        int listBottom = listTop + listHeight;
        int verticalOffset = ScreenLayout.verticalCenterOffset(titleY, listBottom, height);

        TextWidget titleWidget = TextWidget.create()
                .setX(center - 75)
                .setY(titleY + verticalOffset)
                .setWidth(150)
                .setHeight(20)
                .setMessage(Component.literal(Component.translatable("ardapaths.client.journal.screen.title").getString()))
                .build();
        this.addRenderableWidget(titleWidget);

        JournalListWidget listWidget = new JournalListWidget(
                this.minecraft, totalUiWidth, listTop, listBottom, 32
        );
        listWidget.offsetY(verticalOffset);

        // Centre horizontally
        listWidget.setX((width - totalUiWidth) / 2);

        // Add pre-built entries
        for (JournalListEntry entry : entries) {
            listWidget.addJournalEntry(entry);
        }

        this.addRenderableWidget(listWidget);
    }

    /**
     * Teleport the player using the provided teleport packet. Also sets the selected path and chapter if provided.
     *
     * @param pathId         The ID of the path
     * @param chapterId      The ID of the chapter
     * @param teleportPacket The teleport packet containing teleportation data
     */
    private void handleTeleportRequest(String pathId, String chapterId, PlayerTeleportPacket teleportPacket) {

        if (pathId != null && chapterId != null) {

            ArdaPathsClient.CONFIG.setSelectedPath(pathId);
            ArdaPathsClient.CONFIG.setCurrentChapter(chapterId);
        }

        if (teleportPacket != null) {

            ProximityRenderer.clear();
            PacketRegistry.PLAYER_TELEPORT.send(teleportPacket);
        }
    }

}
