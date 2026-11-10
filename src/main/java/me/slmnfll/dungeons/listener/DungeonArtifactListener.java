package me.slmnfll.dungeons.listener;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.encounter.DungeonDamageContext;
import me.slmnfll.dungeons.equipment.artifact.ArtifactAbility;
import me.slmnfll.dungeons.equipment.artifact.DungeonArtifactData;
import me.slmnfll.dungeons.equipment.artifact.DungeonArtifactDefinition;
import me.slmnfll.dungeons.equipment.artifact.DungeonArtifactScaling;
import me.slmnfll.dungeons.equipment.artifact.DungeonArtifactService;
import me.slmnfll.dungeons.profile.DungeonProfile;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Objects;

public final class DungeonArtifactListener implements Listener {
    private final DungeonArtifactService artifactService;
    private final DungeonProfileService profileService;
    private final DungeonDamageContext damageContext;
    private final DungeonArtifactScaling scaling = new DungeonArtifactScaling();

    public DungeonArtifactListener(
            DungeonArtifactService artifactService,
            DungeonProfileService profileService,
            DungeonDamageContext damageContext
    ) {
        this.artifactService = Objects.requireNonNull(artifactService, "artifactService");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.damageContext = Objects.requireNonNull(damageContext, "damageContext");
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.isCancelled() && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        ItemStack item = event.getItem();
        DungeonArtifactData artifact = artifactService.read(item).orElse(null);
        if (artifact == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.hasCooldown(item)) {
            return;
        }
        player.setCooldown(item, artifact.definition().cooldownTicks());

        DungeonProfile profile = profileService.profile(player);
        Affinity selectedAffinity = profile.selectedAffinity().orElse(null);
        switch (artifact.definition().ability()) {
            case GUARDIAN_SHELL -> activateGuardianShell(player, artifact.definition(), selectedAffinity);
            case GALE_STEP -> activateGaleStep(player, artifact.definition(), selectedAffinity);
            case EMBER_BURST -> activateEmberBurst(player, artifact.definition(), selectedAffinity);
        }
    }

    private void activateGuardianShell(
            Player player,
            DungeonArtifactDefinition definition,
            Affinity selectedAffinity
    ) {
        int duration = scaling.durationTicks(
                definition.durationTicks(),
                selectedAffinity,
                definition.affinity()
        );
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, 1, false, true, true));
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0D, 1.0D, 0.0D),
                30, 0.55D, 0.8D, 0.55D, 0.12D);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.75F, 1.2F);
        activated(player, definition.ability());
    }

    private void activateGaleStep(
            Player player,
            DungeonArtifactDefinition definition,
            Affinity selectedAffinity
    ) {
        double force = scaling.potency(definition.power(), selectedAffinity, definition.affinity());
        Vector velocity = player.getLocation().getDirection().normalize().multiply(force);
        velocity.setY(Math.max(0.28D, velocity.getY() + 0.18D));
        player.setVelocity(velocity);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                scaling.durationTicks(definition.durationTicks(), selectedAffinity, definition.affinity()),
                0,
                false,
                false,
                true
        ));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0.0D, 0.25D, 0.0D),
                24, 0.45D, 0.18D, 0.45D, 0.08D);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.8F, 1.25F);
        activated(player, definition.ability());
    }

    private void activateEmberBurst(
            Player player,
            DungeonArtifactDefinition definition,
            Affinity selectedAffinity
    ) {
        double damage = scaling.damage(
                definition.power(),
                profileService.effectiveLevel(player),
                selectedAffinity,
                definition.affinity()
        );
        int fireTicks = scaling.durationTicks(
                definition.durationTicks(),
                selectedAffinity,
                definition.affinity()
        );
        damageContext.withoutWeaponModifiers(player, () -> {
            for (LivingEntity target : player.getLocation().getNearbyLivingEntities(definition.radius())) {
                if (target.getUniqueId().equals(player.getUniqueId())
                        || target instanceof Player
                        || target instanceof ArmorStand
                        || target.isDead()) {
                    continue;
                }
                target.damage(damage, player);
                target.setFireTicks(Math.max(target.getFireTicks(), fireTicks));
            }
        });
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0.0D, 0.35D, 0.0D),
                55, definition.radius() * 0.45D, 0.35D, definition.radius() * 0.45D, 0.08D);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0F, 0.75F);
        activated(player, definition.ability());
    }

    private void activated(Player player, ArtifactAbility ability) {
        player.sendActionBar(Component.text(ability.displayName() + " activated", NamedTextColor.LIGHT_PURPLE));
    }
}
