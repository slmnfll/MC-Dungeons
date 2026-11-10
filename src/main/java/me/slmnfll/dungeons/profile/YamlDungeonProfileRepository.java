package me.slmnfll.dungeons.profile;

import me.slmnfll.dungeons.affinity.Affinity;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class YamlDungeonProfileRepository implements DungeonProfileRepository {
    private final JavaPlugin plugin;
    private final File profileFile;
    private final YamlConfiguration configuration;
    private boolean dirty;

    public YamlDungeonProfileRepository(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the Dungeons data directory.", exception);
        }
        this.profileFile = new File(plugin.getDataFolder(), "profiles.yml");
        this.configuration = YamlConfiguration.loadConfiguration(profileFile);
    }

    @Override
    public DungeonProfile load(UUID playerId) {
        String path = "players." + playerId;
        int level = Math.max(1, configuration.getInt(path + ".level", 1));
        long experience = Math.max(0L, configuration.getLong(path + ".experience", 0L));
        Set<Affinity> claimedStarters = EnumSet.noneOf(Affinity.class);
        for (String affinityId : configuration.getStringList(path + ".claimed-starters")) {
            Affinity.fromId(affinityId).ifPresent(claimedStarters::add);
        }
        Affinity selectedAffinity = Affinity.fromId(configuration.getString(path + ".selected-affinity"))
                .orElse(null);
        int completedExpeditions = Math.max(0, configuration.getInt(path + ".completed-expeditions", 0));
        return new DungeonProfile(
                playerId,
                level,
                experience,
                claimedStarters,
                selectedAffinity,
                completedExpeditions
        );
    }

    @Override
    public void save(DungeonProfile profile) {
        String path = "players." + profile.playerId();
        configuration.set(path + ".level", profile.level());
        configuration.set(path + ".experience", profile.experienceIntoLevel());
        configuration.set(
                path + ".claimed-starters",
                profile.claimedStarters().stream().map(Affinity::id).sorted().toList()
        );
        configuration.set(
                path + ".selected-affinity",
                profile.selectedAffinity().map(Affinity::id).orElse(null)
        );
        configuration.set(path + ".completed-expeditions", profile.completedExpeditions());
        dirty = true;
    }

    @Override
    public void flush() {
        if (!dirty) {
            return;
        }
        try {
            configuration.save(profileFile);
            dirty = false;
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save Dungeon profiles: " + exception.getMessage());
        }
    }
}
