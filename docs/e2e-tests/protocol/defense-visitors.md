[← Defense](../defense.md) · [Visitors and quests](../visitors-quests.md) · [How the tests work](../framework.md)

# Protocol: defense and visitors/quests (DEF-01..11, VIS-01..04)

Suite: `src/gametest/java/com/minecolonies/gametest/suites/DefenseVisitorsTests.java`, run with
`./gradlew --no-daemon runGameTest -Pe2eFilter=defense_visitors_tests`.

Final run: all 25 required tests passed, 2 optional (bug) tests fail as expected. The whole suite takes about 20 s of server time
(plus Gradle and server start-up). Runtime = game ticks until the test finished in the final run (they vary between runs, see the
budgets in the code).

| ID | Test method(s) | Status | Runtime (ticks) | Notes |
|---|---|---|---|---|
| DEF-01 | `def01_guardKillsHostileMob` | ✅ passing | 150 | Knight kills a husk, gains XP, hits no bystander/owner. |
| DEF-02 | `def02_knightFightsWithSword` | ✅ passing | 165 | Sword in hand while attacking, husk killed. Shield blocking not checked. |
| DEF-02 | `def02_huscarlFightsWithAxe` | ✅ passing | 205 | Research `combat/slicedanddecided`. |
| DEF-02 | `def02_rangerShootsArrows` | ✅ passing | 230 | Arrows owned by the ranger seen. |
| DEF-02 | `def02_marksmanShootsCrossbow` | ✅ passing | 410 | Research `combat/thathitthemark`, crossbow. |
| DEF-02 | `def02_druidThrowsPotions` | ✅ passing | 146 | Design changed: druid slows the mob, doesn't kill it. |
| DEF-02 | cavalry | ⛔ not implementable | - | See DEF-10. |
| DEF-03 | `def03_patrolGuardFollow` | ✅ passing | 5336 | Guard mode checks "returns to the spot"; follow needs the owner in the player lists (test adds him). |
| DEF-04 | `def04_guardGetsGearByLevel` | ✅ passing (partial) | 1580 | Level 1 gear via warehouse + courier. Upgrade part 📝 not done. |
| DEF-05 | `def05_injuredGuardRetreats` | ✅ passing | 2180 | Flee → regen at tower → back on duty. |
| DEF-06 | `def06_barbarianRaid` | ✅ passing | 3586 | Also TOO_SMALL and maxRaiders checks. |
| DEF-06 | `def06_amazonRaid` | ✅ passing | 2594 | |
| DEF-06 | `def06_egyptianRaid` | ✅ passing | 3552 | |
| DEF-06 | `def06_norsemenRaid` | ✅ passing | 3618 | Land raid only. |
| DEF-06 | `def06_pirateGroundRaid` | ✅ passing | 3584 | Pirates without ship (`pirate_ground_raid`). |
| DEF-06 | drowned pirates, ship raids | ⛔ not implementable | - | Ships need deep water next to the colony; the fixture's `startRaid` forbids ships; `drowned_pirate_raid` without ships = NO_SPAWN_POINT. |
| DEF-07 | `def07_raidWithoutDeathMakesHappy` | ✅ passing | 574 | |
| DEF-07 | `def07_raidWithDeathNoBonus` | ✅ passing | 520 | |
| DEF-08 | `def08_citizensFleeFromDanger` | ✅ passing | 775 | Design changed: flee when hurt, not on sight. |
| DEF-09 | `def09_trainingRaisesSkills` | ✅ passing | 2140 | Checks XP gain (levels need far more time). |
| DEF-10 | - | ⛔ not implementable | - | See below. |
| DEF-11 | `def11_raidTimingFollowsConfig` | ✅ passing | 1840 | Raids at nights 3, 6, 9 (min 2 / avg 3); none with raids off. |
| VIS-01 | `vis01_visitorsComeToTavern` | ✅ passing | 12280 | First visitor after ~9700 ticks; capacity 3 never exceeded. |
| VIS-02 | `vis02_recruitVisitor` | ✅ passing | ~1 | `maxAttempts = 4` because 3% of recruits run off (mod design). |
| VIS-02 | `vis02_tavernRemovedAfterRecruiting` | ❌ fails due to mod bug | ~1 | E2E-VIS02-1 (optional test). |
| VIS-03 | `vis03_questFromStartToReward` | ✅ passing | 29 | Welcome quest (real trigger) + hungry courier (injected). |
| VIS-04 | `vis04_declineAndCancelQuests` | ✅ passing | 613 | Decline + cancel in the first dialogue. |
| VIS-04 | `vis04_cancelAcceptedQuestAtDelivery` | ❌ fails due to mod bug | ~1 | E2E-VIS04-1 (optional test). |

