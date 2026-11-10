package me.slmnfll.dungeons.tutorial;

import me.slmnfll.dungeons.instance.DungeonRunClosedEvent;
import me.slmnfll.dungeons.instance.DungeonRunStartedEvent;
import me.slmnfll.dungeons.instance.DungeonRunType;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

public final class TutorialProgressionService implements Listener {
    private final DungeonProfileService profileService;

    public TutorialProgressionService(DungeonProfileService profileService) {
        this.profileService = Objects.requireNonNull(profileService, "profileService");
    }

    @EventHandler
    public void onRunStarted(DungeonRunStartedEvent event) {
        if (event.session().instance().runType() == DungeonRunType.TUTORIAL) {
            profileService.beginTutorial(event.player());
        }
    }

    @EventHandler
    public void onRunClosed(DungeonRunClosedEvent event) {
        if (event.runType() == DungeonRunType.TUTORIAL) {
            profileService.endTutorial(event.player());
        }
    }
}
