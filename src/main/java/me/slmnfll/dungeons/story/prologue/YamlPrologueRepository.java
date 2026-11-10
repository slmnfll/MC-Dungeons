package me.slmnfll.dungeons.story.prologue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

final class YamlPrologueRepository {
    private final JavaPlugin plugin;
    private final File storyFile;
    private final YamlConfiguration configuration;
    private boolean dirty;

    YamlPrologueRepository(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the Dungeons data directory.", exception);
        }
        this.storyFile = new File(plugin.getDataFolder(), "story.yml");
        this.configuration = YamlConfiguration.loadConfiguration(storyFile);
    }

    PrologueProgress load(UUID playerId) {
        String storedStage = configuration.getString("players." + playerId + ".prologue-stage");
        return new PrologueProgress(PrologueStage.fromId(storedStage).orElse(PrologueStage.NEW));
    }

    void save(UUID playerId, PrologueProgress progress) {
        configuration.set("players." + playerId + ".prologue-stage", progress.stage().id());
        dirty = true;
    }

    void flush() {
        if (!dirty) {
            return;
        }
        try {
            configuration.save(storyFile);
            dirty = false;
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save story progress: " + exception.getMessage());
        }
    }
}
