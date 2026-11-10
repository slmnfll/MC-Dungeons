package me.slmnfll.dungeons.equipment.rune.ability;

import me.slmnfll.dungeons.encounter.DungeonDamageContext;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneEffect;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DungeonRuneAbilityListener implements Listener {
    private final DungeonRuneService runeService;
    private final DungeonDamageContext damageContext;
    private final NamespacedKey projectileCinderRunesKey;
    private final Map<UUID, Integer> cinderHitCounts = new HashMap<>();

    public DungeonRuneAbilityListener(
            JavaPlugin plugin,
            DungeonRuneService runeService,
            DungeonDamageContext damageContext
    ) {
        this.runeService = Objects.requireNonNull(runeService, "runeService");
        this.damageContext = Objects.requireNonNull(damageContext, "damageContext");
        this.projectileCinderRunesKey = new NamespacedKey(plugin, "projectile_cinder_runes");
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)
                || !(event.getProjectile() instanceof Projectile projectile)
                || event.getBow() == null) {
            return;
        }
        int runeCount = cinderRuneCount(event.getBow());
        if (runeCount > 0) {
            projectile.getPersistentDataContainer().set(
                    projectileCinderRunesKey,
                    PersistentDataType.INTEGER,
                    runeCount
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null
                || damageContext.suppressesWeaponModifiers(attacker)
                || !(event.getEntity() instanceof LivingEntity target)
                || target instanceof Player
                || target instanceof ArmorStand) {
            return;
        }

        int runeCount = event.getDamager() instanceof Projectile projectile
                ? projectile.getPersistentDataContainer().getOrDefault(
                        projectileCinderRunesKey,
                        PersistentDataType.INTEGER,
                        0
                )
                : cinderRuneCount(attacker.getInventory().getItemInMainHand());
        if (runeCount <= 0) {
            return;
        }

        int hitCount = cinderHitCounts.merge(attacker.getUniqueId(), 1, Integer::sum);
        if (hitCount < DungeonRuneAbilityScaling.CINDER_HITS_REQUIRED) {
            attacker.sendActionBar(Component.text(
                    "Cinder Burst " + hitCount + "/" + DungeonRuneAbilityScaling.CINDER_HITS_REQUIRED,
                    NamedTextColor.GOLD
            ));
            return;
        }
        cinderHitCounts.put(attacker.getUniqueId(), 0);
        triggerCinderBurst(attacker, target, event.getDamage(), runeCount);
    }

    @EventHandler
    public void onWeaponUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (event.getHand() != EquipmentSlot.HAND
                || (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
                || !event.getPlayer().isSneaking()
                || (event.isCancelled() && action != Action.RIGHT_CLICK_AIR)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack weapon = player.getInventory().getItemInMainHand();
        int runeCount = runeCount(weapon, DungeonRuneEffect.RIFT);
        if (runeCount <= 0) {
            return;
        }
        event.setCancelled(true);
        if (player.hasCooldown(weapon)) {
            return;
        }

        player.setCooldown(weapon, DungeonRuneAbilityScaling.RIFT_COOLDOWN_TICKS);
        double radius = DungeonRuneAbilityScaling.riftRadius(runeCount);
        double pullStrength = DungeonRuneAbilityScaling.riftPullStrength(runeCount);
        int slowTicks = DungeonRuneAbilityScaling.riftSlowTicks(runeCount);
        int affected = 0;
        for (LivingEntity target : player.getLocation().getNearbyLivingEntities(radius)) {
            if (target instanceof Player || target instanceof ArmorStand || target.isDead()) {
                continue;
            }
            Vector pull = player.getLocation().toVector().subtract(target.getLocation().toVector());
            if (pull.lengthSquared() < 0.01D) {
                continue;
            }
            pull.normalize().multiply(pullStrength);
            pull.setY(Math.max(0.22D, pull.getY() + 0.18D));
            target.setVelocity(pull);
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    slowTicks,
                    1,
                    false,
                    true,
                    true
            ));
            affected++;
        }

        player.getWorld().spawnParticle(
                Particle.REVERSE_PORTAL,
                player.getLocation().add(0.0D, 1.0D, 0.0D),
                72,
                radius * 0.55D,
                0.8D,
                radius * 0.55D,
                0.08D
        );
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.65F);
        player.sendActionBar(Component.text(
                "Rift Pull affected " + affected + (affected == 1 ? " enemy" : " enemies"),
                NamedTextColor.DARK_PURPLE
        ));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cinderHitCounts.remove(event.getPlayer().getUniqueId());
    }

    private void triggerCinderBurst(
            Player attacker,
            LivingEntity primaryTarget,
            double triggeringDamage,
            int runeCount
    ) {
        double damage = triggeringDamage * DungeonRuneAbilityScaling.cinderDamageMultiplier(runeCount);
        int fireTicks = DungeonRuneAbilityScaling.cinderFireTicks(runeCount);
        primaryTarget.setFireTicks(Math.max(primaryTarget.getFireTicks(), fireTicks));

        damageContext.withoutWeaponModifiers(attacker, () -> {
            for (Entity nearby : primaryTarget.getNearbyEntities(
                    DungeonRuneAbilityScaling.CINDER_RADIUS,
                    DungeonRuneAbilityScaling.CINDER_RADIUS * 0.65D,
                    DungeonRuneAbilityScaling.CINDER_RADIUS
            )) {
                if (!(nearby instanceof LivingEntity living)
                        || living instanceof Player
                        || living instanceof ArmorStand
                        || living.isDead()
                        || living.getUniqueId().equals(primaryTarget.getUniqueId())) {
                    continue;
                }
                living.damage(damage, attacker);
                living.setFireTicks(Math.max(living.getFireTicks(), fireTicks));
            }
        });

        primaryTarget.getWorld().spawnParticle(
                Particle.FLAME,
                primaryTarget.getLocation().add(0.0D, 0.8D, 0.0D),
                42,
                1.25D,
                0.65D,
                1.25D,
                0.08D
        );
        primaryTarget.getWorld().spawnParticle(
                Particle.LAVA,
                primaryTarget.getLocation().add(0.0D, 0.5D, 0.0D),
                12,
                0.9D,
                0.35D,
                0.9D,
                0.0D
        );
        primaryTarget.getWorld().playSound(
                primaryTarget.getLocation(),
                Sound.ITEM_FIRECHARGE_USE,
                1.0F,
                0.8F
        );
        attacker.sendActionBar(Component.text("Cinder Burst!", NamedTextColor.GOLD));
    }

    private int cinderRuneCount(ItemStack equipment) {
        return runeCount(equipment, DungeonRuneEffect.CINDERS);
    }

    private int runeCount(ItemStack equipment, DungeonRuneEffect effect) {
        return (int) runeService.socketedRunes(equipment).stream()
                .filter(rune -> rune.effect() == effect)
                .count();
    }

    private Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
