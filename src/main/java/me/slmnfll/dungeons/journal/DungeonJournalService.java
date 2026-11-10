package me.slmnfll.dungeons.journal;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.content.DungeonItemDeliveryService;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorService;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorSummary;
import me.slmnfll.dungeons.instance.DungeonInstance;
import me.slmnfll.dungeons.instance.DungeonInstanceManager;
import me.slmnfll.dungeons.instance.DungeonSession;
import me.slmnfll.dungeons.profile.DungeonProfile;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import me.slmnfll.dungeons.profile.DungeonProgressionBenefits;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public final class DungeonJournalService implements Listener {
    private static final String JOURNAL_ITEM_ID = "adventure_journal";
    private static final String ADAPTIVE_DUNGEON_ID = "adaptive";
    private static final int ITEM_SCHEMA_VERSION = 1;
    private static final long ENTRY_CONFIRMATION_MILLIS = 3_000L;

    private final JavaPlugin plugin;
    private final DungeonInstanceManager instanceManager;
    private final DungeonProfileService profileService;
    private final DungeonArmorService armorService;
    private final DungeonItemDeliveryService itemDeliveryService;
    private final DungeonProgressionBenefits progressionBenefits = new DungeonProgressionBenefits();
    private final DungeonJournalPagePolicy pagePolicy = new DungeonJournalPagePolicy();
    private final DungeonEntryConfirmation entryConfirmation =
            new DungeonEntryConfirmation(ENTRY_CONFIRMATION_MILLIS);
    private final NamespacedKey itemIdKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey schemaKey;
    private Predicate<Player> accessPolicy = ignored -> false;

    public DungeonJournalService(
            JavaPlugin plugin,
            DungeonInstanceManager instanceManager,
            DungeonProfileService profileService,
            DungeonArmorService armorService,
            DungeonItemDeliveryService itemDeliveryService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.instanceManager = Objects.requireNonNull(instanceManager, "instanceManager");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.armorService = Objects.requireNonNull(armorService, "armorService");
        this.itemDeliveryService = Objects.requireNonNull(itemDeliveryService, "itemDeliveryService");
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.categoryKey = new NamespacedKey(plugin, "item_category");
        this.schemaKey = new NamespacedKey(plugin, "item_schema");
    }

    public void handle(Player player, String[] args) {
        if (!accessPolicy.test(player)) {
            throw new IllegalStateException(
                    "The Guild Chronicler gives you an Adventure Journal after Survey Site Zero."
            );
        }
        if (args.length == 1) {
            open(player);
            return;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("enter")) {
            enterDungeon(player, args[2].toLowerCase(Locale.ROOT));
            return;
        }
        throw new IllegalArgumentException("Use /dungeons journal.");
    }

    public void setAccessPolicy(Predicate<Player> accessPolicy) {
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
    }

    public boolean grant(Player player) {
        ItemStack journal = findJournal(player);
        if (journal != null) {
            refresh(journal, player);
            return false;
        }
        journal = new ItemStack(Material.WRITTEN_BOOK);
        refresh(journal, player);
        itemDeliveryService.give(player, List.of(journal));
        return true;
    }

    public boolean isJournal(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK || !item.hasItemMeta()) {
            return false;
        }
        String itemId = item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
        return JOURNAL_ITEM_ID.equals(itemId);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!isJournal(event.getItemDrop().getItemStack())) {
            return;
        }
        event.getItemDrop().remove();
        event.getPlayer().sendActionBar(Component.text(
                "Use /dungeons journal to recover it.",
                NamedTextColor.YELLOW
        ));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
                && isJournal(event.getItem())) {
            refresh(event.getItem(), event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        entryConfirmation.clear(event.getPlayer().getUniqueId());
    }

    private void open(Player player) {
        ItemStack journal = findJournal(player);
        if (journal == null) {
            grant(player);
            journal = findJournal(player);
            if (journal == null) {
                throw new IllegalStateException("Make one free inventory slot to recover your Adventure Journal.");
            }
        } else {
            refresh(journal, player);
        }
        player.openBook(journal);
    }

    private void enterDungeon(Player player, String dungeonId) {
        if (!ADAPTIVE_DUNGEON_ID.equals(dungeonId)) {
            throw new IllegalArgumentException("That dungeon has not been explored.");
        }
        DungeonProfile profile = profileService.profile(player);
        Affinity affinity = profile.selectedAffinity().orElseThrow(() ->
                new IllegalStateException("Choose an affinity at spawn before entering."));
        if (instanceManager.sessionFor(player).isPresent()) {
            throw new IllegalStateException("You already have an active dungeon instance.");
        }
        if (!entryConfirmation.confirm(player.getUniqueId(), dungeonId, System.currentTimeMillis())) {
            player.sendMessage(Component.text(
                    "Click Adaptive Dungeon again within 3 seconds to enter.",
                    NamedTextColor.GOLD
            ));
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8F, 1.15F);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && instanceManager.sessionFor(player).isEmpty()) {
                    player.openBook(confirmationBook());
                }
            });
            return;
        }

        player.closeInventory();
        player.sendMessage(Component.text("The dungeon is taking shape...", NamedTextColor.GRAY));
        long seed = ThreadLocalRandom.current().nextLong();
        DungeonSession session = instanceManager.start(player, affinity, seed);
        DungeonInstance instance = session.instance();
        player.sendMessage(Component.text(
                "Entered an adaptive " + instance.layout().rooms().size()
                        + "-room expedition at Dungeon Level " + profile.level() + ".",
                NamedTextColor.AQUA
        ));
    }

    private ItemStack findJournal(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isJournal(item)) {
                return item;
            }
        }
        return null;
    }

    private void refresh(ItemStack journal, Player player) {
        if (journal == null) {
            return;
        }
        List<Component> pages = pages(player);
        pagePolicy.validate(pages);

        BookMeta meta = (BookMeta) journal.getItemMeta();
        meta.title(Component.text("Adventure Journal", NamedTextColor.DARK_PURPLE));
        meta.author(Component.text("MunchyMC", NamedTextColor.DARK_GRAY));
        meta.displayName(Component.text("Adventure Journal", NamedTextColor.DARK_PURPLE)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.setGeneration(BookMeta.Generation.ORIGINAL);
        meta.pages(pages);
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(itemIdKey, PersistentDataType.STRING, JOURNAL_ITEM_ID);
        data.set(categoryKey, PersistentDataType.STRING, "journal");
        data.set(schemaKey, PersistentDataType.INTEGER, ITEM_SCHEMA_VERSION);
        journal.setItemMeta(meta);
    }

    private ItemStack confirmationBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        Component page = dungeonEntryPage(false);
        pagePolicy.validate(List.of(page));
        meta.title(Component.text("Confirm Dungeon", NamedTextColor.DARK_PURPLE));
        meta.author(Component.text("MunchyMC", NamedTextColor.DARK_GRAY));
        meta.setGeneration(BookMeta.Generation.ORIGINAL);
        meta.pages(List.of(page));
        book.setItemMeta(meta);
        return book;
    }

    private List<Component> pages(Player player) {
        DungeonProfile profile = profileService.profile(player);
        DungeonArmorSummary armor = armorService.summary(player);
        long requiredExperience = profileService.experienceRequiredForNextLevel(profile.level());
        double progress = Math.min(100.0D,
                profile.experienceIntoLevel() * 100.0D / requiredExperience);
        DungeonProgressionBenefits.NextLevelBenefits next =
                progressionBenefits.nextLevelBenefits(profile.level());

        return List.of(
                coverPage(player, profile),
                contentsPage(),
                profilePage(player, profile),
                statsPage(player, profile, armor),
                nextLevelPage(profile, requiredExperience, progress, next),
                dungeonsPage()
        );
    }

    private Component coverPage(Player player, DungeonProfile profile) {
        return Component.text()
                .append(Component.text("DUNGEONS", NamedTextColor.DARK_PURPLE)
                        .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("ADVENTURE JOURNAL", NamedTextColor.BLACK)
                        .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(line(""))
                .append(coloredLine("Explorer's Record", NamedTextColor.DARK_GRAY))
                .append(line(player.getName()))
                .append(line("Dungeon Level: " + profile.level()))
                .build();
    }

    private Component contentsPage() {
        return pageTitle("CONTENTS")
                .append(line(""))
                .append(pageLink("[ Profile ]", 3))
                .append(pageLink("[ Current Stats ]", 4))
                .append(pageLink("[ Next Level ]", 5))
                .append(pageLink("[ Explored Dungeons ]", 6))
                .build();
    }

    private Component profilePage(Player player, DungeonProfile profile) {
        String affinity = profile.selectedAffinity().map(Affinity::displayName).orElse("Not selected");
        return pageTitle("PROFILE")
                .append(line(""))
                .append(line("Explorer: " + player.getName()))
                .append(line("Affinity: " + affinity))
                .append(line("Expeditions: " + profile.completedExpeditions()))
                .append(backToContents())
                .build();
    }

    private Component statsPage(Player player, DungeonProfile profile, DungeonArmorSummary armor) {
        AttributeInstance maximumHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double health = maximumHealth == null ? 20.0D : maximumHealth.getValue();
        double damageTakenReduction = (1.0D - armor.incomingDamageMultiplier()) * 100.0D;
        return pageTitle("CURRENT STATS")
                .append(line(""))
                .append(line("Max Health: " + decimal(health)))
                .append(line("Level Damage: +" + percent(progressionBenefits.damageBonus(profile.level()))))
                .append(line("Armor Power: +" + percent(armor.powerBonus())))
                .append(line("Damage Taken: -" + decimal(damageTakenReduction) + "%"))
                .append(line("Special Loot: +" + percent(
                        progressionBenefits.specialLootFindBonus(profile.level()))))
                .append(line("Affinity Boost: +15%"))
                .append(backToContents())
                .build();
    }

    private Component nextLevelPage(
            DungeonProfile profile,
            long requiredExperience,
            double progress,
            DungeonProgressionBenefits.NextLevelBenefits next
    ) {
        TextComponent.Builder page = pageTitle("NEXT LEVEL")
                .append(line("Level " + next.level()))
                .append(line("XP: " + profile.experienceIntoLevel() + " / " + requiredExperience))
                .append(line("Progress: " + decimal(progress) + "%"))
                .append(line(""))
                .append(coloredLine("REWARDS", NamedTextColor.DARK_GREEN))
                .append(line("Damage: +" + percent(next.damageBonus())));
        if (next.lootFindCapped()) {
            page.append(line("Special Loot: Capped"));
        } else {
            page.append(line("Special Loot: +" + percent(next.specialLootFindBonus())));
        }
        return page.append(backToContents()).build();
    }

    private Component dungeonsPage() {
        return dungeonEntryPage(true);
    }

    private Component dungeonEntryPage(boolean includeContentsLink) {
        TextComponent.Builder page = pageTitle("EXPLORED DUNGEONS")
                .append(line(""))
                .append(Component.text("[ Adaptive Dungeon ]", NamedTextColor.DARK_PURPLE)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/dungeons journal enter adaptive"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to enter")))
                        .append(Component.newline()))
                .append(line("Scales to your level."))
                .append(line(""))
                .append(coloredLine("Click to confirm entry.", NamedTextColor.DARK_GRAY));
        if (includeContentsLink) {
            page.append(backToContents());
        }
        return page.build();
    }

    private TextComponent.Builder pageTitle(String title) {
        return Component.text()
                .append(Component.text(title, NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
                .append(Component.newline());
    }

    private Component pageLink(String label, int pageNumber) {
        return Component.text(label, NamedTextColor.DARK_PURPLE)
                .clickEvent(ClickEvent.changePage(pageNumber))
                .hoverEvent(HoverEvent.showText(Component.text("Open this category")))
                .append(Component.newline())
                .decoration(TextDecoration.ITALIC, false);
    }

    private Component backToContents() {
        return Component.text("[ Contents ]", NamedTextColor.DARK_GRAY)
                .clickEvent(ClickEvent.changePage(2))
                .append(Component.newline())
                .decoration(TextDecoration.ITALIC, false);
    }

    private Component line(String value) {
        return Component.text(value, NamedTextColor.BLACK)
                .append(Component.newline())
                .decoration(TextDecoration.ITALIC, false);
    }

    private Component coloredLine(String value, NamedTextColor color) {
        return Component.text(value, color)
                .decorate(TextDecoration.BOLD)
                .append(Component.newline())
                .decoration(TextDecoration.ITALIC, false);
    }

    private String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String percent(double fraction) {
        return String.format(Locale.ROOT, "%.2f%%", fraction * 100.0D);
    }
}
