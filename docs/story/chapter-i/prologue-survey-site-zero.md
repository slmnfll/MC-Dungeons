# Prologue — Survey Site Zero

## Purpose

Introduce the guild, class affinity, dungeon risk, and the first hint that Aevori is not truly gone.

## Player Flow

1. The objective bar sends a new explorer to the **Guild Chronicler** at spawn.
2. The Chronicler gives a humorous introduction and asks how experienced they are.
3. The player chooses **Vanguard**, **Arbalist**, or **Ravager** and is told to empty their inventory.
4. The player enters **Survey Site Zero**, is permanently placed at Dungeon Level 1, and receives that affinity's starter set.
5. Reaching the exit reveals an undocumented Warden and the phrase *the gate remembers us*. Death ends the survey with the same story progression but forfeits unsecured finds.
6. The player returns to the Chronicler, receives the **Adventure Journal**, and unlocks adventure dungeons beginning with the route to the **Rootbound Gate**.

## Implementation Boundary

Survey Site Zero uses its own deterministic graybox definition rather than the random adventure generator. It has six linear rooms, five fixed pots, one guaranteed virtual key, one locked chest, one fixed Zombie-and-Skeleton encounter, and an 80-health Aevori Survey Warden. The route teaches pots, automatic collection, keys, locked chests, combat gates, unsecured loot, the expedition map, consumable healing, and the exit portal in order. Builders can later replace the graybox geometry without changing this progression contract.

Survey Site Zero is the one-time gateway into Dungeon progression. Its first successful start permanently resets old prototype progression to **Dungeon Level 1**, zero Dungeon XP, and zero completed expeditions while preserving the chosen affinity. Its kills, encounter clears, chest, and boss award no Dungeon XP. Adventure dungeons and the Adventure Journal remain locked until the survey ends through either the exit portal or player death and the Chronicler completes the prologue. Disconnecting, leaving, or being ejected does not graduate the player and permits another attempt.

The first attempt requires a completely empty inventory, including armor and offhand slots. Inside, the guild permanently issues a full affinity armor set, the affinity's starter weapon, a revealed affinity artifact, three Mossbreads, two Lumen Berries, and 32 Survey Bolts for Arbalists. These items are marked as **Guild starter items** and survive every outcome. An interrupted retry may bring only those starter items back into the survey.

Hunger is held steady inside every dungeon and passive healing is blocked. Recovery foods apply explicit Regeneration effects: Mossbread grants Regeneration I for 10 seconds, Sunroot grants Regeneration I for 15 seconds, Deepdelve Stew grants Regeneration II for 12 seconds, and Lumen Berries grant Regeneration III for 6 seconds. Their vanilla item cooldowns last three to five seconds.

Tutorial pots use a fixed showcase table rather than the normal currency table: Mossbread, a Jewelry Fragment, Sturdy Arrows, an Epic-or-higher rune, and Aevori Scrap. The locked chest demonstrates an affinity weapon upgrade and Sunroot, the combat room demonstrates Deepdelve Stew, and the Warden drops a Survey Warden Fragment. Every found item displays **UNSECURED LOOT**. Crossing the exit portal removes that warning and keeps the finds; dying removes all unsecured tutorial finds while preserving the starter set. The Warden remains deliberately reduced to 80 health and at most 6 direct damage per hit.

The first boss-bar objective directs the player into the pot-training chamber. Tutorial pots remain sealed until the player reaches that chamber and the boss bar explicitly changes to **Break a pot**; pots in regular adventure dungeons are unaffected by this training restriction.

Every room contains a concise floating **Guild Survey Marker** explaining the mechanic taught there. The boss bar remains the authoritative current objective, while the Chronicler handles story and preparation at spawn rather than following the explorer through the ruin.

The Chronicler is a native Minecraft mannequin with native Paper dialogues. Short lines accumulate into a chat-style transcript before the player receives response choices, and the selected player response is added before the Chronicler continues. A blank line separates the speaker heading from each phrase and separates consecutive phrases. The transcript has a 30-logical-line budget: headings, messages, invisible spacer lines, and the gap before response buttons all count toward it. When older content is hidden, `Earlier messages…` receives two blank lines before the remaining transcript. Each new Chronicler line remains visible for 1.5–2 seconds according to its visible length and plays a spatial villager voice cue. The first line receives a small rendering allowance within that cap because the client is still opening the dialogue. Dialogue portraits are intentionally omitted. Continue/proceed buttons use green text, cancel/close buttons use red, and neutral selections use aqua. Dialogues that continue use Paper's waiting transition, while terminal conversations use normal close behavior and an explicit `✕` button. Native plain-message bodies are centered and expose width but no alignment or font-size setting, so truly left-aligned or enlarged text would require a resource pack/custom font. The prototype uses Mojang's `MHF_Villager` profile by default, and the skin remains configurable from another Minecraft profile name. No NPC plugin is required for this version.
