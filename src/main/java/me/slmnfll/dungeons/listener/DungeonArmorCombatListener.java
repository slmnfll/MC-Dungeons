package me.slmnfll.dungeons.listener;

import me.slmnfll.dungeons.equipment.armor.DungeonArmorService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Objects;

public final class DungeonArmorCombatListener implements Listener {
    private final DungeonArmorService armorService;

    public DungeonArmorCombatListener(DungeonArmorService armorService) {
        this.armorService = Objects.requireNonNull(armorService, "armorService");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        event.setDamage(event.getDamage() * armorService.summary(player).incomingDamageMultiplier());
    }
}
