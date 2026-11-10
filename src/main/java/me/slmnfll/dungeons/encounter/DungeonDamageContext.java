package me.slmnfll.dungeons.encounter;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DungeonDamageContext {
    private final Set<UUID> weaponModifiersSuppressed = new HashSet<>();

    public boolean suppressesWeaponModifiers(Player player) {
        return weaponModifiersSuppressed.contains(player.getUniqueId());
    }

    public void withoutWeaponModifiers(Player player, Runnable damageAction) {
        UUID playerId = player.getUniqueId();
        weaponModifiersSuppressed.add(playerId);
        try {
            damageAction.run();
        } finally {
            weaponModifiersSuppressed.remove(playerId);
        }
    }
}
