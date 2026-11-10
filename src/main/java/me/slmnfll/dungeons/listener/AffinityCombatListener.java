package me.slmnfll.dungeons.listener;

import me.slmnfll.dungeons.affinity.DungeonPowerScaling;
import me.slmnfll.dungeons.encounter.DungeonDamageContext;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorService;
import me.slmnfll.dungeons.equipment.weapon.DungeonWeaponData;
import me.slmnfll.dungeons.equipment.weapon.DungeonWeaponService;
import me.slmnfll.dungeons.equipment.weapon.WeaponAbility;
import me.slmnfll.dungeons.profile.DungeonProfile;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class AffinityCombatListener implements Listener {
    private static final double MARK_DAMAGE_BONUS = 0.25D;
    private static final long MARK_DURATION_MILLIS = 5_000L;
    private static final int SHOCKWAVE_COOLDOWN_TICKS = 100;

    private final DungeonProfileService profileService;
    private final DungeonWeaponService weaponService;
    private final DungeonArmorService armorService;
    private final DungeonDamageContext damageContext;
    private final DungeonPowerScaling powerScaling = new DungeonPowerScaling();
    private final NamespacedKey projectileDamageKey;
    private final NamespacedKey projectileAbilityKey;
    private final Map<UUID, Integer> cleaveHits = new HashMap<>();
    private final Map<UUID, Mark> marks = new HashMap<>();

    public AffinityCombatListener(
            JavaPlugin plugin,
            DungeonProfileService profileService,
            DungeonWeaponService weaponService,
            DungeonArmorService armorService,
            DungeonDamageContext damageContext
    ) {
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.weaponService = Objects.requireNonNull(weaponService, "weaponService");
        this.armorService = Objects.requireNonNull(armorService, "armorService");
        this.damageContext = Objects.requireNonNull(damageContext, "damageContext");
        this.projectileDamageKey = new NamespacedKey(plugin, "custom_projectile_damage");
        this.projectileAbilityKey = new NamespacedKey(plugin, "custom_projectile_ability");
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getProjectile() instanceof Projectile projectile)
                || event.getBow() == null) {
            return;
        }
        DungeonWeaponData weapon = weaponService.read(event.getBow()).orElse(null);
        if (weapon == null) {
            return;
        }

        PersistentDataContainer data = projectile.getPersistentDataContainer();
        data.set(
                projectileDamageKey,
                PersistentDataType.DOUBLE,
                weaponService.effectiveDamage(event.getBow(), weapon)
                        * dungeonCombatMultiplier(player, weapon, true)
        );
        data.set(projectileAbilityKey, PersistentDataType.STRING, weapon.definition().ability().id());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null || damageContext.suppressesWeaponModifiers(attacker)) {
            return;
        }

        DungeonWeaponData weapon = directWeapon(event, attacker);
        WeaponAbility projectileAbility = projectileAbility(event.getDamager());
        if (event.getDamager() instanceof Projectile projectile) {
            Double projectileDamage = projectile.getPersistentDataContainer()
                    .get(projectileDamageKey, PersistentDataType.DOUBLE);
            if (projectileDamage != null) {
                event.setDamage(projectileDamage);
            }
        } else if (weapon != null) {
            event.setDamage(event.getDamage() * dungeonCombatMultiplier(attacker, weapon, false));
        }

        applyExistingMark(event, attacker);
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (projectileAbility == WeaponAbility.HUNTERS_MARK) {
            marks.put(target.getUniqueId(), new Mark(attacker.getUniqueId(), System.currentTimeMillis() + MARK_DURATION_MILLIS));
            target.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 12, 0.28D, 0.35D, 0.28D, 0.05D);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7F, 1.35F);
            return;
        }

        if (weapon == null || !(event.getDamager() instanceof Player)) {
            return;
        }
        switch (weapon.definition().ability()) {
            case CLEAVE -> tryCleave(attacker, target, event.getDamage());
            case SHOCKWAVE -> tryShockwave(attacker, target, event.getDamage(), attacker.getInventory().getItemInMainHand());
            default -> {
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        marks.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cleaveHits.remove(playerId);
        marks.entrySet().removeIf(entry -> entry.getKey().equals(playerId) || entry.getValue().ownerId().equals(playerId));
    }

    private DungeonWeaponData directWeapon(EntityDamageByEntityEvent event, Player attacker) {
        if (!(event.getDamager() instanceof Player)) {
            return null;
        }
        return weaponService.read(attacker.getInventory().getItemInMainHand()).orElse(null);
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

    private WeaponAbility projectileAbility(Entity damager) {
        if (!(damager instanceof Projectile projectile)) {
            return WeaponAbility.NONE;
        }
        String abilityId = projectile.getPersistentDataContainer()
                .get(projectileAbilityKey, PersistentDataType.STRING);
        return WeaponAbility.fromId(abilityId).orElse(WeaponAbility.NONE);
    }

    private double dungeonCombatMultiplier(Player player, DungeonWeaponData weapon, boolean projectile) {
        DungeonProfile profile = profileService.profile(player);
        double weaponMultiplier = powerScaling.weaponDamageMultiplier(
                profileService.effectiveLevel(player),
                profile.selectedAffinity().orElse(null),
                weapon.definition().affinity()
        );
        return weaponMultiplier * armorService.summary(player).outgoingDamageMultiplier(projectile);
    }

    private void applyExistingMark(EntityDamageByEntityEvent event, Player attacker) {
        Mark mark = marks.get(event.getEntity().getUniqueId());
        if (mark == null) {
            return;
        }
        if (mark.expiresAtMillis() < System.currentTimeMillis()) {
            marks.remove(event.getEntity().getUniqueId());
            return;
        }
        if (!mark.ownerId().equals(attacker.getUniqueId())) {
            return;
        }
        marks.remove(event.getEntity().getUniqueId());
        event.setDamage(event.getDamage() * (1.0D + MARK_DAMAGE_BONUS));
        event.getEntity().getWorld().spawnParticle(
                Particle.ENCHANTED_HIT,
                event.getEntity().getLocation().add(0.0D, 1.0D, 0.0D),
                15,
                0.3D,
                0.4D,
                0.3D,
                0.1D
        );
    }

    private void tryCleave(Player player, LivingEntity primaryTarget, double primaryDamage) {
        int hits = cleaveHits.merge(player.getUniqueId(), 1, Integer::sum);
        if (hits < 3) {
            return;
        }
        cleaveHits.put(player.getUniqueId(), 0);
        primaryTarget.getWorld().spawnParticle(
                Particle.SWEEP_ATTACK,
                primaryTarget.getLocation().add(0.0D, 1.0D, 0.0D),
                4,
                0.7D,
                0.35D,
                0.7D,
                0.0D
        );
        primaryTarget.getWorld().playSound(primaryTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.85F);
        damageNearby(player, primaryTarget, 3.25D, primaryDamage * 0.45D);
    }

    private void tryShockwave(Player player, LivingEntity primaryTarget, double primaryDamage, ItemStack weapon) {
        if (player.getAttackCooldown() < 0.90F || player.hasCooldown(weapon)) {
            return;
        }
        player.setCooldown(weapon, SHOCKWAVE_COOLDOWN_TICKS);
        primaryTarget.getWorld().spawnParticle(
                Particle.CLOUD,
                primaryTarget.getLocation().add(0.0D, 0.2D, 0.0D),
                28,
                1.4D,
                0.25D,
                1.4D,
                0.08D
        );
        primaryTarget.getWorld().playSound(primaryTarget.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.65F, 1.25F);
        damageNearby(player, primaryTarget, 4.0D, primaryDamage * 0.60D);
    }

    private void damageNearby(Player player, LivingEntity primaryTarget, double radius, double damage) {
        damageContext.withoutWeaponModifiers(player, () -> {
            for (Entity nearby : primaryTarget.getNearbyEntities(radius, radius * 0.6D, radius)) {
                if (!(nearby instanceof LivingEntity living)
                        || living instanceof Player
                        || living.isDead()
                        || living.getUniqueId().equals(primaryTarget.getUniqueId())) {
                    continue;
                }
                living.damage(damage, player);
            }
        });
    }

    private record Mark(UUID ownerId, long expiresAtMillis) {
    }
}
