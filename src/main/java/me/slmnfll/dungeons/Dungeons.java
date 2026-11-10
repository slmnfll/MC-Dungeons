package me.slmnfll.dungeons;

import me.slmnfll.dungeons.affinity.DungeonAffinityService;
import me.slmnfll.dungeons.command.DungeonCommand;
import me.slmnfll.dungeons.content.DungeonContentGenerator;
import me.slmnfll.dungeons.content.DungeonContentPlanner;
import me.slmnfll.dungeons.content.DungeonContentService;
import me.slmnfll.dungeons.content.DungeonItemDeliveryService;
import me.slmnfll.dungeons.content.DungeonLootService;
import me.slmnfll.dungeons.content.DungeonRiskService;
import me.slmnfll.dungeons.content.DungeonRiskListener;
import me.slmnfll.dungeons.encounter.DungeonCombatTuning;
import me.slmnfll.dungeons.encounter.DungeonDamageContext;
import me.slmnfll.dungeons.encounter.DungeonEncounterGenerator;
import me.slmnfll.dungeons.encounter.DungeonEncounterPlanner;
import me.slmnfll.dungeons.encounter.DungeonEncounterService;
import me.slmnfll.dungeons.encounter.DungeonExitPortalService;
import me.slmnfll.dungeons.encounter.DungeonGateService;
import me.slmnfll.dungeons.encounter.DungeonRoomLocator;
import me.slmnfll.dungeons.equipment.DungeonEquipmentService;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorRegistry;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorService;
import me.slmnfll.dungeons.equipment.artifact.DungeonArtifactRegistry;
import me.slmnfll.dungeons.equipment.artifact.DungeonArtifactService;
import me.slmnfll.dungeons.equipment.consumable.DungeonConsumableRegistry;
import me.slmnfll.dungeons.equipment.consumable.DungeonConsumableService;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneRegistry;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneService;
import me.slmnfll.dungeons.equipment.rune.ability.DungeonRuneAbilityListener;
import me.slmnfll.dungeons.equipment.rune.smith.DungeonRuneSmithService;
import me.slmnfll.dungeons.equipment.weapon.DungeonWeaponRegistry;
import me.slmnfll.dungeons.equipment.weapon.DungeonWeaponService;
import me.slmnfll.dungeons.instance.DungeonInstanceManager;
import me.slmnfll.dungeons.journal.DungeonJournalService;
import me.slmnfll.dungeons.layout.DungeonLayoutPlanner;
import me.slmnfll.dungeons.layout.DungeonLayoutValidator;
import me.slmnfll.dungeons.listener.AffinityCombatListener;
import me.slmnfll.dungeons.listener.DungeonArmorCombatListener;
import me.slmnfll.dungeons.listener.DungeonArtifactListener;
import me.slmnfll.dungeons.listener.DungeonConsumableListener;
import me.slmnfll.dungeons.listener.DungeonPlayerListener;
import me.slmnfll.dungeons.map.ExpeditionMapService;
import me.slmnfll.dungeons.profile.DungeonLevelCurve;
import me.slmnfll.dungeons.profile.DungeonProfileListener;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import me.slmnfll.dungeons.profile.YamlDungeonProfileRepository;
import me.slmnfll.dungeons.story.prologue.PrologueService;
import me.slmnfll.dungeons.tutorial.SurveySiteZeroDefinition;
import me.slmnfll.dungeons.tutorial.TutorialLoadoutService;
import me.slmnfll.dungeons.tutorial.TutorialObjectiveService;
import me.slmnfll.dungeons.tutorial.TutorialProgressionService;
import me.slmnfll.dungeons.tutorial.SurveySiteZeroMarkerGenerator;
import me.slmnfll.dungeons.world.DungeonWorldService;
import me.slmnfll.dungeons.world.GrayboxDungeonBuilder;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Dungeons extends JavaPlugin {
    private DungeonInstanceManager instanceManager;
    private DungeonContentService contentService;
    private DungeonEncounterService encounterService;
    private DungeonProfileService profileService;
    private DungeonRuneSmithService runeSmithService;
    private PrologueService prologueService;
    private TutorialObjectiveService tutorialObjectiveService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ExpeditionMapService mapService = new ExpeditionMapService(this);
        DungeonRiskService riskService = new DungeonRiskService(this);
        DungeonItemDeliveryService itemDeliveryService = new DungeonItemDeliveryService();
        profileService = new DungeonProfileService(
                this,
                new YamlDungeonProfileRepository(this),
                new DungeonLevelCurve()
        );
        profileService.start();
        DungeonRuneRegistry runeRegistry = new DungeonRuneRegistry();
        DungeonWeaponService weaponService = new DungeonWeaponService(
                this,
                new DungeonWeaponRegistry(),
                runeRegistry
        );
        DungeonArtifactService artifactService = new DungeonArtifactService(this, new DungeonArtifactRegistry());
        DungeonConsumableService consumableService = new DungeonConsumableService(
                this,
                new DungeonConsumableRegistry()
        );
        DungeonArmorService armorService = new DungeonArmorService(
                this,
                new DungeonArmorRegistry(),
                profileService,
                runeRegistry
        );
        DungeonRuneService runeService = new DungeonRuneService(
                this,
                runeRegistry,
                weaponService,
                armorService,
                itemDeliveryService
        );
        runeSmithService = new DungeonRuneSmithService(this, runeService, itemDeliveryService);
        DungeonEquipmentService equipmentService = new DungeonEquipmentService(this);
        DungeonContentPlanner contentPlanner = new DungeonContentPlanner();
        DungeonGateService gateService = new DungeonGateService();
        SurveySiteZeroDefinition tutorialDefinition = new SurveySiteZeroDefinition();
        DungeonWorldService worldService = new DungeonWorldService(
                this,
                new GrayboxDungeonBuilder(),
                new DungeonContentGenerator(contentPlanner, tutorialDefinition),
                new DungeonEncounterGenerator(new DungeonEncounterPlanner(), gateService, tutorialDefinition),
                new SurveySiteZeroMarkerGenerator()
        );
        int abandonedWorlds = worldService.cleanupAbandonedWorlds();
        if (abandonedWorlds > 0) {
            getLogger().info("Removed " + abandonedWorlds + " abandoned dungeon world(s).");
        }
        instanceManager = new DungeonInstanceManager(
                this,
                new DungeonLayoutPlanner(),
                new DungeonLayoutValidator(),
                worldService,
                mapService,
                riskService,
                equipmentService,
                profileService,
                tutorialDefinition
        );
        DungeonAffinityService affinityService = new DungeonAffinityService(
                profileService,
                instanceManager
        );
        DungeonDamageContext damageContext = new DungeonDamageContext();

        DungeonLootService lootService = new DungeonLootService(
                this,
                weaponService,
                armorService,
                artifactService,
                runeService,
                consumableService
        );
        DungeonJournalService journalService = new DungeonJournalService(
                this,
                instanceManager,
                profileService,
                armorService,
                itemDeliveryService
        );
        prologueService = new PrologueService(
                this,
                affinityService,
                instanceManager,
                profileService,
                journalService
        );
        instanceManager.setAdventureAccess(prologueService::hasCompleted);
        journalService.setAccessPolicy(prologueService::hasCompleted);

        DungeonCommand dungeonCommand = new DungeonCommand(
                instanceManager,
                profileService,
                affinityService,
                weaponService,
                armorService,
                artifactService,
                runeService,
                consumableService,
                runeSmithService,
                itemDeliveryService,
                journalService,
                prologueService
        );
        PluginCommand command = getCommand("dungeons");
        if (command == null) {
            throw new IllegalStateException("The dungeons command is missing from plugin.yml.");
        }
        command.setExecutor(dungeonCommand);
        command.setTabCompleter(dungeonCommand);

        getServer().getPluginManager().registerEvents(
                new DungeonPlayerListener(this, instanceManager, mapService),
                this
        );
        getServer().getPluginManager().registerEvents(
                new AffinityCombatListener(this, profileService, weaponService, armorService, damageContext),
                this
        );
        getServer().getPluginManager().registerEvents(
                new DungeonRuneAbilityListener(this, runeService, damageContext),
                this
        );
        getServer().getPluginManager().registerEvents(new DungeonArmorCombatListener(armorService), this);
        getServer().getPluginManager().registerEvents(
                new DungeonArtifactListener(artifactService, profileService, damageContext),
                this
        );
        getServer().getPluginManager().registerEvents(
                new DungeonConsumableListener(consumableService, damageContext),
                this
        );
        getServer().getPluginManager().registerEvents(runeSmithService, this);
        getServer().getPluginManager().registerEvents(journalService, this);
        getServer().getPluginManager().registerEvents(prologueService, this);
        getServer().getPluginManager().registerEvents(
                new DungeonProfileListener(this, profileService),
                this
        );
        getServer().getPluginManager().registerEvents(new DungeonRiskListener(riskService), this);
        getServer().getPluginManager().registerEvents(new TutorialProgressionService(profileService), this);
        getServer().getPluginManager().registerEvents(
                new TutorialLoadoutService(
                        this,
                        armorService,
                        weaponService,
                        artifactService,
                        consumableService,
                        itemDeliveryService,
                        profileService
                ),
                this
        );
        tutorialObjectiveService = new TutorialObjectiveService(this, instanceManager);
        getServer().getPluginManager().registerEvents(tutorialObjectiveService, this);
        tutorialObjectiveService.start();
        contentService = new DungeonContentService(
                this,
                instanceManager,
                lootService,
                itemDeliveryService,
                profileService,
                riskService,
                tutorialObjectiveService
        );
        getServer().getPluginManager().registerEvents(contentService, this);
        contentService.start();

        encounterService = new DungeonEncounterService(
                this,
                instanceManager,
                profileService,
                lootService,
                itemDeliveryService,
                gateService,
                new DungeonRoomLocator(),
                new DungeonExitPortalService(),
                new DungeonCombatTuning(),
                riskService
        );
        getServer().getPluginManager().registerEvents(encounterService, this);
        encounterService.start();
        prologueService.start();
        getLogger().info("Dungeon instance prototype enabled.");
    }

    @Override
    public void onDisable() {
        if (tutorialObjectiveService != null) {
            tutorialObjectiveService.stop();
        }
        if (runeSmithService != null) {
            runeSmithService.shutdown();
        }
        if (encounterService != null) {
            encounterService.stop();
        }
        if (contentService != null) {
            contentService.stop();
        }
        if (instanceManager != null) {
            instanceManager.shutdown();
        }
        if (prologueService != null) {
            prologueService.stop();
        }
        if (profileService != null) {
            profileService.close();
        }
    }
}
