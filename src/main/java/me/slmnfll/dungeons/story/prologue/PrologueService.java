package me.slmnfll.dungeons.story.prologue;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import me.slmnfll.dungeons.instance.DungeonInstanceState;
import me.slmnfll.dungeons.instance.DungeonRunEndReason;
import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.affinity.DungeonAffinityService;
import me.slmnfll.dungeons.instance.DungeonInstance;
import me.slmnfll.dungeons.instance.DungeonInstanceManager;
import me.slmnfll.dungeons.instance.DungeonRunClosedEvent;
import me.slmnfll.dungeons.instance.DungeonSession;
import me.slmnfll.dungeons.journal.DungeonJournalService;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;
import java.util.UUID;

public final class PrologueService implements Listener {
    private static final String CHRONICLER_NPC_ID = "guild_chronicler";
    private static final String DEFAULT_SKIN_PROFILE = "MHF_Villager";
    private final JavaPlugin plugin;
    private final DungeonAffinityService affinityService;
    private final DungeonInstanceManager instanceManager;
    private final DungeonProfileService profileService;
    private final DungeonJournalService journalService;
    private final YamlPrologueRepository repository;
    private final PrologueDialogueFactory dialogues = new PrologueDialogueFactory();
    private final PrologueMessageTiming messageTiming = new PrologueMessageTiming();
    private final PrologueTranscriptLayout transcriptLayout = new PrologueTranscriptLayout();
    private final NamespacedKey npcIdKey;
    private final Map<UUID, PrologueProgress> progressByPlayer = new HashMap<>();
    private final Map<UUID, BossBar> objectiveBars = new HashMap<>();
    private final Map<UUID, Long> conversationVersions = new HashMap<>();
    private final Map<UUID, List<TranscriptLine>> transcripts = new HashMap<>();

    public PrologueService(
            JavaPlugin plugin,
            DungeonAffinityService affinityService,
            DungeonInstanceManager instanceManager,
            DungeonProfileService profileService,
            DungeonJournalService journalService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.affinityService = Objects.requireNonNull(affinityService, "affinityService");
        this.instanceManager = Objects.requireNonNull(instanceManager, "instanceManager");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.journalService = Objects.requireNonNull(journalService, "journalService");
        this.repository = new YamlPrologueRepository(plugin);
        this.npcIdKey = new NamespacedKey(plugin, "npc_id");
    }

    public void start() {
        findChronicler().ifPresent(this::configureChronicler);
        applySkinProfile(configuredSkinProfile(), null, false);
        Bukkit.getOnlinePlayers().forEach(player -> {
            progress(player);
            refreshObjective(player);
        });
    }

