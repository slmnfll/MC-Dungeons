package me.slmnfll.dungeons.tutorial;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.content.DungeonItemDeliveryService;
import me.slmnfll.dungeons.content.ItemRarity;
import me.slmnfll.dungeons.equipment.DungeonEquipmentService;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorService;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorSlot;
import me.slmnfll.dungeons.equipment.artifact.DungeonArtifactService;
import me.slmnfll.dungeons.equipment.consumable.DungeonConsumableService;
import me.slmnfll.dungeons.equipment.weapon.DungeonWeaponService;
import me.slmnfll.dungeons.instance.DungeonRunStartedEvent;
import me.slmnfll.dungeons.instance.DungeonRunType;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TutorialLoadoutService implements Listener {
    private static final long ARMOR_SEED = SurveySiteZeroDefinition.SEED ^ 0xA660L;

    private final DungeonArmorService armorService;
    private final DungeonWeaponService weaponService;
    private final DungeonArtifactService artifactService;
    private final DungeonConsumableService consumableService;
    private final DungeonItemDeliveryService itemDeliveryService;
    private final DungeonProfileService profileService;
    private final NamespacedKey guildStarterKey;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey rarityKey;

    public TutorialLoadoutService(
            JavaPlugin plugin,
            DungeonArmorService armorService,
            DungeonWeaponService weaponService,
            DungeonArtifactService artifactService,
            DungeonConsumableService consumableService,
            DungeonItemDeliveryService itemDeliveryService,
            DungeonProfileService profileService
    ) {
        this.armorService = Objects.requireNonNull(armorService, "armorService");
        this.weaponService = Objects.requireNonNull(weaponService, "weaponService");
        this.artifactService = Objects.requireNonNull(artifactService, "artifactService");
        this.consumableService = Objects.requireNonNull(consumableService, "consumableService");
        this.itemDeliveryService = Objects.requireNonNull(itemDeliveryService, "itemDeliveryService");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.guildStarterKey = new NamespacedKey(plugin, DungeonEquipmentService.GUILD_STARTER_KEY);
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.rarityKey = new NamespacedKey(plugin, "rarity");
    }

    @EventHandler
    public void onRunStarted(DungeonRunStartedEvent event) {
        if (event.session().instance().runType() != DungeonRunType.TUTORIAL) {
            return;
        }
        Player player = event.player();
        Affinity affinity = event.session().instance().affinity();
        equipMissingArmor(player, affinity);

        List<ItemStack> supplies = new ArrayList<>();
        if (!hasStarterWeapon(player, affinity)) {
            supplies.add(tagStarter(weaponService.createStarter(affinity)));
        }
        if (!hasStarterArtifact(player, affinity)) {
            supplies.add(tagStarter(artifactService.createStarterArtifact(affinity)));
        }
        ensureAmount(player, supplies, consumableService.create("mossbread"), 3);
        ensureAmount(player, supplies, consumableService.create("lumen_berries"), 2);
        if (affinity == Affinity.ARBALIST) {
            ensureAmount(player, supplies, createArrows(), 32);
        }
        itemDeliveryService.give(player, supplies);
        profileService.claimStarter(player, affinity);
        player.sendMessage(Component.text(
                "The guild issued your permanent " + affinity.displayName()
                        + " armor, weapon, artifact, and recovery supplies.",
                NamedTextColor.GREEN
        ));
    }

    private void equipMissingArmor(Player player, Affinity affinity) {
        List<ItemStack> armor = armorService.createSet(affinity, ARMOR_SEED);
        DungeonArmorSlot[] slots = DungeonArmorSlot.values();
        for (int index = 0; index < slots.length; index++) {
            DungeonArmorSlot slot = slots[index];
            if (!isEmptyEquipmentSlot(equipped(player.getInventory(), slot))) {
                continue;
            }
            equip(player.getInventory(), slot, tagStarter(armor.get(index)));
        }
    }

    static boolean isEmptyEquipmentSlot(ItemStack item) {
        return item == null || isEmptyEquipmentMaterial(item.getType());
    }

    static boolean isEmptyEquipmentMaterial(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    private ItemStack createArrows() {
        ItemStack arrows = new ItemStack(Material.ARROW, 32);
        ItemMeta meta = arrows.getItemMeta();
        meta.displayName(Component.text("Survey Bolts", NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, "tutorial_arbalist_arrows");
        meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, ItemRarity.COMMON.name());
        arrows.setItemMeta(meta);
        return arrows;
    }

    private ItemStack tagStarter(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(Component.empty());
        lore.add(Component.text("Guild starter item", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Protected if Survey Site Zero fails", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(guildStarterKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void ensureAmount(Player player, List<ItemStack> supplies, ItemStack template, int desiredAmount) {
        String itemId = template.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        int currentAmount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (!isStarterItem(item, itemId)) {
                continue;
            }
            currentAmount += item.getAmount();
        }
        if (currentAmount < desiredAmount) {
            template.setAmount(desiredAmount - currentAmount);
            supplies.add(tagStarter(template));
        }
    }

    private boolean hasStarterWeapon(Player player, Affinity affinity) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isGuildStarter(item) && weaponService.isStarter(item, affinity)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStarterArtifact(Player player, Affinity affinity) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isGuildStarter(item)
                    && artifactService.read(item)
                    .filter(artifact -> artifact.definition().affinity() == affinity)
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean isStarterItem(ItemStack item, String expectedItemId) {
        if (!isGuildStarter(item) || expectedItemId == null) {
            return false;
        }
        String itemId = item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
        return expectedItemId.equals(itemId);
    }

    private boolean isGuildStarter(ItemStack item) {
        return item != null
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                .has(guildStarterKey, PersistentDataType.BYTE);
    }

    private ItemStack equipped(PlayerInventory inventory, DungeonArmorSlot slot) {
        return switch (slot) {
            case HELMET -> inventory.getHelmet();
            case CHESTPLATE -> inventory.getChestplate();
            case LEGGINGS -> inventory.getLeggings();
            case BOOTS -> inventory.getBoots();
        };
    }

    private void equip(PlayerInventory inventory, DungeonArmorSlot slot, ItemStack item) {
        switch (slot) {
            case HELMET -> inventory.setHelmet(item);
            case CHESTPLATE -> inventory.setChestplate(item);
            case LEGGINGS -> inventory.setLeggings(item);
            case BOOTS -> inventory.setBoots(item);
        }
    }
}
