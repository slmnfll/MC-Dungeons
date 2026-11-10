package me.slmnfll.dungeons.content;

import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.instance.DungeonInstance;
import me.slmnfll.dungeons.instance.DungeonRunType;
import me.slmnfll.dungeons.tutorial.SurveySiteZeroDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;

import java.util.Objects;

public final class DungeonContentGenerator {
    private final DungeonContentPlanner planner;
    private final SurveySiteZeroDefinition tutorialDefinition;

    public DungeonContentGenerator(
            DungeonContentPlanner planner,
            SurveySiteZeroDefinition tutorialDefinition
    ) {
        this.planner = Objects.requireNonNull(planner, "planner");
        this.tutorialDefinition = Objects.requireNonNull(tutorialDefinition, "tutorialDefinition");
    }

    public DungeonRunContent populate(World world, DungeonInstance instance) {
        DungeonLayout layout = instance.layout();
        DungeonContentPlan plan = instance.runType() == DungeonRunType.TUTORIAL
                ? tutorialDefinition.contentPlan()
                : planner.plan(layout);
        for (BlockPosition position : plan.pots().keySet()) {
            world.getBlockAt(position.x(), position.y(), position.z()).setType(Material.DECORATED_POT, false);
        }

        for (BlockPosition position : plan.chests().keySet()) {
            Block block = world.getBlockAt(position.x(), position.y(), position.z());
            block.setType(Material.CHEST, false);
            if (block.getState() instanceof Chest chest) {
                chest.customName(Component.text("Locked Treasure Chest", NamedTextColor.GOLD));
                chest.getBlockInventory().clear();
                chest.update(true, false);
            }
        }
        return new DungeonRunContent(plan);
    }
}
