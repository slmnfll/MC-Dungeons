package me.slmnfll.dungeons.equipment.artifact;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.content.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DungeonArtifactRegistry {
    private final Map<String, DungeonArtifactDefinition> byId = new LinkedHashMap<>();
    private final Map<Affinity, DungeonArtifactDefinition> firstByAffinity = new EnumMap<>(Affinity.class);

    public DungeonArtifactRegistry() {
        register(new DungeonArtifactDefinition(
                "guardian_shell", "Guardian Shell", Affinity.VANGUARD,
                Material.NAUTILUS_SHELL, ItemRarity.RARE, ArtifactAbility.GUARDIAN_SHELL,
                8.0D, 0.0D, 200, 700
        ));
        register(new DungeonArtifactDefinition(
                "gale_feather", "Gale Feather", Affinity.ARBALIST,
                Material.FEATHER, ItemRarity.RARE, ArtifactAbility.GALE_STEP,
                1.35D, 0.0D, 40, 240
        ));
        register(new DungeonArtifactDefinition(
                "ember_core", "Ember Core", Affinity.RAVAGER,
                Material.BLAZE_POWDER, ItemRarity.RARE, ArtifactAbility.EMBER_BURST,
                7.0D, 5.0D, 60, 500
        ));
    }

    public Optional<DungeonArtifactDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public DungeonArtifactDefinition firstFor(Affinity affinity) {
        DungeonArtifactDefinition definition = firstByAffinity.get(affinity);
        if (definition == null) {
            throw new IllegalStateException("No artifact registered for " + affinity.id());
        }
        return definition;
    }

    private void register(DungeonArtifactDefinition definition) {
        if (byId.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate dungeon artifact ID: " + definition.id());
        }
        if (firstByAffinity.putIfAbsent(definition.affinity(), definition) != null) {
            throw new IllegalStateException("Duplicate first artifact for " + definition.affinity().id());
        }
    }
}
