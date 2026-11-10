package me.slmnfll.dungeons.profile;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class DungeonProfileListener implements Listener {
    private final JavaPlugin plugin;
    private final DungeonProfileService profileService;

    public DungeonProfileListener(JavaPlugin plugin, DungeonProfileService profileService) {
        this.plugin = plugin;
        this.profileService = profileService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> profileService.load(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profileService.unload(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVanillaExperience(PlayerExpChangeEvent event) {
        event.setAmount(0);
    }
}
