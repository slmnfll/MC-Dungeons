package me.slmnfll.dungeons.profile;

import me.slmnfll.dungeons.affinity.Affinity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DungeonProfileService {
    private final JavaPlugin plugin;
    private final DungeonProfileRepository repository;
    private final DungeonLevelCurve levelCurve;
    private final Map<UUID, DungeonProfile> loadedProfiles = new HashMap<>();
    private final DungeonProgressionOverrideRegistry progressionOverrides = new DungeonProgressionOverrideRegistry();
    private BukkitTask displayTask;
    private BukkitTask saveTask;

    public DungeonProfileService(
            JavaPlugin plugin,
            DungeonProfileRepository repository,
            DungeonLevelCurve levelCurve
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.levelCurve = Objects.requireNonNull(levelCurve, "levelCurve");
    }

    public void start() {
        Bukkit.getOnlinePlayers().forEach(this::load);
        displayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::synchronizeOnlineBars, 1L, 20L);
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::saveLoadedProfiles, 600L, 600L);
    }

    public DungeonProfile load(Player player) {
        DungeonProfile profile = loadedProfiles.computeIfAbsent(
                player.getUniqueId(),
                repository::load
        );
        synchronizeBar(player, profile);
        return profile;
    }

    public DungeonProfile profile(Player player) {
        return loadedProfiles.computeIfAbsent(player.getUniqueId(), repository::load);
    }

    public DungeonProfile.ExperienceAward award(Player player, long amount) {
        return award(player, amount, true);
    }

    public DungeonProfile.ExperienceAward award(Player player, long amount, boolean announce) {
        DungeonProfile profile = profile(player);
        if (!progressionOverrides.experienceEnabled(player.getUniqueId())) {
            synchronizeBar(player, profile);
            return new DungeonProfile.ExperienceAward(0L, 0, effectiveLevel(player), 0L);
        }
        DungeonProfile.ExperienceAward result = profile.addExperience(amount, levelCurve);
        repository.save(profile);
        synchronizeBar(player, profile);

        if (announce) {
            player.sendMessage(Component.text("+" + amount + " Dungeon XP", NamedTextColor.GREEN));
        }
        if (result.levelsGained() > 0) {
            player.sendMessage(Component.text(
                    "Dungeon Level " + result.newLevel() + " reached!",
                    NamedTextColor.GOLD
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        }
        return result;
    }

    public int effectiveLevel(Player player) {
        return progressionOverrides.effectiveLevel(
                player.getUniqueId(),
                profile(player).level()
        );
    }

    public void beginTutorial(Player player) {
        DungeonProfile profile = profile(player);
        profile.resetForNewAdventure();
        repository.save(profile);
        repository.flush();
        progressionOverrides.beginTutorial(player.getUniqueId());
        synchronizeBar(player, profile);
    }

    public void endTutorial(Player player) {
        progressionOverrides.clear(player.getUniqueId());
        synchronizeBar(player, profile(player));
    }

    public boolean claimStarter(Player player, Affinity affinity) {
        DungeonProfile profile = profile(player);
        if (!profile.claimStarter(affinity)) {
            return false;
        }
        repository.save(profile);
        repository.flush();
        return true;
    }

    public boolean selectAffinity(Player player, Affinity affinity) {
        DungeonProfile profile = profile(player);
        if (!profile.selectAffinity(affinity)) {
            return false;
        }
        repository.save(profile);
        repository.flush();
        return true;
    }

    public int recordCompletedExpedition(Player player) {
        DungeonProfile profile = profile(player);
        int completed = profile.recordCompletedExpedition();
        repository.save(profile);
        repository.flush();
        return completed;
    }

    public long experienceRequiredForNextLevel(int level) {
        return levelCurve.experienceRequiredForNextLevel(level);
    }

    public void unload(Player player) {
        progressionOverrides.clear(player.getUniqueId());
        DungeonProfile profile = loadedProfiles.remove(player.getUniqueId());
        if (profile != null) {
            repository.save(profile);
            repository.flush();
        }
    }

    public void close() {
        if (displayTask != null) {
            displayTask.cancel();
        }
        if (saveTask != null) {
            saveTask.cancel();
        }
        saveLoadedProfiles();
    }

    private void synchronizeOnlineBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            synchronizeBar(player, profile(player));
        }
    }

    private void synchronizeBar(Player player, DungeonProfile profile) {
        int effectiveLevel = progressionOverrides.effectiveLevel(player.getUniqueId(), profile.level());
        player.setLevel(effectiveLevel);
        player.setExp(progressionOverrides.experienceEnabled(player.getUniqueId())
                ? levelCurve.progress(profile.level(), profile.experienceIntoLevel())
                : 0.0F);
    }

    private void saveLoadedProfiles() {
        loadedProfiles.values().forEach(repository::save);
        repository.flush();
    }
}
