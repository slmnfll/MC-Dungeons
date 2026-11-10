package me.slmnfll.dungeons.equipment.rune.smith;

import java.util.List;

public final class DungeonRuneSmithLayout {
    public static final int SIZE = 54;
    public static final int INFO_SLOT = 4;
    public static final int EQUIPMENT_LABEL_SLOT = 11;
    public static final int RUNE_LABEL_SLOT = 15;
    public static final int EQUIPMENT_INPUT_SLOT = 20;
    public static final int RUNE_INPUT_SLOT = 24;
    public static final int SOCKET_ACTION_SLOT = 30;
    public static final int REPAIR_ACTION_SLOT = 32;
    public static final List<Integer> SOCKET_DISPLAY_SLOTS = List.of(38, 40, 42);
    public static final int CLOSE_SLOT = 49;

    private DungeonRuneSmithLayout() {
    }

    public static boolean isInputSlot(int slot) {
        return slot == EQUIPMENT_INPUT_SLOT || slot == RUNE_INPUT_SLOT;
    }

    public static int oneBasedRuneSlot(int inventorySlot) {
        int index = SOCKET_DISPLAY_SLOTS.indexOf(inventorySlot);
        return index < 0 ? 0 : index + 1;
    }
}
