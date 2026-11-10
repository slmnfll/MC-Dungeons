package me.slmnfll.dungeons.content;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;

public enum ItemRarity {
    COMMON("Common", NamedTextColor.GREEN, Color.fromRGB(85, 255, 85)),
    UNCOMMON("Uncommon", NamedTextColor.DARK_GREEN, Color.fromRGB(0, 170, 0)),
    RARE("Rare", NamedTextColor.AQUA, Color.fromRGB(85, 255, 255)),
    EPIC("Epic", NamedTextColor.DARK_PURPLE, Color.fromRGB(170, 0, 170)),
    LEGENDARY("Legendary", NamedTextColor.GOLD, Color.fromRGB(255, 170, 0)),
    MYTHICAL("Mythical", NamedTextColor.DARK_RED, Color.fromRGB(170, 0, 0)),
    EXTINCT("Extinct", NamedTextColor.WHITE, Color.WHITE);

    private final String displayName;
    private final NamedTextColor textColor;
    private final Color particleColor;

    ItemRarity(String displayName, NamedTextColor textColor, Color particleColor) {
        this.displayName = displayName;
        this.textColor = textColor;
        this.particleColor = particleColor;
    }

    public String displayName() {
        return displayName;
    }

    public NamedTextColor textColor() {
        return textColor;
    }

    public Color particleColor() {
        return particleColor;
    }
}
