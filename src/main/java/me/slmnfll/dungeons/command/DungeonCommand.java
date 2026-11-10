package me.slmnfll.dungeons.command;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.affinity.DungeonAffinityService;
import me.slmnfll.dungeons.encounter.EncounterRoomState;
import me.slmnfll.dungeons.content.DungeonItemDeliveryService;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorService;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorSlot;
import me.slmnfll.dungeons.equipment.artifact.DungeonArtifactService;
import me.slmnfll.dungeons.equipment.consumable.DungeonConsumableService;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneDefinition;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneService;
import me.slmnfll.dungeons.equipment.rune.smith.DungeonRuneSmithService;
import me.slmnfll.dungeons.equipment.weapon.DungeonWeaponService;
import me.slmnfll.dungeons.instance.DungeonInstance;
import me.slmnfll.dungeons.instance.DungeonInstanceManager;
import me.slmnfll.dungeons.instance.DungeonSession;
import me.slmnfll.dungeons.journal.DungeonJournalService;
import me.slmnfll.dungeons.profile.DungeonProfile;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import me.slmnfll.dungeons.story.prologue.PrologueService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class DungeonCommand implements CommandExecutor, TabCompleter {
    private final DungeonInstanceManager instanceManager;
    private final DungeonProfileService profileService;
    private final DungeonAffinityService affinityService;
    private final DungeonWeaponService weaponService;
    private final DungeonArmorService armorService;
    private final DungeonArtifactService artifactService;
    private final DungeonRuneService runeService;
    private final DungeonConsumableService consumableService;
    private final DungeonRuneSmithService runeSmithService;
    private final DungeonItemDeliveryService itemDeliveryService;
    private final DungeonJournalService journalService;
    private final PrologueService prologueService;

    public DungeonCommand(
            DungeonInstanceManager instanceManager,
            DungeonProfileService profileService,
            DungeonAffinityService affinityService,
            DungeonWeaponService weaponService,
            DungeonArmorService armorService,
            DungeonArtifactService artifactService,
            DungeonRuneService runeService,
            DungeonConsumableService consumableService,
            DungeonRuneSmithService runeSmithService,
            DungeonItemDeliveryService itemDeliveryService,
            DungeonJournalService journalService,
            PrologueService prologueService
    ) {
        this.instanceManager = instanceManager;
        this.profileService = profileService;
        this.affinityService = affinityService;
        this.weaponService = weaponService;
        this.armorService = armorService;
        this.artifactService = artifactService;
        this.runeService = runeService;
        this.consumableService = consumableService;
        this.runeSmithService = runeSmithService;
        this.itemDeliveryService = itemDeliveryService;
        this.journalService = journalService;
        this.prologueService = prologueService;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This development command can only be used by a player.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (!subcommand.equals("journal")
                && !subcommand.equals("prologue")
                && !player.hasPermission("dungeons.admin")) {
            player.sendMessage(Component.text("You do not have access to that command.", NamedTextColor.RED));
            return true;
        }

        try {
            switch (subcommand) {
                case "journal" -> journalService.handle(player, args);
                case "prologue" -> prologueService.handle(player, args);
                case "start" -> start(player, args);
                case "leave" -> {
                    instanceManager.leave(player);
                    player.sendMessage(Component.text("You left the dungeon safely.", NamedTextColor.YELLOW));
                }
                case "complete" -> {
                    instanceManager.complete(player);
                    player.sendMessage(Component.text("Dungeon completed. Your recovered loot is now secured.", NamedTextColor.GREEN));
                }
                case "status" -> status(player);
                case "profile" -> profile(player);
                case "affinity" -> affinity(player, args);
                case "weapon" -> weapon(player, args);
                case "armor" -> armor(player, args);
                case "armorset" -> armorSet(player, args);
                case "artifact" -> artifact(player, args);
                case "rune" -> rune(player, args);
                case "consumable" -> consumable(player, args);
                case "runesmith" -> runeSmithService.open(player);
                case "socket" -> socket(player);
                case "unsocket" -> unsocket(player, args);
                case "repair" -> repair(player);
                case "identify" -> identify(player);
                default -> sendUsage(player, label);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private void start(Player player, String[] args) {
        int seedArgument = 1;
        Affinity affinity;
        if (args.length >= 2 && Affinity.fromId(args[1]).isPresent()) {
            affinity = Affinity.fromId(args[1]).orElseThrow();
            affinityService.select(player, affinity);
            seedArgument = 2;
        } else {
            affinity = profileService.profile(player).selectedAffinity().orElseThrow(() ->
                    new IllegalStateException(
                            "Select an affinity first with /dungeons affinity <vanguard|arbalist|ravager>."
                    )
            );
        }
        if (args.length > seedArgument + 1) {
            throw new IllegalArgumentException("Use /dungeons start [affinity] [seed].");
        }
        long seed = args.length > seedArgument
                ? parseSeed(args[seedArgument])
                : ThreadLocalRandom.current().nextLong();

        player.sendMessage(Component.text("The dungeon is taking shape...", NamedTextColor.GRAY));
        DungeonSession session = instanceManager.start(player, affinity, seed);
        DungeonInstance instance = session.instance();
        player.sendMessage(Component.text(
                "Entered a " + instance.layout().rooms().size() + "-room expedition as "
                        + affinity.displayName() + ". Pots: " + session.content().totalPots()
                        + ", locked chests: " + session.content().totalChests()
                        + ", required fights: " + session.encounters().plan().requiredCombatRoomIds().size()
                        + ". Seed: " + seed,
                NamedTextColor.AQUA
        ));
    }

    private void status(Player player) {
        DungeonSession session = instanceManager.sessionFor(player)
                .orElseThrow(() -> new IllegalStateException("You do not have an active dungeon instance."));
        DungeonInstance instance = session.instance();
        long requiredTotal = session.encounters().plan().requiredCombatRoomIds().size();
        long requiredCleared = session.encounters().plan().requiredCombatRoomIds().stream()
                .map(roomId -> session.encounters().room(roomId).orElseThrow())
                .filter(room -> room.state() == EncounterRoomState.CLEARED)
                .count();
        player.sendMessage(Component.text(
                "State: " + instance.state()
                        + " | Affinity: " + instance.affinity().displayName()
                        + " | Rooms: " + instance.layout().rooms().size()
                        + " | Keys: " + session.progress().keys()
                        + " | Pots: " + session.progress().brokenPots() + "/" + session.progress().totalPots()
                        + " | Chests: " + session.progress().openedChests() + "/" + session.progress().totalChests()
                        + " | Dungeon Level: " + profileService.profile(player).level()
                        + " | Required fights: " + requiredCleared + "/" + requiredTotal
                        + " | Seed: " + instance.seed(),
                NamedTextColor.AQUA
        ));
    }

    private void profile(Player player) {
        DungeonProfile profile = profileService.profile(player);
        String selectedAffinity = profile.selectedAffinity()
                .map(Affinity::displayName)
                .orElse("Not selected");
        String claimedStarters = profile.claimedStarters().isEmpty()
                ? "None"
                : profile.claimedStarters().stream()
                        .map(Affinity::displayName)
                        .sorted()
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("None");
        player.sendMessage(Component.text(
                "Dungeon Level " + profile.level()
                        + " | Current level XP: " + profile.experienceIntoLevel()
                        + " | Affinity: " + selectedAffinity
                        + " | Claimed starters: " + claimedStarters,
                NamedTextColor.GREEN
        ));
    }

    private void affinity(Player player, String[] args) {
        if (args.length < 2) {
            DungeonProfile profile = profileService.profile(player);
            player.sendMessage(Component.text(
                    "Selected affinity: " + profile.selectedAffinity()
                            .map(Affinity::displayName)
                            .orElse("None"),
                    NamedTextColor.AQUA
            ));
            return;
        }
        Affinity affinity = Affinity.fromId(args[1]).orElseThrow(() -> new IllegalArgumentException(
                "Unknown affinity. Use vanguard, arbalist, or ravager."
        ));
        DungeonAffinityService.SelectionResult result = affinityService.select(player, affinity);
        player.sendMessage(Component.text(
                result.changed()
                        ? "Your affinity is now " + affinity.displayName() + "."
                        : "Your affinity is already " + affinity.displayName() + ".",
                result.changed() ? NamedTextColor.GREEN : NamedTextColor.YELLOW
        ));
    }

    private void weapon(Player player, String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Use /dungeons weapon <vanguard|arbalist|ravager> [seed].");
        }
        Affinity affinity = Affinity.fromId(args[1]).orElseThrow(() -> new IllegalArgumentException(
                "Unknown affinity. Use vanguard, arbalist, or ravager."
        ));
        long seed = args.length >= 3 ? parseSeed(args[2]) : ThreadLocalRandom.current().nextLong();
        itemDeliveryService.give(player, List.of(weaponService.createLootWeapon(affinity, seed)));
        player.sendMessage(Component.text(
                "Created a test " + affinity.displayName() + " weapon with roll seed " + seed + ".",
                NamedTextColor.LIGHT_PURPLE
        ));
    }

    private void armor(Player player, String[] args) {
        if (args.length < 3 || args.length > 4) {
            throw new IllegalArgumentException(
                    "Use /dungeons armor <vanguard|arbalist|ravager> <helmet|chestplate|leggings|boots> [seed]."
            );
        }
        Affinity affinity = parseAffinity(args[1]);
        DungeonArmorSlot slot = DungeonArmorSlot.fromId(args[2]).orElseThrow(() ->
                new IllegalArgumentException("Unknown armor slot. Use helmet, chestplate, leggings, or boots."));
        long seed = args.length == 4 ? parseSeed(args[3]) : ThreadLocalRandom.current().nextLong();
        itemDeliveryService.give(player, List.of(armorService.createLootArmor(affinity, slot, seed)));
        player.sendMessage(Component.text(
                "Created a test " + affinity.displayName() + " " + slot.displayName().toLowerCase(Locale.ROOT)
                        + " with roll seed " + seed + ".",
                NamedTextColor.LIGHT_PURPLE
        ));
    }

    private void armorSet(Player player, String[] args) {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("Use /dungeons armorset <vanguard|arbalist|ravager> [seed].");
        }
        Affinity affinity = parseAffinity(args[1]);
        long seed = args.length == 3 ? parseSeed(args[2]) : ThreadLocalRandom.current().nextLong();
        itemDeliveryService.give(player, armorService.createSet(affinity, seed));
        player.sendMessage(Component.text(
                "Created the provisional " + affinity.displayName() + " armor set with roll seed " + seed + ".",
                NamedTextColor.LIGHT_PURPLE
        ));
    }

    private Affinity parseAffinity(String value) {
        return Affinity.fromId(value).orElseThrow(() -> new IllegalArgumentException(
                "Unknown affinity. Use vanguard, arbalist, or ravager."
        ));
    }

    private void artifact(Player player, String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Use /dungeons artifact <vanguard|arbalist|ravager>.");
        }
        Affinity affinity = parseAffinity(args[1]);
        itemDeliveryService.give(player, List.of(artifactService.createLootArtifact(affinity)));
        player.sendMessage(Component.text(
                "Created the provisional " + affinity.displayName() + " artifact.",
                NamedTextColor.LIGHT_PURPLE
        ));
    }

    private void rune(Player player, String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Use /dungeons rune <force|ward|restoration|cinders|rift>.");
        }
        itemDeliveryService.give(player, List.of(runeService.create(args[1].toLowerCase(Locale.ROOT))));
        player.sendMessage(Component.text("Created a provisional dungeon rune.", NamedTextColor.LIGHT_PURPLE));
    }

    private void consumable(Player player, String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Use /dungeons consumable <mossbread|sunroot|deepdelve_stew|lumen_berries|healing|stonehide|cinder>."
            );
        }
        itemDeliveryService.give(player, List.of(
                consumableService.create(args[1].toLowerCase(Locale.ROOT))
        ));
        player.sendMessage(Component.text("Created a dungeon consumable.", NamedTextColor.LIGHT_PURPLE));
    }

    private void socket(Player player) {
        DungeonRuneDefinition rune = runeService.socket(player);
        player.sendMessage(Component.text(
                rune.displayName() + " was socketed into your held equipment.",
                NamedTextColor.GREEN
        ));
    }

    private void unsocket(Player player, String[] args) {
        if (args.length > 2) {
            throw new IllegalArgumentException("Use /dungeons unsocket [slot].");
        }
        int slot = args.length == 2 ? parsePositiveInt(args[1], "Rune slot") : 1;
        DungeonRuneDefinition rune = runeService.unsocket(player, slot);
        player.sendMessage(Component.text(
                rune.displayName() + " was removed and returned to you.",
                NamedTextColor.GREEN
        ));
    }

    private void repair(Player player) {
        runeService.repair(player);
        player.sendMessage(Component.text(
                "Your held dungeon equipment was fully repaired.",
                NamedTextColor.GREEN
        ));
    }

    private void identify(Player player) {
        if (!weaponService.revealAbility(player.getInventory().getItemInMainHand())
                && !artifactService.revealAbility(player.getInventory().getItemInMainHand())) {
            throw new IllegalStateException("Hold an unidentified dungeon item with an ability.");
        }
        player.sendMessage(Component.text("The item's hidden ability has been identified.", NamedTextColor.LIGHT_PURPLE));
    }

    private long parseSeed(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("The seed must be a whole number.");
        }
    }

    private int parsePositiveInt(String value, String label) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a positive whole number.");
        }
    }

    private void sendUsage(Player player, String label) {
        player.sendMessage(Component.text("/" + label + " journal", NamedTextColor.DARK_PURPLE));
        if (!player.hasPermission("dungeons.admin")) {
            return;
        }
        player.sendMessage(Component.text("Dungeon development commands:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/" + label + " affinity [vanguard|arbalist|ravager]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " start [affinity] [seed]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " status", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " profile", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " weapon <affinity> [seed]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " armor <affinity> <slot> [seed]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " armorset <affinity> [seed]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " artifact <affinity>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text(
                "/" + label + " rune <force|ward|restoration|cinders|rift>",
                NamedTextColor.YELLOW
        ));
        player.sendMessage(Component.text(
                "/" + label + " consumable <mossbread|sunroot|deepdelve_stew|lumen_berries|healing|stonehide|cinder>",
                NamedTextColor.YELLOW
        ));
        player.sendMessage(Component.text("/" + label + " runesmith", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " socket", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " unsocket [slot]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " repair", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " identify", NamedTextColor.YELLOW));
        player.sendMessage(Component.text(
                "/" + label + " prologue <place|reset|skin <MinecraftName>>",
                NamedTextColor.YELLOW
        ));
        player.sendMessage(Component.text("/" + label + " complete", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " leave", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            if (!sender.hasPermission("dungeons.admin")) {
                return filter(List.of("journal"), args[0]);
            }
            return filter(List.of(
                    "journal",
                    "affinity", "start", "status", "profile", "weapon", "armor", "armorset", "artifact",
                    "rune", "consumable", "runesmith", "socket", "unsocket", "repair", "identify", "prologue",
                    "complete", "leave"
            ), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("prologue")
                && sender.hasPermission("dungeons.admin")) {
            return filter(List.of("place", "reset", "skin"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return filter(Arrays.stream(Affinity.values()).map(Affinity::id).toList(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("weapon")) {
            return filter(Arrays.stream(Affinity.values()).map(Affinity::id).toList(), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("armor")
                || args[0].equalsIgnoreCase("armorset")
                || args[0].equalsIgnoreCase("artifact"))) {
            return filter(Arrays.stream(Affinity.values()).map(Affinity::id).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("armor")) {
            return filter(Arrays.stream(DungeonArmorSlot.values()).map(DungeonArmorSlot::id).toList(), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("affinity")) {
            return filter(Arrays.stream(Affinity.values()).map(Affinity::id).toList(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("rune")) {
            return filter(List.of("force", "ward", "restoration", "cinders", "rift"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("consumable")) {
            return filter(List.of(
                    "mossbread", "sunroot", "deepdelve_stew", "lumen_berries",
                    "healing", "stonehide", "cinder"
            ), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("unsocket")) {
            return filter(List.of("1", "2", "3"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> choices, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return choices.stream().filter(choice -> choice.startsWith(normalized)).toList();
    }
}
