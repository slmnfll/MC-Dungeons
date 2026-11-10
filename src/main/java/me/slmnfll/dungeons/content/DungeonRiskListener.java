package me.slmnfll.dungeons.content;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class DungeonRiskListener implements Listener {
    private final DungeonRiskService riskService;

    public DungeonRiskListener(DungeonRiskService riskService) {
        this.riskService = riskService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        int discarded = riskService.discardAllAtRisk(event.getPlayer());
        if (discarded > 0) {
            event.getPlayer().sendMessage(Component.text(
                    "Unsecured loot from an interrupted expedition was lost.",
                    NamedTextColor.RED
            ));
        }
    }
}
