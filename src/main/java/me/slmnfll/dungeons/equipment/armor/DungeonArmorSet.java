package me.slmnfll.dungeons.equipment.armor;

import me.slmnfll.dungeons.affinity.Affinity;

public enum DungeonArmorSet {
    BULWARK("bulwark", "Bulwark", Affinity.VANGUARD,
            "-5% damage taken", "-15% total damage taken"),
    TRAILSEEKER("trailseeker", "Trailseeker", Affinity.ARBALIST,
            "+8% projectile damage", "+20% total projectile damage"),
    WARBORN("warborn", "Warborn", Affinity.RAVAGER,
            "+8% melee damage", "+20% total melee damage");

    private final String id;
    private final String displayName;
    private final Affinity affinity;
    private final String twoPieceDescription;
    private final String fourPieceDescription;

    DungeonArmorSet(
            String id,
            String displayName,
            Affinity affinity,
            String twoPieceDescription,
            String fourPieceDescription
    ) {
        this.id = id;
        this.displayName = displayName;
        this.affinity = affinity;
        this.twoPieceDescription = twoPieceDescription;
        this.fourPieceDescription = fourPieceDescription;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Affinity affinity() {
        return affinity;
    }

    public String twoPieceDescription() {
        return twoPieceDescription;
    }

    public String fourPieceDescription() {
        return fourPieceDescription;
    }
}