Counts: 25 ✅ (test methods), 2 ❌ (mod bugs, optional tests), 3 ⛔ (cavalry/DEF-10, drowned pirates and ship raids), 1 📝 (DEF-04
upgrade part).

**DEF-10 (⛔):** stable level 1 with a stablemaster (lead) and a cavalry guard (spear), two tamed horses inside the stable's corners.
Within 12000 ticks no cavalry horse was registered to the stable: the stablemaster trains only after a cooldown of 50 decisions and then
with 25% chance, only horses inside the building's corners count, and its "gather mounts" step led the horses out of the counted area
(also with the horses' AI off). Getting a deterministic setup would need a stable-specific helper (pen layout, horse placement the
stablemaster accepts); stopped after a reasonable effort. Not evidence of a bug.

## Bugs found

### E2E-VIS04-1 Cancelling an accepted quest at a delivery step does nothing

- **Severity:** medium (a player can't cancel an accepted delivery quest; the "cancel" button only closes the dialogue, the quest stays
  in progress until it times out).
- **Port-specific or upstream:** upstream. The code is in shared MineColonies code (`src/main`), not in the Fabric shim.
- **Evidence:** `core/colony/interactionhandling/QuestDeliveryInteraction.java:55-73`: for a final answer that is not positive
  (`QuestCancellationDialogueAnswer`), the else branch only sets `currentElement = null` and returns; `applyToQuest` (which calls
  `quest.onDeletion()`) is never called. The plain dialogue (`QuestDialogueInteraction.onServerResponseTriggered:81-83`) does call it, so
  "cancel" works in normal dialogues (tested in `vis04_declineAndCancelQuests`).
