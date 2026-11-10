package me.slmnfll.dungeons.encounter;

import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.instance.DungeonInstance;
import me.slmnfll.dungeons.instance.DungeonRunType;
import me.slmnfll.dungeons.tutorial.SurveySiteZeroDefinition;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Objects;

public final class DungeonEncounterGenerator {
    private final DungeonEncounterPlanner planner;
    private final DungeonGateService gateService;
    private final SurveySiteZeroDefinition tutorialDefinition;

    public DungeonEncounterGenerator(
            DungeonEncounterPlanner planner,
            DungeonGateService gateService,
            SurveySiteZeroDefinition tutorialDefinition
    ) {
        this.planner = Objects.requireNonNull(planner, "planner");
        this.gateService = Objects.requireNonNull(gateService, "gateService");
        this.tutorialDefinition = Objects.requireNonNull(tutorialDefinition, "tutorialDefinition");
    }

    public DungeonRunEncounters prepare(World world, DungeonInstance instance) {
        DungeonLayout layout = instance.layout();
        DungeonRunEncounters encounters = new DungeonRunEncounters(
                instance.runType() == DungeonRunType.TUTORIAL
                        ? tutorialDefinition.encounterPlan()
                        : planner.plan(layout)
        );
        if (!encounters.bossUnlocked()) {
            gateService.seal(world, layout, layout.bossRoomId(), Material.BLACK_STAINED_GLASS);
        }
        return encounters;
    }
}
