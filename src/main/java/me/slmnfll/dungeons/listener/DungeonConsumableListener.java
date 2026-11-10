package me.slmnfll.dungeons.listener;

import me.slmnfll.dungeons.encounter.DungeonDamageContext;
import me.slmnfll.dungeons.equipment.consumable.DungeonConsumableDefinition;
import me.slmnfll.dungeons.equipment.consumable.DungeonConsumableService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public final class DungeonConsumableListener implements Listener {
    private final DungeonConsumableService consumableService;
    private final DungeonDamageContext damageContext;

    public DungeonConsumableListener(
            DungeonConsumableService consumableService,
            DungeonDamageContext damageContext
    ) {
        this.consumableService = Objects.requireNonNull(consumableService, "consumableService");
        this.damageContext = Objects.requireNonNull(damageContext, "damageContext");
    }

    @EventHandler
    public void onFoodUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.isCancelled() && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        ItemStack item = event.getItem();
        DungeonConsumableDefinition consumable = consumableService.read(item).orElse(null);
        if (consumable == null || !consumable.food()) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!beginCooldown(player, item, consumable)) {
            return;
        }
        consumeHeldItem(player, event.getHand(), item);
        applyRegeneration(player, consumable);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrink(PlayerItemConsumeEvent event) {
        DungeonConsumableDefinition consumable = consumableService.read(event.getItem()).orElse(null);
        if (consumable == null || consumable.throwable() || consumable.food()) {
            return;
        }
        Player player = event.getPlayer();
        if (!beginCooldown(player, event.getItem(), consumable)) {
            event.setCancelled(true);
            return;
        }
        switch (consumable.ability()) {
            case REGENERATION -> applyRegeneration(player, consumable);
            case HEALING -> applyHealing(player, consumable);
            case STONEHIDE -> applyStonehide(player, consumable);
            case CINDER_BURST -> {
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSplash(PotionSplashEvent event) {
        DungeonConsumableDefinition consumable = consumableService.read(event.getPotion().getItem()).orElse(null);
        if (consumable == null || !consumable.throwable()) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getPotion().getShooter() instanceof Player player)) {
            return;
        }
        if (!beginCooldown(player, event.getPotion().getItem(), consumable)) {
            return;
        }
        Location impact = event.getPotion().getLocation();
        damageContext.withoutWeaponModifiers(player, () -> {
            for (LivingEntity target : impact.getNearbyLivingEntities(consumable.radius())) {
                if (target instanceof Player || target instanceof ArmorStand || target.isDead()) {
                    continue;
                }
                target.damage(consumable.magnitude(), player);
                target.setFireTicks(Math.max(target.getFireTicks(), consumable.durationTicks()));
            }
        });
        impact.getWorld().spawnParticle(Particle.FLAME, impact, 42,
                consumable.radius() * 0.4D, 0.4D, consumable.radius() * 0.4D, 0.08D);
        impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.65F, 1.3F);
    }

    private boolean beginCooldown(
            Player player,
            ItemStack item,
            DungeonConsumableDefinition consumable
    ) {
        if (player.hasCooldown(item)) {
            return false;
        }
        player.setCooldown(item, consumable.cooldownTicks());
        return true;
    }

    private void consumeHeldItem(Player player, EquipmentSlot hand, ItemStack item) {
        item.subtract(1);
        player.getInventory().setItem(hand, item.getAmount() > 0 ? item : null);
    }

    private void applyRegeneration(Player player, DungeonConsumableDefinition consumable) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                consumable.durationTicks(),
                consumable.regenerationLevel() - 1,
                false,
                true,
                true
        ));
        player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                player.getLocation().add(0.0D, 1.0D, 0.0D),
                12,
                0.45D,
                0.55D,
                0.45D,
                0.04D
        );
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8F, 1.1F);
        player.sendActionBar(Component.text(
                consumable.displayName() + " consumed",
                NamedTextColor.LIGHT_PURPLE
        ));
    }

    private void applyHealing(Player player, DungeonConsumableDefinition consumable) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double maximum = maxHealth == null ? 20.0D : maxHealth.getValue();
        double healed = Math.min(maximum, player.getHealth() + consumable.magnitude());
        player.setHealth(healed);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0.0D, 1.0D, 0.0D),
                8, 0.45D, 0.55D, 0.45D, 0.04D);
        player.sendActionBar(Component.text("Healing Phial consumed", NamedTextColor.GREEN));
    }

    private void applyStonehide(Player player, DungeonConsumableDefinition consumable) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                consumable.durationTicks(),
                0,
                false,
                true,
                true
        ));
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0.0D, 1.0D, 0.0D),
                18, 0.5D, 0.7D, 0.5D, 0.08D);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, 0.8F, 0.7F);
        player.sendActionBar(Component.text("Stonehide active", NamedTextColor.GRAY));
    }
}
