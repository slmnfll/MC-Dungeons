package me.slmnfll.dungeons.instance;

import org.bukkit.entity.Player;

public record DungeonPlayerState(int foodLevel, float saturation, float exhaustion) {
    public static DungeonPlayerState capture(Player player) {
        return new DungeonPlayerState(
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion()
        );
    }

    public void prepareForDungeon(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(0.0F);
        player.setExhaustion(0.0F);
    }

    public void restore(Player player) {
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setExhaustion(exhaustion);
    }
}
