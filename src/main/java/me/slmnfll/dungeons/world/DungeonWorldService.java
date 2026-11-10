package me.slmnfll.dungeons.world;

import me.slmnfll.dungeons.content.DungeonContentGenerator;
import me.slmnfll.dungeons.content.DungeonRunContent;
import me.slmnfll.dungeons.encounter.DungeonEncounterGenerator;
import me.slmnfll.dungeons.encounter.DungeonRunEncounters;
import me.slmnfll.dungeons.instance.DungeonInstance;
import me.slmnfll.dungeons.instance.DungeonRunType;
import me.slmnfll.dungeons.tutorial.SurveySiteZeroMarkerGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class DungeonWorldService {
    public static final String WORLD_PREFIX = "dungeon_instance_";

    private final JavaPlugin plugin;
    private final DungeonBuilder builder;
    private final DungeonContentGenerator contentGenerator;
    private final DungeonEncounterGenerator encounterGenerator;
    private final SurveySiteZeroMarkerGenerator tutorialMarkerGenerator;

    public DungeonWorldService(
            JavaPlugin plugin,
            DungeonBuilder builder,
            DungeonContentGenerator contentGenerator,
            DungeonEncounterGenerator encounterGenerator,
            SurveySiteZeroMarkerGenerator tutorialMarkerGenerator
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.builder = Objects.requireNonNull(builder, "builder");
        this.contentGenerator = Objects.requireNonNull(contentGenerator, "contentGenerator");
        this.encounterGenerator = Objects.requireNonNull(encounterGenerator, "encounterGenerator");
        this.tutorialMarkerGenerator = Objects.requireNonNull(tutorialMarkerGenerator, "tutorialMarkerGenerator");
    }

    public CreatedDungeonWorld create(DungeonInstance instance) {
        World world = new WorldCreator(instance.worldName())
                .environment(World.Environment.NORMAL)
                .seed(instance.seed())
                .generator(new VoidChunkGenerator())
                .generateStructures(false)
                .createWorld();

        if (world == null) {
            throw new IllegalStateException("Paper did not create dungeon world " + instance.worldName());
        }

        try {
            configure(world);
            DungeonBuildResult buildResult = builder.build(world, instance.layout());
            if (instance.runType() == DungeonRunType.TUTORIAL) {
                tutorialMarkerGenerator.populate(world, instance.layout());
            }
            DungeonRunContent content = contentGenerator.populate(world, instance);
            DungeonRunEncounters encounters = encounterGenerator.prepare(world, instance);
            return new CreatedDungeonWorld(world, buildResult.spawn(), buildResult.mapCenter(), content, encounters);
        } catch (RuntimeException exception) {
            if (world.getPlayers().isEmpty()) {
                try {
                    destroy(world);
                } catch (RuntimeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw exception;
        }
    }

    public void destroy(World world) {
        Objects.requireNonNull(world, "world");
        Path worldPath = world.getWorldPath().toAbsolutePath().normalize();
        validateOwnedWorldPath(worldPath);

        if (!world.getPlayers().isEmpty()) {
            throw new IllegalStateException("Cannot delete dungeon world while players remain inside it.");
        }
        if (!Bukkit.unloadWorld(world, false)) {
            throw new IllegalStateException("Paper refused to unload dungeon world " + world.getName());
        }

        deleteDirectory(worldPath);
    }

    public int cleanupAbandonedWorlds() {
        Path worldContainer = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        int deleted = cleanupLoadedAbandonedWorlds(worldContainer);
        Set<Path> searchRoots = new LinkedHashSet<>();
        searchRoots.add(worldContainer);
        for (World world : Bukkit.getWorlds()) {
            Path loadedWorldPath = world.getWorldPath().toAbsolutePath().normalize();
            Path siblingDimensionRoot = loadedWorldPath.getParent();
            if (siblingDimensionRoot != null) {
                searchRoots.add(siblingDimensionRoot);
            }
            searchRoots.add(loadedWorldPath.resolve("dimensions").resolve("minecraft"));
        }

        Set<Path> abandoned = new LinkedHashSet<>();
        for (Path searchRoot : searchRoots) {
            if (!Files.isDirectory(searchRoot)) {
                continue;
            }
            try (Stream<Path> children = Files.list(searchRoot)) {
                children
                        .filter(Files::isDirectory)
                        .filter(path -> DungeonWorldPathPolicy.isOwned(worldContainer, path))
                        .filter(path -> Bukkit.getWorld(path.getFileName().toString()) == null)
                        .forEach(abandoned::add);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not scan for abandoned dungeon worlds in " + searchRoot, exception);
            }
        }

        for (Path path : abandoned) {
            try {
                validateOwnedWorldPath(path.toAbsolutePath().normalize());
                deleteDirectory(path);
                deleted++;
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not remove abandoned dungeon world " + path, exception);
            }
        }
        return deleted;
    }

    private int cleanupLoadedAbandonedWorlds(Path worldContainer) {
        int deleted = 0;
        for (World world : List.copyOf(Bukkit.getWorlds())) {
            if (!DungeonWorldPathPolicy.isOwnedWorldName(world.getName())) {
                continue;
            }

            Path worldPath = world.getWorldPath().toAbsolutePath().normalize();
            if (!DungeonWorldPathPolicy.isOwned(worldContainer, worldPath)) {
                plugin.getLogger().warning("Skipped suspicious loaded dungeon world path: " + worldPath);
                continue;
            }
            if (!world.getPlayers().isEmpty()) {
                plugin.getLogger().warning("Could not recover abandoned dungeon world with players inside: " + world.getName());
                continue;
            }
            if (!Bukkit.unloadWorld(world, false)) {
                plugin.getLogger().warning("Paper refused to unload abandoned dungeon world " + world.getName());
                continue;
            }

            try {
                deleteDirectory(worldPath);
                deleted++;
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not remove abandoned dungeon world " + worldPath, exception);
            }
        }
        return deleted;
    }

    private void configure(World world) {
        world.setAutoSave(false);
        world.setDifficulty(Difficulty.NORMAL);
        world.setTime(18_000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.MOB_GRIEFING, false);
        world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        world.setGameRule(GameRules.PVP, false);
        world.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
    }

    private void validateOwnedWorldPath(Path worldPath) {
        Path worldContainer = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        if (!DungeonWorldPathPolicy.isOwned(worldContainer, worldPath)) {
            throw new IllegalArgumentException("Refusing to delete a world not owned by Dungeons: " + worldPath);
        }
    }

    private void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void deleteDirectory(Path worldPath) {
        try (Stream<Path> paths = Files.walk(worldPath)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::deletePath);
        } catch (IOException | UncheckedIOException exception) {
            throw new IllegalStateException("Could not delete dungeon directory " + worldPath, exception);
        }
    }

    public record CreatedDungeonWorld(
            World world,
            Location spawn,
            Location mapCenter,
            DungeonRunContent content,
            DungeonRunEncounters encounters
    ) {
        public CreatedDungeonWorld {
            Objects.requireNonNull(world, "world");
            spawn = Objects.requireNonNull(spawn, "spawn").clone();
            mapCenter = Objects.requireNonNull(mapCenter, "mapCenter").clone();
            Objects.requireNonNull(content, "content");
            Objects.requireNonNull(encounters, "encounters");
        }

        @Override
        public Location spawn() {
            return spawn.clone();
        }

        @Override
        public Location mapCenter() {
            return mapCenter.clone();
        }
    }
}
