package me.slmnfll.dungeons.equipment.armor;

import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum DungeonArmorSlot {
    HELMET("helmet", "Helmet", EquipmentSlotGroup.HEAD),
    CHESTPLATE("chestplate", "Chestplate", EquipmentSlotGroup.CHEST),
    LEGGINGS("leggings", "Leggings", EquipmentSlotGroup.LEGS),
    BOOTS("boots", "Boots", EquipmentSlotGroup.FEET);

    private final String id;
    private final String displayName;
    private final EquipmentSlotGroup equipmentSlotGroup;

    DungeonArmorSlot(String id, String displayName, EquipmentSlotGroup equipmentSlotGroup) {
        this.id = id;
        this.displayName = displayName;
        this.equipmentSlotGroup = equipmentSlotGroup;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public EquipmentSlotGroup equipmentSlotGroup() {
        return equipmentSlotGroup;
    }

    public static Optional<DungeonArmorSlot> fromId(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(slot -> slot.id.equals(normalized)).findFirst();
    }
}
