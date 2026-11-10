package me.slmnfll.dungeons.equipment.weapon;

import java.util.Arrays;
import java.util.Optional;

public enum WeaponAbility {
    NONE("none", "", ""),
    CLEAVE("cleave", "Cleave", "Every third strike cuts nearby enemies."),
    SHOCKWAVE("shockwave", "Shockwave", "A heavy strike releases a damaging shockwave."),
    HUNTERS_MARK("hunters_mark", "Hunter's Mark", "Bolts mark a target; your next hit deals increased damage.");

    private final String id;
    private final String displayName;
    private final String description;

    WeaponAbility(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public static Optional<WeaponAbility> fromId(String id) {
        return Arrays.stream(values()).filter(ability -> ability.id.equals(id)).findFirst();
    }
}