    public void stop() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideObjective(player);
        }
        progressByPlayer.forEach(repository::save);
        repository.flush();
        progressByPlayer.clear();
        conversationVersions.clear();
        transcripts.clear();
    }

    public void handle(Player player, String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException(
                    "Use /dungeons prologue <place|reset|skin|respond>."
            );
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "respond" -> {
                if (args.length != 3) {
                    throw new IllegalArgumentException("That dialogue response is invalid.");
                }
                respond(player, args[2].toLowerCase(Locale.ROOT));
            }
            case "place" -> {
                requireAdmin(player);
                if (args.length != 2) {
                    throw new IllegalArgumentException("Use /dungeons prologue place.");
                }
                placeChronicler(player);
            }
            case "reset" -> {
                requireAdmin(player);
                if (args.length != 2) {
                    throw new IllegalArgumentException("Use /dungeons prologue reset.");
                }
                reset(player);
            }
            case "skin" -> {
                requireAdmin(player);
                if (args.length != 3) {
                    throw new IllegalArgumentException("Use /dungeons prologue skin <MinecraftName>.");
                }
                setSkinProfile(args[2], player);
            }
            default -> throw new IllegalArgumentException(
                    "Use /dungeons prologue <place|reset|skin|respond>."
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || !(event.getRightClicked() instanceof Mannequin mannequin)
                || !isChronicler(mannequin)) {
            return;
        }
        event.setCancelled(true);
        talk(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            progress(event.getPlayer());
            refreshObjective(event.getPlayer());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PrologueProgress progress = progressByPlayer.remove(player.getUniqueId());
        if (progress != null) {
            repository.save(player.getUniqueId(), progress);
            repository.flush();
        }
        hideObjective(player);
        conversationVersions.remove(player.getUniqueId());
        transcripts.remove(player.getUniqueId());
    }

    @EventHandler
    public void onDungeonClosed(DungeonRunClosedEvent event) {
        Player player = event.player();
        PrologueProgress progress = progress(player);
        if (!progress.resolveTutorialClose(event.finalState(), event.endReason())) {
            return;
        }
        persist(player, progress);
        if (event.finalState() == DungeonInstanceState.COMPLETED) {
            player.sendMessage(Component.text(
                    "Survey Site Zero complete. Return to the Guild Chronicler.",
                    NamedTextColor.GOLD
            ));
        } else if (event.endReason() == DungeonRunEndReason.DEATH) {
            player.sendMessage(Component.text(
                    "Survey Site Zero has made its point. Return to the Guild Chronicler.",
                    NamedTextColor.GOLD
            ));
        } else {
            player.sendMessage(Component.text(
                    "The survey ended. Speak to the Guild Chronicler when you are ready to retry.",
                    NamedTextColor.YELLOW
            ));
        }
    }

    private void talk(Player player) {
        transcripts.put(player.getUniqueId(), new ArrayList<>());
        switch (progress(player).stage()) {
            case NEW -> playConversation(player, dialogues.introduction());
            case CHOOSING_AFFINITY -> playConversation(player, dialogues.affinityReminder());
            case TUTORIAL_READY -> {
                Affinity affinity = selectedAffinity(player);
                playConversation(player, dialogues.tutorialReady(affinity));
            }
            case TUTORIAL_ACTIVE -> playConversation(player, dialogues.activeTutorial());
            case RETURN_TO_CHRONICLER -> playConversation(player, dialogues.returnFromTutorial());
            case COMPLETE -> playConversation(player, dialogues.completed());
        }
    }

    private void respond(Player player, String response) {
        PrologueProgress progress = progress(player);
        if (response.equals("intro_professional")
                || response.equals("intro_chests")
                || response.equals("intro_unaware")) {
            progress.advance(PrologueStage.NEW, PrologueStage.CHOOSING_AFFINITY);
            persist(player, progress);
            appendPlayerReply(player, response);
            openNextTick(player, () -> playConversation(
                    player,
                    dialogues.affinity(response)
            ));
            return;
        }
        if (response.startsWith("affinity_")) {
            if (progress.stage() != PrologueStage.CHOOSING_AFFINITY) {
                throw new IllegalStateException("That prologue choice is no longer available.");
            }
            Affinity affinity = parseAffinityResponse(response);
            affinityService.select(player, affinity);
            progress.advance(PrologueStage.CHOOSING_AFFINITY, PrologueStage.TUTORIAL_READY);
            persist(player, progress);
            appendPlayerReply(player, response);
            openNextTick(player, () -> playConversation(
                    player,
                    dialogues.tutorialReady(affinity)
            ));
            return;
        }
        switch (response) {
            case "begin_tutorial" -> {
                if (progress.stage() != PrologueStage.TUTORIAL_READY) {
                    throw new IllegalStateException("That assignment is not currently available.");
                }
                openNextTick(player, () -> {
                    player.closeDialog();
                    startTutorial(player);
                });
            }
            case "finish_prologue" -> {
                progress.advance(PrologueStage.RETURN_TO_CHRONICLER, PrologueStage.COMPLETE);
                persist(player, progress);
                if (journalService.grant(player)) {
                    player.sendMessage(Component.text(
                            "The Guild Chronicler handed you the Adventure Journal.",
                            NamedTextColor.GOLD
                    ));
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.9F, 1.0F);
                }
                appendPlayerReply(player, response);
                player.sendMessage(Component.text(
                        "The Rootbound Gate has been added to the expedition plan.",
                        NamedTextColor.DARK_PURPLE
                ));
                openNextTick(player, () -> playConversation(
                        player,
                        dialogues.completed()
                ));
            }
            case "not_yet", "close" -> {
                conversationVersions.merge(player.getUniqueId(), 1L, Long::sum);
                transcripts.remove(player.getUniqueId());
                player.closeDialog();
            }
            default -> throw new IllegalArgumentException("That dialogue response is invalid.");
        }
    }

    private void startTutorial(Player player) {
        PrologueProgress progress = progress(player);
        if (progress.stage() != PrologueStage.TUTORIAL_READY) {
            return;
        }
        Affinity affinity = selectedAffinity(player);
        player.sendMessage(Component.text("Survey Site Zero is taking shape...", NamedTextColor.GRAY));
        try {
            DungeonSession session = instanceManager.startTutorial(player, affinity);
            progress.advance(PrologueStage.TUTORIAL_READY, PrologueStage.TUTORIAL_ACTIVE);
            persist(player, progress);
            DungeonInstance instance = session.instance();
            player.sendMessage(Component.text(
                    "Entered the " + instance.layout().rooms().size()
                            + "-room tutorial survey. Follow the expedition map.",
                    NamedTextColor.AQUA
            ));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
            refreshObjective(player);
        }
    }

    private void placeChronicler(Player player) {
        Location location = player.getLocation().clone();
        Mannequin chronicler = findChronicler().orElseGet(() ->
                player.getWorld().spawn(location, Mannequin.class));
        chronicler.teleport(location);
        configureChronicler(chronicler);
        player.sendMessage(Component.text(
                "The Guild Chronicler was placed at your location.",
                NamedTextColor.GREEN
        ));
        applySkinProfile(configuredSkinProfile(), player, false);
    }

    private void configureChronicler(Mannequin chronicler) {
        chronicler.getPersistentDataContainer().set(
                npcIdKey,
                PersistentDataType.STRING,
                CHRONICLER_NPC_ID
        );
        chronicler.customName(Component.text("Guild Chronicler", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        chronicler.setCustomNameVisible(true);
        chronicler.setDescription(Component.text("Right-click to speak", NamedTextColor.GRAY));
        chronicler.setImmovable(true);
        chronicler.setAI(false);
        chronicler.setInvulnerable(true);
        chronicler.setCollidable(false);
        chronicler.setSilent(true);
        chronicler.setPersistent(true);
        chronicler.setRemoveWhenFarAway(false);
        chronicler.getEquipment().setItemInMainHand(new ItemStack(Material.WRITTEN_BOOK));
    }

    private void setSkinProfile(String profileName, Player requester) {
        if (profileName.equalsIgnoreCase("default")) {
            profileName = DEFAULT_SKIN_PROFILE;
        }
        plugin.getConfig().set("prologue.chronicler.skin-profile", profileName);
        plugin.saveConfig();
        applySkinProfile(profileName, requester, true);
    }

    private String configuredSkinProfile() {
        String configured = plugin.getConfig().getString(
                "prologue.chronicler.skin-profile",
                DEFAULT_SKIN_PROFILE
        );
        return configured == null || configured.isBlank() ? DEFAULT_SKIN_PROFILE : configured;
    }

    private void applySkinProfile(String profileName, Player requester, boolean announce) {
        PlayerProfile unresolved = Bukkit.createProfile(profileName);
        unresolved.update().whenComplete((profile, failure) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (failure != null || profile == null || !profile.hasTextures()) {
                if (announce) {
                    requester.sendMessage(Component.text(
                            "Could not resolve that Minecraft skin profile.",
                            NamedTextColor.RED
                    ));
                }
                return;
            }
            Mannequin chronicler = findChronicler().orElse(null);
            if (chronicler == null) {
                if (announce) {
                    requester.sendMessage(Component.text(
                            "Place the Guild Chronicler before setting its skin.",
                            NamedTextColor.RED
                    ));
                }
                return;
            }
            chronicler.setProfile(ResolvableProfile.resolvableProfile(profile));
            if (announce) {
                requester.sendMessage(Component.text(
                        "Applied the skin from " + profileName + " to the Guild Chronicler.",
                        NamedTextColor.GREEN
                ));
            }
        }));
    }

    private Optional<Mannequin> findChronicler() {
        return Bukkit.getWorlds().stream()
                .flatMap(world -> world.getEntitiesByClass(Mannequin.class).stream())
                .filter(this::isChronicler)
                .findFirst();
    }

    private boolean isChronicler(Mannequin mannequin) {
        String npcId = mannequin.getPersistentDataContainer().get(npcIdKey, PersistentDataType.STRING);
        return CHRONICLER_NPC_ID.equals(npcId);
    }

    private void reset(Player player) {
        if (instanceManager.sessionFor(player).isPresent()) {
            throw new IllegalStateException("Leave your active dungeon before resetting the prologue.");
        }
        PrologueProgress progress = progress(player);
        progress.reset();
        transcripts.remove(player.getUniqueId());
        persist(player, progress);
        player.closeDialog();
        player.sendMessage(Component.text(
                "Your prologue progress was reset. Starter equipment claims were preserved.",
                NamedTextColor.YELLOW
        ));
    }

    private void persist(Player player, PrologueProgress progress) {
        repository.save(player.getUniqueId(), progress);
        repository.flush();
        refreshObjective(player);
    }

    private PrologueProgress progress(Player player) {
        return progressByPlayer.computeIfAbsent(player.getUniqueId(), repository::load);
    }

    public boolean hasCompleted(Player player) {
        return progress(player).stage() == PrologueStage.COMPLETE;
    }

    private void refreshObjective(Player player) {
        hideObjective(player);
        Component objective = switch (progress(player).stage()) {
            case NEW -> Component.text("Objective: Speak with the Guild Chronicler at Spawn");
            case CHOOSING_AFFINITY -> Component.text("Objective: Choose your affinity with the Guild Chronicler");
            case TUTORIAL_READY -> Component.text("Objective: Begin Survey Site Zero");
            case TUTORIAL_ACTIVE, COMPLETE -> null;
            case RETURN_TO_CHRONICLER -> Component.text("Objective: Return to the Guild Chronicler");
        };
        if (objective == null || !player.isOnline()) {
            return;
        }
        BossBar bar = BossBar.bossBar(
                objective.color(NamedTextColor.GOLD),
                1.0F,
                BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS
        );
        objectiveBars.put(player.getUniqueId(), bar);
        player.showBossBar(bar);
    }

    private void hideObjective(Player player) {
        BossBar existing = objectiveBars.remove(player.getUniqueId());
        if (existing != null) {
            player.hideBossBar(existing);
        }
    }

    private Affinity selectedAffinity(Player player) {
        return profileService.profile(player).selectedAffinity().orElseThrow(() ->
                new IllegalStateException("Choose an affinity before beginning the survey."));
    }

    private Affinity parseAffinityResponse(String response) {
        String affinityId = response.substring("affinity_".length());
        return Affinity.fromId(affinityId).orElseThrow(() ->
                new IllegalArgumentException("That affinity is not available."));
    }

    private void openNextTick(Player player, Runnable action) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                action.run();
            }
        });
    }

    private void playConversation(Player player, PrologueConversation conversation) {
        long version = conversationVersions.merge(player.getUniqueId(), 1L, Long::sum);
        showConversationMessage(player, conversation, 0, version);
    }

    private void showConversationMessage(
            Player player,
            PrologueConversation conversation,
            int messageIndex,
            long version
    ) {
        if (!player.isOnline()
                || conversationVersions.getOrDefault(player.getUniqueId(), 0L) != version) {
            return;
        }
        if (messageIndex >= conversation.messages().size()) {
            appendChroniclerLine(player, conversation.finalMessage());
            player.showDialog(dialogues.response(
                    renderTranscript(player),
                    conversation.responseButtons(),
                    conversation.waitsForContinuation()
            ));
            playChroniclerVoice(player);
            return;
        }
        appendChroniclerLine(player, conversation.messages().get(messageIndex));
        player.showDialog(dialogues.message(renderTranscript(player)));
        playChroniclerVoice(player);
        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> showConversationMessage(player, conversation, messageIndex + 1, version),
                messageTiming.delayTicks(
                        conversation.messages().get(messageIndex),
                        messageIndex == 0
                )
        );
    }

    private void playChroniclerVoice(Player player) {
        Mannequin chronicler = findChronicler().orElse(null);
        if (chronicler != null && chronicler.getWorld().equals(player.getWorld())) {
            chronicler.swingMainHand();
            player.playSound(
                    chronicler.getLocation(),
                    Sound.ENTITY_VILLAGER_AMBIENT,
                    SoundCategory.VOICE,
                    1.0F,
                    0.9F
            );
            return;
        }
        player.playSound(
                player.getLocation(),
                Sound.ENTITY_VILLAGER_AMBIENT,
                SoundCategory.VOICE,
                0.9F,
                0.9F
        );
    }

    private void appendChroniclerLine(Player player, Component message) {
        transcript(player).add(new TranscriptLine(false, message));
    }

    private void appendPlayerReply(Player player, String response) {
        Component message = switch (response) {
            case "intro_professional" -> Component.text("I'm a professional.");
            case "intro_chests" -> Component.text("I've opened several chests.");
            case "intro_unaware" -> Component.text("Nobody mentioned dungeons.");
            case "affinity_vanguard" -> Component.text("Vanguard — Sword");
            case "affinity_arbalist" -> Component.text("Arbalist — Crossbow");
            case "affinity_ravager" -> Component.text("Ravager — Axe");
            case "finish_prologue" -> Component.text("What happens now?");
            default -> null;
        };
        if (message != null) {
            transcript(player).add(new TranscriptLine(true, message));
        }
    }

    private List<TranscriptLine> transcript(Player player) {
        return transcripts.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayList<>());
    }

    private Component renderTranscript(Player player) {
        List<TranscriptLine> transcript = transcript(player);
        List<Boolean> speakers = transcript.stream().map(TranscriptLine::player).toList();
        int firstVisible = transcriptLayout.window(speakers).firstMessageIndex();
        Component rendered = Component.empty();
        Boolean previousPlayer = null;
        if (firstVisible > 0) {
            rendered = rendered.append(Component.text("Earlier messages…", NamedTextColor.DARK_GRAY)
                    .decorate(TextDecoration.ITALIC))
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.newline());
        }
        for (int index = firstVisible; index < transcript.size(); index++) {
            TranscriptLine line = transcript.get(index);
            boolean startsSpeakerGroup = previousPlayer == null || previousPlayer != line.player();
            if (previousPlayer != null) {
                rendered = rendered.append(Component.newline());
                rendered = rendered.append(Component.newline());
            }
            if (startsSpeakerGroup) {
                Component speaker = line.player()
                        ? Component.text("You", NamedTextColor.AQUA).decorate(TextDecoration.BOLD)
                        : Component.text("Guild Chronicler", NamedTextColor.DARK_PURPLE)
                                .decorate(TextDecoration.BOLD);
                rendered = rendered.append(speaker)
                        .append(Component.newline())
                        .append(Component.newline());
            }
            rendered = rendered.append(line.message());
            previousPlayer = line.player();
        }
        return rendered;
    }

    private void requireAdmin(Player player) {
        if (!player.hasPermission("dungeons.admin")) {
            throw new IllegalStateException("You do not have access to that prologue command.");
        }
    }

    private record TranscriptLine(boolean player, Component message) {
        private TranscriptLine {
            Objects.requireNonNull(message, "message");
        }
    }
}
