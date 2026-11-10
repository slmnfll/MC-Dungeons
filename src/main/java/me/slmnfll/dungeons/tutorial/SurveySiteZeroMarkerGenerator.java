package me.slmnfll.dungeons.tutorial;

import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.layout.RoomNode;
import me.slmnfll.dungeons.world.DungeonGeometry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.List;

public final class SurveySiteZeroMarkerGenerator {
    private static final List<MarkerDefinition> MARKERS = List.of(
            new MarkerDefinition(
                    0,
                    "GUILD SURVEY MARKER",
                    "Follow the objective bar to the next chamber.\nYour expedition map is in hotbar slot 9."
            ),
            new MarkerDefinition(
                    1,
                    "DECORATED POTS",
                    "Wait for the pot objective, then interact.\nRight-click, strike, or shoot; loot auto-collects."
            ),
            new MarkerDefinition(
                    2,
                    "KEY SEARCH",
                    "One pot contains a virtual Dungeon Key.\nYour key count appears above the hotbar."
            ),
            new MarkerDefinition(
                    3,
                    "LOCKED TREASURE",
                    "Right-click the chest to spend one key.\nRecovered items remain Unsecured until exit."
            ),
            new MarkerDefinition(
                    4,
                    "SEALED ENCOUNTER",
                    "Entering seals the room.\nDefeat every enemy before advancing."
            ),
            new MarkerDefinition(
                    5,
                    "FINAL CHAMBER",
                    "Right-click food consumables to recover.\nAfter victory, cross the portal to secure loot."
            )
    );

    public List<MarkerDefinition> definitions() {
        return MARKERS;
    }

    public void populate(World world, DungeonLayout layout) {
        for (MarkerDefinition marker : MARKERS) {
            RoomNode room = layout.roomById(marker.roomId()).orElseThrow();
            Location location = new Location(
                    world,
                    DungeonGeometry.worldCoordinate(room.position().x()) + 0.5D,
                    DungeonGeometry.FLOOR_Y + 3.25D,
                    DungeonGeometry.worldCoordinate(room.position().z()) - 5.5D
            );
            world.spawn(location, TextDisplay.class, display -> configure(display, marker));
        }
    }

    private void configure(TextDisplay display, MarkerDefinition marker) {
        display.text(Component.text(marker.title(), NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text(marker.instructions(), NamedTextColor.WHITE)));
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setBillboard(Display.Billboard.CENTER);
        display.setLineWidth(240);
        display.setShadowed(true);
        display.setSeeThrough(false);
        display.setDefaultBackground(false);
        display.setBackgroundColor(Color.fromARGB(155, 12, 8, 20));
        display.setViewRange(0.7F);
        display.setPersistent(true);
        display.setInvulnerable(true);
        display.setGravity(false);
    }

    public record MarkerDefinition(int roomId, String title, String instructions) {
    }
}
