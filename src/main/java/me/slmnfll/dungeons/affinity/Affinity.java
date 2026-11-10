package me.slmnfll.dungeons.affinity;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum Affinity {
    VANGUARD("vanguard", "Vanguard", Material.IRON_SWORD),
    ARBALIST("arbalist", "Arbalist", Material.CROSSBOW),
    RAVAGER("ravager", "Ravager", Material.IRON_AXE);

    public static final double EFFECTIVENESS_BONUS = 0.15D;

    private final String id;
    private final String displayName;
    private final Material primaryWeapon;

    Affinity(String id, String displayName, Material primaryWeapon) {
        this.id = id;
        this.displayName = displayName;
        this.primaryWeapon = primaryWeapon;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material primaryWeapon() {
        return primaryWeapon;
    }

    public boolean matches(Material material) {
        return switch (this) {
            case VANGUARD -> material.name().endsWith("_SWORD");
            case ARBALIST -> material == Material.CROSSBOW;
            case RAVAGER -> material.name().endsWith("_AXE");
        };
    }

    public static Optional<Affinity> fromId(String value) {
        if (value == null) {
            return Optional.empty();
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(affinity -> affinity.id.equals(normalized))
                .findFirst();
    }
}