- **Expected:** the quest is removed from in-progress (not finished). **Found:** `hungrycourier: in progress objective 1`.
- **Reproduction:** `vis04_cancelAcceptedQuestAtDelivery` (hungry courier quest accepted, owner without a baked potato picks response 1
  of the delivery's "waiting" dialogue = cancel).

### E2E-VIS02-1 Removing a tavern right after a recruit throws and leaves visitors behind

- **Severity:** low (needs the tavern to be removed within ~500 ticks of a recruit; then the remaining visitors are not removed).
- **Port-specific or upstream:** upstream (shared code).
- **Evidence:** after a recruit, `RecruitmentInteraction.onServerResponseTriggered` removes the visitor from the visitor manager, but
  `TavernBuildingModule.externalCitizens` keeps its id until the tavern's next colony tick (`onColonyTick` `removeIf`). `onDestroyed()`
  (`TavernBuildingModule.java:309-313`) passes `getVisitor(id)` = null to `VisitorManager.removeCivilian`
  (`VisitorManager.java:301`, `citizen.getId()`) → `NullPointerException`, the loop stops, later visitors stay registered. Also seen in
  the log when the fixture deleted the colony right after `vis02_recruitVisitor`:
  `WARN Something went wrong deleting a building while deleting the colony! java.lang.NullPointerException: Cannot invoke
  "ICivilianData.getId()" because "citizen" is null at VisitorManager.removeCivilian(VisitorManager.java:301) at
  TavernBuildingModule.onDestroyed(TavernBuildingModule.java:312)`.
- **Reproduction:** `vis02_tavernRemovedAfterRecruiting`.

### Suspect (code reading, not reproduced by a test)

- `RaidManager.onRaidEventFinished` (`core/colony/events/raid/RaidManager.java`, the mercy check and the victory sound) reads
  `raidHistories.get(0)`, the colony's *first* raid, instead of the last one (`getLastRaid()`); after more than one raid the mercy
  message/extra delay and the sound choice depend on the first raid's losses. Upstream code.

## Design changes

- **DEF-02 druid:** the druid throws slowness (weakness with research + magic potions) at monsters and support potions only at allies in
  combat; it doesn't kill and doesn't heal idle injured citizens. Test checks potion thrown + husk slowed. Knight shield blocking not
  checked (needs research + shield).
- **DEF-03 guard mode:** a guarding guard leaves for errands (dumping inventory at its hut) and walks back lazily; the test checks it is
  back at the spot later instead of "never leaves".
- **DEF-04:** level 1 only; the upgrade part needs a builder (not done).
- **DEF-06:** no guards (raiders are killed by the test after the approach is seen); only land raids (ships off); pirates = land
  variant.
- **DEF-07:** the citizen is killed with raider-sourced hits by the test.
- **DEF-08:** citizens flee when hurt, not when a monster approaches.
- **DEF-09:** experience gain instead of level rise.
- **DEF-11:** nights are produced by setting the time; raids are ended at once; config min 2 / average 3 makes the timing deterministic
  (raid every third night).
- **VIS-01:** waiting time between visitors skipped after the first natural visitor.
- **VIS-03:** no quest with a happiness reward has a deterministic trigger; the hungry courier quest is injected (only the trigger
  evaluation is skipped).
- **VIS-04:** cancel of an accepted quest is only offered in dialogues (e.g. delivery steps); split into a passing decline/cancel test and
  the bug test.

All changes are written into the "As implemented" paragraphs of `defense.md` and `visitors-quests.md`.

## Framework issues

- **Fresh worktree can't start the server:** `src/main/resources/assets/minecolonies/lang/en_us.json` is git-ignored, so
  `LanguageHandler$LanguageCache.load` NPEs at start. Copied from the main checkout (not committed).
- **The fake owner is in no player list.** Code that looks players up in `level.players()` or the server's `PlayerList` (guard follow:
  `ServerUtils.getPlayerFromUUID(UUID, Level)`, rally banner, raid bars, ...) doesn't find him. The suite has a private helper
  `addOwnerToPlayerList` that adds him to both lists for one test and removes him at the end; it could move into the fixture.
- **Natural raids are impossible with the test config:** `WorldUtil.isPeaceful` treats game rule `spawnMobs = false` as peaceful, so
  `RaidManager.canRaid()` is false; and natural raid spawn points lie 40+ blocks outside the buildings, where the arena has no ground
  (`BlockPosUtil.getFloor` gives up long before y = -61). DEF-11 turns `spawnMobs` on for the test and lays a temporary grass ring around
  the arena (`layGroundAroundArena`, removed at the end).
- **Raiders cap damage without a living attacker** (also the fake owner counts as none), and **citizens cap damage per hit**: tests that
  kill raiders/citizens need a living source (the suite spawns an armor stand) and repeated hits.
- **The fake owner is shared between tests:** game mode, inventory and position carry over. Tests here set the game mode and clear the
  inventory themselves.
- **`fillWarehouse` alone doesn't give deliveries:** a courier needs a courier's hut (`ModBuildings.deliveryman`); hiring at the warehouse
  gives no courier job.
- Helpers in the suite that could move into the framework: `onTestEnd`, `updateSetting` (setting change as from the GUI), `answer` /
  `respondRecruit` (server side of the interaction response message), `injectQuest`, `spawnMob`/`standable`.
