[← Back to the test overview](E2E-tests.md) · [Test protocol](e2e-tests/PROTOCOL.md)

# Upstream bug reports (MineColonies and Structurize)

The end-to-end tests of the Fabric port ([test protocol](e2e-tests/PROTOCOL.md)) found these bugs in code the port shares with the
original mods. They are not caused by the port: each one is in the original sources as quoted. Every entry is written so it can be filed
as an issue as it stands: title, affected branches, steps to reproduce in normal gameplay, expected and actual behavior, root cause with
file and line, and a suggested patch. The same changes are applied in the Fabric port; the e2e test named at the end of each entry covers
it and is a required test there.

**Branches checked**

- MineColonies **`upstream/version/1.21`** (NeoForge 1.21.1, head `d66bdaae3a` "Packet range (#11888)"): the branch the Fabric port is
  based on. Line numbers are for this branch unless noted.
- MineColonies **`version/main`** (Forge 1.20.1, head `b2fb29ec11` "Hotfix"): line numbers are given as well where the bug exists there
  too.
- Structurize **`version/1.21`** at `9ad76cfd3a` (the copy vendored by the port), for the [Structurize section](#structurize).

**Two fixes change gameplay balance; the maintainers should decide how to fix them:**

- [E2E-CIT06-1](#e2e-cit06-1-unemployed-or-sick-citizens-can-be-happier-than-healthy-employed-ones) (happiness average): averaging over
  all factors again (as before #9092) makes neutral factors count, which raises typical happiness values. The factor weights or the final
  scale may need rebalancing instead.
- [E2E-SRV02-1](#e2e-srv02-1-library-study-items-make-studying-less-successful) (study chance): with the inverted check fixed, the base
  chance to gain Intelligence per study session drops from 87.5% to 12.5%; study items then raise it as configured (book 75%). The
  config values may need adjusting so libraries don't feel slower than today.

The other fixes only restore the intended behavior.

## MineColonies

19 bugs, and two [visual quirks](#visual-quirks-not-filed-as-bugs).

| ID | Title | `version/1.21` | `version/main` (1.20.1) |
|---|---|---|---|
| [E2E-CIT06-1](#e2e-cit06-1-unemployed-or-sick-citizens-can-be-happier-than-healthy-employed-ones) | Negative happiness factors can raise total happiness (gameplay change) | yes | yes |
| [E2E-SRV02-1](#e2e-srv02-1-library-study-items-make-studying-less-successful) | Library study items make studying less successful (gameplay change) | yes | yes |
| [E2E-COL-05-1](#e2e-col-05-1-friends-cant-open-hut-menus) | Friends can't open hut menus | yes | no (different container check) |
| [E2E-CON-01-1](#e2e-con-01-1-a-removed-connection-sign-leaves-a-stale-link-in-the-other-colony) | A removed connection sign leaves a stale link in the other colony | yes | yes |
| [E2E-CON-02-1](#e2e-con-02-1-ally-teleport-only-works-when-the-player-is-within-15-blocks-of-the-target-gate) | Ally teleport only works within 15 blocks of the target gate | yes | yes |
| [E2E-VIS04-1](#e2e-vis04-1-cancelling-an-accepted-quest-at-a-delivery-step-does-nothing) | Cancelling an accepted quest at a delivery step does nothing | yes | yes |
| [E2E-VIS02-1](#e2e-vis02-1-removing-a-tavern-right-after-recruiting-a-visitor-throws-and-leaves-visitors-behind) | Removing a tavern right after a recruit throws | yes | yes |
| [E2E-DEF-S1](#e2e-def-s1-raid-end-uses-the-colonys-first-raid-instead-of-the-one-that-just-ended) | Raid end reads the first raid instead of the last | yes | yes |
| [E2E-CRF01-1](#e2e-crf01-1-crusher-crafts-a-request-again-before-it-is-closed-wastes-ingredients) | Crusher crafts a request again before it is closed | yes | yes |
| [E2E-CRF07-1](#e2e-crf07-1-enchanter-drains-himself-instead-of-the-workers-of-his-stations) | Enchanter drains himself instead of his stations' workers | yes | yes |
| [E2E-GAT-03-1](#e2e-gat-03-1-miner-falls-off-the-shaft-ladder-when-he-stops-on-it) | Miner falls off the shaft ladder when he stops on it | yes | yes |
| [E2E-GAT-04-1](#e2e-gat-04-1-miner-never-seals-water-or-lava-next-to-a-mine-node) | Miner never seals water or lava next to a mine node | yes | yes |
| [E2E-GAT-06-1](#e2e-gat-06-1-nether-worker-stands-at-the-portal-for-up-to-3-days-after-an-expedition) | Nether worker idles at the portal after an expedition | yes | yes |
| [E2E-FRM-06-1](#e2e-frm-06-1-herder-and-beekeeper-feeds-animals-that-are-too-far-apart-to-mate) | Herder (and beekeeper) feeds animals too far apart to mate | yes | yes |
| [E2E-FRM-03-1](#e2e-frm-03-1-florist-compost-on-a-bed-that-sat-idle-expires-at-once) | Florist: compost on an idle bed expires at once | yes | yes |
| [E2E-CIT01-1](#e2e-cit01-1-a-citizen-who-just-finished-a-meal-sometimes-eats-once-more) | A citizen who just finished a meal sometimes eats once more | yes | yes |
| [E2E-FRM-01-2](#e2e-frm-01-2-farmer-skips-a-field-cell-after-every-interruption) | Farmer skips a field cell after every interruption | yes | yes |
| [E2E-FAB-05-1](#e2e-fab-05-1-the-pharao-scepter-shoots-whatever-is-in-the-off-hand-or-the-first-hotbar-slot) | The pharao scepter shoots whatever is in the off hand or the first hotbar slot | yes | yes |
| [E2E-FAB-06-1](#e2e-fab-06-1-dropped-fire-arrows-vanish) | Dropped fire arrows vanish | yes | yes |

### E2E-CIT06-1: Unemployed or sick citizens can be happier than healthy employed ones

**Applies to:** `version/1.21` and `version/main`; since "Happiness rework (#9092)".

**Steps to reproduce**

1. New colony without guards (security factor below 1), two residences, a library.
2. Citizen A: has a home and a job, is healthy. Citizen B: same, but without a job. Citizen C: same as A, but sick.
3. Compare the happiness values in the citizen windows.

**Expected:** B and C (one below-neutral factor more than A) are less happy than A.

**Actual:** A 3.0, B 3.5, C 3.5 (homeless 2.0, a citizen with a varied diet 10.0).

**Root cause:** `CitizenHappinessHandler.getHappiness`
(`src/main/java/com/minecolonies/core/entity/citizen/citizenhandlers/CitizenHappinessHandler.java:148`; version/main `:146`) skips
every factor that is exactly 1.0 and takes the weighted average of the rest. When a factor drops below 1.0 (unemployment 0.5, sickness
0.5) it enters the average; if it is above the current average (security 0.3 without guards), the average goes *up*. The result is not
monotonic in the factors. A citizen whose factors are all 1.0 even gets `0 / 0 = NaN`.

**Suggested patch:** average over all factors, so neutral factors count as neutral (this is how it worked before #9092). This raises the
typical happiness value (neutral factors no longer drop out); if the old scale is wanted, the factor values or the scale in the last
line can be rebalanced, but any average that leaves out the neutral factors keeps the problem.

```diff
--- a/src/main/java/com/minecolonies/core/entity/citizen/citizenhandlers/CitizenHappinessHandler.java
+++ b/src/main/java/com/minecolonies/core/entity/citizen/citizenhandlers/CitizenHappinessHandler.java
@@ -145,8 +145,6 @@ public class CitizenHappinessHandler implements ICitizenHappinessHandler
             for (final IHappinessModifier happinessModifier : happinessFactors.values())
             {
                 final double factor = happinessModifier.getFactor(citizenData);
-                if (factor == 1.0)
-                    continue;
                 total += factor * happinessModifier.getWeight();
                 totalWeight += happinessModifier.getWeight();
             }
```

**E2E test:** `CitizensServicesTests.cit06b_negativeFactorsLowerHappiness`.

---

### E2E-SRV02-1: Library study items make studying less successful

**Applies to:** `version/1.21` and `version/main`; since "Fix library issues ... (#7349)".

**Steps to reproduce**

1. Library with students. Compare the "intelligence levelled" statistic with no study items, with paper, and with books in the hut.
2. Or call `tryLevelUpIntelligence` with the chances `EntityAIStudy` uses: 8 (no item), `8 * 10 / 40` (paper), `8 * 10 / 60` (book).

**Expected:** books (60% "skill increase chance" in the config) give the most gains, no item the fewest. `ONE_IN_X_CHANCE` is documented
as "One in X chance to gain experience".

**Actual:** 4000 rolls each: no item 89%, paper 50%, book 26%. The best configured item is the worst.

**Root cause:** `CitizenSkillHandler.tryLevelUpIntelligence`
(`src/main/java/com/minecolonies/core/entity/citizen/citizenhandlers/CitizenSkillHandler.java:152`; version/main `:153`) returns "no
gain" when `random * customChance < 1`, i.e. with probability `1 / customChance`. So the success chance is `1 - 1/X` instead of `1/X`,
and lowering `X` with a study item lowers the chance. Before #7349 the check was `random.nextInt(customChance) > 0` (fail), success
`1/X`.

**Suggested patch:** (note: this lowers the base gain from 87.5% to 12.5% per study session; study items then raise it as configured,
e.g. book to 75%)

```diff
--- a/src/main/java/com/minecolonies/core/entity/citizen/citizenhandlers/CitizenSkillHandler.java
+++ b/src/main/java/com/minecolonies/core/entity/citizen/citizenhandlers/CitizenSkillHandler.java
@@ -149,7 +149,7 @@ public class CitizenSkillHandler implements ICitizenSkillHandler
     @Override
     public boolean tryLevelUpIntelligence(@NotNull final Random random, final double customChance, @NotNull final ICitizenData citizen)
     {
-        if (customChance > 0 && random.nextDouble() * customChance < 1)
+        if (customChance > 0 && random.nextDouble() * customChance >= 1)
         {
             return false;
         }
```

**E2E test:** `CitizensServicesTests.srv02b_studyItemsHelp`.

---

### E2E-COL-05-1: Friends can't open hut menus

**Applies to:** `version/1.21`. Not on `version/main`: there the container check is `block instanceof BaseEntityBlock`, which huts are
not.

**Steps to reproduce**

1. Colony with colony protection on (default) and any hut.
2. Add a second player with the default "Friend" rank (has `ACCESS_HUTS`, not `OPEN_CONTAINER`).
3. The friend right-clicks the hut.

**Expected:** the hut window opens.

**Actual:** nothing opens; the town hall permission log shows `<friend> OPEN_CONTAINER at <hut>`.

**Root cause:** `ColonyPermissionEventHandler.on(PlayerInteractEvent.RightClickBlock)`
(`src/main/java/com/minecolonies/core/colony/permissions/ColonyPermissionEventHandler.java:371`) lets the hut click through when the
player has `ACCESS_HUTS`, then continues to the generic container check (`:398-411`). Hut block entities expose an item handler
(`MineColonies.java`, `registerBlockEntity(ItemHandler.BLOCK, BUILDING, ...)`), so the hut counts as a container and the click is
refused with `OPEN_CONTAINER`. Huts already have their own permission (`ACCESS_HUTS`, checked above); the hut inventory itself is only
reachable through the hut window.

**Suggested patch:**

```diff
--- a/src/main/java/com/minecolonies/core/colony/permissions/ColonyPermissionEventHandler.java
+++ b/src/main/java/com/minecolonies/core/colony/permissions/ColonyPermissionEventHandler.java
@@ -408,7 +408,7 @@ public class ColonyPermissionEventHandler
                     }
                 }
 
-                if (isContainer && !perms.hasPermission(event.getEntity(), Action.OPEN_CONTAINER))
+                if (isContainer && !(block instanceof AbstractBlockHut) && !perms.hasPermission(event.getEntity(), Action.OPEN_CONTAINER))
                 {
                     cancelEvent(event, event.getEntity(), colony, Action.OPEN_CONTAINER, event.getPos());
                     return;
```

**E2E test:** `ColonyConnectionsRobustnessTests.col05b_friendOpensHut`.

---

### E2E-CON-01-1: A removed connection sign leaves a stale link in the other colony

**Applies to:** `version/1.21` and `version/main`.

**Steps to reproduce**

1. Two colonies A and B with a gate house each. Each owner places a colony sign in front of his gate house.
2. Owner A links his sign to B's sign (sneak-use the sign item on B's sign). The colonies get connected.
3. Owner A breaks his sign, places a new one and links it to B's sign again.

**Expected:** B's sign is linked to A's new sign.

**Actual:** the link is refused ("connection failed"); B's sign still has `nextNode` = the removed sign of A. The sign chain between the
colonies can't be repaired.

**Root cause:** `ColonyConnectionManager.removeConnectionNode`
(`src/main/java/com/minecolonies/core/colony/managers/ColonyConnectionManager.java:169-189`; version/main `:168-188`) resets only the
neighbour nodes stored in the *same* colony. For the sign that links to the other colony, the next node is the partner sign in the other
colony's manager, which keeps pointing at the removed sign. `connectToColony` (`:372`; version/main `:371`) then refuses the partner
sign because `targetNode.hasNextNode()`.

**Suggested patch:**

```diff
--- a/src/main/java/com/minecolonies/core/colony/managers/ColonyConnectionManager.java
+++ b/src/main/java/com/minecolonies/core/colony/managers/ColonyConnectionManager.java
@@ -183,6 +183,20 @@ public class ColonyConnectionManager implements IColonyConnectionManager
                 nextNode.alterPreviousNode(BlockPos.ZERO);
                 MessageUtils.format(Component.translatable(COM_MINECOLONIES_SIGN_DISRUPTED, nextNode.getPosition().toShortString())).sendTo(this.colony).forManagers();
             }
+            else if (colonyConnectionNode.getTargetColonyId() != -1)
+            {
+                // The next node may be the linked sign of the connected colony, unlink it there too.
+                final IColony targetColony = IColonyManager.getInstance().getColonyByDimension(colonyConnectionNode.getTargetColonyId(), colony.getDimension());
+                if (targetColony != null)
+                {
+                    final ColonyConnectionNode targetNode = targetColony.getConnectionManager().getNode(colonyConnectionNode.getNextNode());
+                    if (targetNode != null && targetNode.getNextNode().equals(connectionPoint))
+                    {
+                        targetNode.alterNextNode(BlockPos.ZERO);
+                        targetColony.markDirty();
+                    }
+                }
+            }
         }
         pendingColonyConnections.remove(connectionPoint);
     }
```

**E2E test:** `ColonyConnectionsRobustnessTests.con01b_reconnectAfterSignRemoved`.

---

### E2E-CON-02-1: Ally teleport only works when the player is within 15 blocks of the target gate

**Applies to:** `version/1.21` and `version/main`; introduced by "Packet range (#11888)" (`d66bdaae3a` on version/1.21, `1063d908ee` on
version/main).

**Steps to reproduce**

1. Two connected colonies A and B, allies, gate houses more than 15 blocks apart (the normal case).
2. A player of A opens A's gate house window and presses "teleport" for B.

**Expected:** the player is teleported to B's gate.

**Actual:** nothing happens.

**Root cause:** `TeleportToColonyMessage.onExecute`
(`src/main/java/com/minecolonies/core/network/messages/server/colony/TeleportToColonyMessage.java:89`; version/main `:73`) checks
`isInRange(player, pos)`, where `pos` is the *target* colony's gate. The message is sent from the player's *own* gate house window
(`ConnectionModuleWindow.teleportToColony`), so the player stands at his own gate and the check fails unless the two gates are at most
15 blocks apart.

**Suggested patch:** keep the range restriction of #11888, but measure it to the gate houses of the origin colony, where the window was
opened. A player far from any of his colony's gate houses is still refused.

```diff
--- a/src/main/java/com/minecolonies/core/network/messages/server/colony/TeleportToColonyMessage.java
+++ b/src/main/java/com/minecolonies/core/network/messages/server/colony/TeleportToColonyMessage.java
@@ -86,7 +86,7 @@ public class TeleportToColonyMessage extends AbstractColonyServerMessage impleme
     @Override
     protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, final IColony colony)
     {
-        if (player == null || !isInRange(player.blockPosition(), pos))
+        if (player == null)
         {
             return;
         }
@@ -97,6 +97,13 @@ public class TeleportToColonyMessage extends AbstractColonyServerMessage impleme
             return;
         }
 
+        // The teleport is started from a gate house window of the origin colony, so the player has to be near one of its gate houses.
+        if (originColony.getServerBuildingManager().getBuildings().values().stream()
+              .noneMatch(building -> building instanceof BuildingGateHouse && isInRange(player.blockPosition(), building.getPosition())))
+        {
+            return;
+        }
+
         if (originColony.getConnectionManager().getColonyDiplomacyStatus(colony.getID()) != DiplomacyStatus.ALLIES)
         {
             return;
```

(On version/main use `ctxIn.getSender()` for `player`.)

**E2E test:** `ColonyConnectionsRobustnessTests.con02b_allyTeleportFromOwnGate`.

---

### E2E-VIS04-1: Cancelling an accepted quest at a delivery step does nothing

**Applies to:** `version/1.21` and `version/main`.

**Steps to reproduce**

1. Accept a quest with a delivery objective (e.g. "hungry courier"), without having the requested item.
2. Talk to the quest giver again and pick the "cancel" answer of the delivery dialogue.

**Expected:** the quest is cancelled (removed from the active quests, no reward), like cancelling in a normal dialogue.

**Actual:** the dialogue closes, the quest stays in progress.

**Root cause:** `QuestDeliveryInteraction.onServerResponseTriggered`
(`src/main/java/com/minecolonies/core/colony/interactionhandling/QuestDeliveryInteraction.java:65-72`; version/main `:66-73`): for a
final answer that is not positive (`QuestCancellationDialogueAnswer`), the else branch only resets `currentElement` and returns;
`applyToQuest` (which calls `quest.onDeletion()`) is never called. `QuestDialogueInteraction.onServerResponseTriggered` does call it.

**Suggested patch:**

```diff
--- a/src/main/java/com/minecolonies/core/colony/interactionhandling/QuestDeliveryInteraction.java
+++ b/src/main/java/com/minecolonies/core/colony/interactionhandling/QuestDeliveryInteraction.java
@@ -66,6 +66,8 @@ public class QuestDeliveryInteraction extends QuestDialogueInteraction
                 {
                     if (!(result instanceof IQuestDialogueAnswer.CloseUIDialogueAnswer))
                     {
+                        ((IFinalQuestDialogueAnswer) result).applyToQuest(player, colonyQuest);
+                        finished = true;
                         currentElement = null;
                         data.getColony().markDirty();
                         return;
```

**E2E test:** `DefenseVisitorsTests.vis04_cancelAcceptedQuestAtDelivery`.

---

### E2E-VIS02-1: Removing a tavern right after recruiting a visitor throws and leaves visitors behind

**Applies to:** `version/1.21` and `version/main`.

**Steps to reproduce**

1. Tavern with two visitors. Recruit one of them.
2. Within the next colony tick (about 25 s), pick up / destroy the tavern (or delete the colony).

**Expected:** the tavern is removed, its remaining visitor is removed too.

**Actual:** `NullPointerException: Cannot invoke "ICivilianData.getId()" because "citizen" is null at
VisitorManager.removeCivilian(VisitorManager.java:301) at TavernBuildingModule.onDestroyed(TavernBuildingModule.java:312)`; the loop
stops, the other visitor stays registered. On colony deletion: `WARN Something went wrong deleting a building while deleting the
colony!`.

**Root cause:** after a recruit the visitor is removed from the visitor manager, but `TavernBuildingModule.externalCitizens` keeps its
id until the tavern's next `onColonyTick`. `TavernBuildingModule.onDestroyed`
(`src/main/java/com/minecolonies/core/colony/buildings/modules/TavernBuildingModule.java:310-313`; version/main `:309-312`) passes
`getVisitor(id)` = `null` to `VisitorManager.removeCivilian` (`VisitorManager.java:301`; version/main `:307`), which dereferences it.

**Suggested patch:**

```diff
--- a/src/main/java/com/minecolonies/core/colony/buildings/modules/TavernBuildingModule.java
+++ b/src/main/java/com/minecolonies/core/colony/buildings/modules/TavernBuildingModule.java
@@ -309,7 +309,12 @@ public class TavernBuildingModule extends AbstractBuildingModule implements IDef
     {
         for (final Integer id : externalCitizens)
         {
-            building.getColony().getVisitorManager().removeCivilian(building.getColony().getVisitorManager().getVisitor(id));
+            // A visitor that was just recruited or removed is no longer known to the visitor manager.
+            final IVisitorData visitor = building.getColony().getVisitorManager().getVisitor(id);
+            if (visitor != null)
+            {
+                building.getColony().getVisitorManager().removeCivilian(visitor);
+            }
         }
     }
```

**E2E test:** `DefenseVisitorsTests.vis02_tavernRemovedAfterRecruiting`.

---

### E2E-DEF-S1: Raid end uses the colony's first raid instead of the one that just ended

**Applies to:** `version/1.21` and `version/main`.

**Steps to reproduce**

1. A colony loses more than half of its citizens in its first raid.
2. A later raid ends without losses.

**Expected:** a normal "raid ended" message, no mercy delay; the victory sound depends on the size of this raid.

**Actual:** the mercy message is shown and the next raid is delayed by `2 * averageNumberOfNightsBetweenRaids` nights after *every*
later raid (and never if the first raid had few losses); the victory sound always depends on the first raid's size.

**Root cause:** `RaidManager.onRaidEventFinished`
(`src/main/java/com/minecolonies/core/colony/events/raid/RaidManager.java:1061` and `:1078`; version/main `:1058` and `:1075`) reads
`raidHistories.get(0)`, the oldest entry. Raids are appended (`raidHistories.add(...)` in `raiderEvent`), and everything else uses the
last entry (`getLastRaid()`, `getLostCitizen()`).

**Suggested patch:**

```diff
--- a/src/main/java/com/minecolonies/core/colony/events/raid/RaidManager.java
+++ b/src/main/java/com/minecolonies/core/colony/events/raid/RaidManager.java
@@ -1058,7 +1058,8 @@ public class RaidManager implements IRaiderManager
             }
         }
 
-        if (((double) raidHistories.get(0).lostCitizens / colony.getCitizenManager().getMaxCitizens()) > 0.5)
+        final RaidHistory lastRaid = getLastRaid();
+        if (lastRaid != null && ((double) lastRaid.lostCitizens / colony.getCitizenManager().getMaxCitizens()) > 0.5)
         {
             MessageUtils.format(RAID_END_MERCY, colony.getName()).sendTo(colony).forManagers();
             extraDaysToNextRaid = MineColonies.getConfig().getServer().averageNumberOfNightsBetweenRaids.get() * 2;
@@ -1075,7 +1076,7 @@ public class RaidManager implements IRaiderManager
             MessageUtils.format(msgID, colony.getName()).sendTo(colony).forManagers();
         }
 
-        PlayAudioMessage audio = new PlayAudioMessage(raidHistories.get(0).raiderAmount <= SMALL_HORDE_SIZE ? RaidSounds.VICTORY_EARLY : RaidSounds.VICTORY, SoundSource.HOSTILE);
+        PlayAudioMessage audio = new PlayAudioMessage(lastRaid != null && lastRaid.raiderAmount <= SMALL_HORDE_SIZE ? RaidSounds.VICTORY_EARLY : RaidSounds.VICTORY, SoundSource.HOSTILE);
         PlayAudioMessage.sendToAll(colony, false, true, audio);
 
         if (colony.getRaiderManager().getLostCitizen() == 0)
```

**E2E test:** `DefenseVisitorsTests.defs1_raidEndUsesLastRaid`.

---

### E2E-CRF01-1: Crusher crafts a request again before it is closed (wastes ingredients)

**Applies to:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`). Intermittent.

**Steps to reproduce**
1. Build a crusher (level 1) with a worker; set its daily limit to 0 so it only works on requests.
2. Put 8 cobblestone into the crusher's hut and request 2 gravel from another hut (e.g. a builder needing 2 gravel; recipe: 2 cobblestone
   → 1 gravel).
3. Watch the crusher's inventory and the hut.

**Expected:** 4 cobblestone are used, 2 gravel are delivered, the request is done.

**Actual:** often (about 2 of 3 tries) all 8 cobblestone are used: the requester gets 2 gravel and 2 more stay with the crusher. Sometimes the
requester gets no gravel at all while the crusher holds it.

**Root cause:** `EntityAIWorkCrusher.craft()` (`core/entity/ai/workers/crafting/EntityAIWorkCrusher.java:223-236`; `version/main`
226-239). When the crafting
count is reached it resets the counters (`resetValues()`) and only finishes the request if `inventoryNeedsDump()` happens to be true;
otherwise it stays in `CRAFT` with the request still open. The next `decide()` → `GET_RECIPE` sees the request again (the gravel already in
the inventory counts as done, but `crush()` crafts whenever the progress is full, regardless of the counter) and crafts more; in `CRAFT`
the daily limit check (line 120) is skipped. `AbstractEntityAICrafting` does it differently: after the last craft it returns
`finalizeCraftingTask()` → `INVENTORY_FULL`, and `afterDump()` closes the request.

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/crafting/EntityAIWorkCrusher.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/crafting/EntityAIWorkCrusher.java
@@ craft()
             if (job.getCraftCounter() >= job.getMaxCraftingCount())
             {
                 incrementActionsDone(getActionRewardForCraftingSuccess());
-                currentRecipeStorage = null;
                 worker.decreaseSaturationForAction();
-                resetValues();
-                if (inventoryNeedsDump())
-                {
-                    if (job.getMaxCraftingCount() == 0 && job.getProgress() == 0 && job.getCraftCounter() == 0 && currentRequest != null)
-                    {
-                        job.finishRequest(true);
-                    }
-                }
+                // Dump now: afterDump() finishes the request. Staying in CRAFT with the request still open would craft it again.
+                return finalizeCraftingTask();
             }
```

**E2E test:** `LogisticsCraftingTests.crf01_crusher`.

---

### E2E-CRF07-1: Enchanter drains himself instead of the workers of his stations

**Applies to:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`).

**Steps to reproduce**
1. Build an enchanter's tower (level 1) with an enchanter, and a lumberjack's hut with a worker.
2. In the enchanter's tower, add the lumberjack's hut as a station to drain.
3. Give the enchanter a book; watch whom he drains (particles go from the drained citizen to the enchanter).

**Expected:** the enchanter walks to the lumberjack's hut and drains the lumberjack (who gets the "random enchant" item reward).

**Actual:** the enchanter walks to the station and drains himself: the particle stream starts and ends at him, the enchant lands in his
own inventory, and the station's worker is never touched.

**Root cause:** `EntityAIWorkEnchanter.gatherAndDrain()` (`core/entity/ai/workers/service/EntityAIWorkEnchanter.java:324`; `version/main` 329) picks the
citizen from `getModuleForJob().getAssignedEntities()`. `getModuleForJob()` is the enchanter's own work module (`job.getWorkModule()`), so
the only candidate is the enchanter. It should pick from the workers of `buildingWorker`, the station it just walked to.

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/service/EntityAIWorkEnchanter.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/service/EntityAIWorkEnchanter.java
@@
 import com.minecolonies.api.colony.buildings.IBuilding;
+import com.minecolonies.api.colony.buildings.modules.IAssignsJob;
@@ gatherAndDrain()
             final List<AbstractEntityCitizen> workers = new ArrayList<>();
-            for (final Optional<AbstractEntityCitizen> citizen : getModuleForJob().getAssignedEntities())
+            for (final IAssignsJob module : buildingWorker.getModulesByType(IAssignsJob.class))
             {
-                citizen.ifPresent(workers::add);
+                for (final Optional<AbstractEntityCitizen> citizen : module.getAssignedEntities())
+                {
+                    citizen.ifPresent(workers::add);
+                }
             }
```

**E2E test:** `LogisticsCraftingTests.crf07_enchanterDrainsStationWorker`.

---

### E2E-GAT-03-1: Miner falls off the shaft ladder when he stops on it

**Applies to:** `version/1.21` (`d66bdaae3a`) and `version/main` (`b2fb29ec11`); the `MovementHandler` JUMPING branch is the same in
both.

**Steps to reproduce**

1. Build a mine (miner's hut level 1) on solid stone and give the miner a pickaxe, a shovel, cobblestone and ladders.
2. Let him dig the shaft. Watch him on the shaft ladder, or watch his health: the moment that matters is when he reaches the spot he
   mines from while he is still on the ladder (`checkMiningLocation` stops his walk there).

**Expected:** he stays on the ladder (or slides down it) and mines; no fall damage.

**Actual:** he keeps "walking" forward on the ladder, slowly drifts sideways off it and falls 3-4 blocks (fall damage each time). In the
Fabric port's test it happened in most full test runs (after 7,500-11,500 ticks of shaft digging) and in 2 of 10 single runs.

**Root cause:** `MovementHandler.tick()`
(`src/main/java/com/minecolonies/core/entity/pathfinding/navigation/MovementHandler.java:138-150`; `version/main` `:144-156`). Going up
the ladder the move control is in `Operation.JUMPING`: its jump condition (lines 127-131) has two halves, and the `!isLadder` test
only applies to the second one, so "target higher than the step height and horizontally close" starts JUMPING on a ladder too. The JUMPING branch calls `mob.setSpeed(...)` every tick, which also sets the forward
input `zza`, and it ends only when the mob is on the ground or in a liquid, which a citizen on a ladder never is. When
`AbstractEntityAIBasic.walkWithProxy` (called from `checkMiningLocation`) stops the navigation while he is on the ladder, the stale
JUMPING state keeps pushing him forward in his current facing, which `WorkerUtil.faceBlock` has turned towards the block he mines (e.g.
yaw 77.9 instead of 90 into the ladder). The push into the ladder keeps him climbing; the small sideways part moves him about 0.013
blocks per tick along the ladder until he leaves the ladder block, then he falls. Traced per tick in the port: path gone at y 5.48,
operation JUMPING, `zza` 0.3, then 35 ticks of climbing and drifting, then the fall.

**Suggested patch:** end JUMPING when the navigation has no path any more. Only the navigation uses this move control, so "navigation
done" always means "nothing to walk to"; the citizen then slides down the ladder slowly (vanilla `handleOnClimbable`, no fall damage).

```diff
--- a/src/main/java/com/minecolonies/core/entity/pathfinding/navigation/MovementHandler.java
+++ b/src/main/java/com/minecolonies/core/entity/pathfinding/navigation/MovementHandler.java
@@ -138,6 +138,18 @@ public class MovementHandler extends MoveControl
         else if (this.operation == net.minecraft.world.entity.ai.control.MoveControl.Operation.JUMPING)
         {
             jumpingticks++;
+
+            // The path was finished or stopped (e.g. a worker reached the spot to mine from) while jumping or climbing: nothing to move to
+            // any more, so stop walking. JUMPING only ends on the ground, which an entity on a ladder never reaches; it would keep walking
+            // forward in its last facing (often turned towards the block it works on), climb on by colliding with the ladder and drift
+            // sideways off it, then fall.
+            if (this.mob.getNavigation().isDone())
+            {
+                this.operation = net.minecraft.world.entity.ai.control.MoveControl.Operation.WAIT;
+                this.mob.setZza(0.0F);
+                return;
+            }
+
             this.mob.setSpeed((float) (this.speedModifier * speedValue));
 
             // Avoid beeing stuck in jumping while in liquids
```

**E2E test:** `FarmingGatheringTests.gat03_minerDigsShaftSafely` (failed in most full runs without the patch, passes with it).

---

### E2E-GAT-04-1: Miner never seals water or lava next to a mine node

**Applies to:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`).

**Steps to reproduce**
1. Have a miner with a finished first mining level and cobblestone in the hut.
2. Next to the open end of the next node the miner will build (within 4 blocks of the node), place a water (or lava) source, as a flooded
   cave would be.
3. Let the miner build that node.

**Expected:** before building the node the miner replaces liquid sources around it with his fill block (the code for this exists:
"Check for liquids").

**Actual:** the source stays; water flows into the tunnels (lava can burn the miner).

**Root cause:** `EntityAIStructureMiner.executeStructurePlacement` (`core/entity/ai/workers/production/EntityAIStructureMiner.java:790-818`;
`version/main` 788-816).
The liquid check comes after the "preload structures" block. On the first call the blueprint is not loaded, so the method starts loading it
and returns `LOAD_STRUCTURE` (line 798). After loading, `afterStructureLoading()` goes to `BUILDING_STEP` and the AI does not come back
to this method before the node is done (`executeSpecificCompleteActions` clears the blueprint), so the check never runs.

**Suggested patch** (`version/1.21`; on `version/main` `initStructure` has an extra `rotation` argument): run the check before loading
the blueprint.
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/production/EntityAIStructureMiner.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/production/EntityAIStructureMiner.java
@@ executeStructurePlacement(...)
         mineNode.setStatus(MineNode.NodeStatus.IN_PROGRESS);
         building.markDirty();
-        //Preload structures
-        if (building.getWorkOrder() == null || building.getWorkOrder().getBlueprint() == null)
-        {
-            initStructure(mineNode,
-              new BlockPos(mineNode.getX(), building.getFirstModuleOccurance(MinerLevelManagementModule.class).getCurrentLevel().getDepth(), mineNode.getZ()),
-              building,
-              world,
-              job);
-            return LOAD_STRUCTURE;
-        }
-
         //Check for liquids
         for (int x = -NODE_DISTANCE / 2 - 1; x <= NODE_DISTANCE / 2 + 1; x++)
         {
             ...
         }
 
+        //Preload structures (after the liquid check: once loaded, the AI builds the node and doesn't come back here)
+        if (building.getWorkOrder() == null || building.getWorkOrder().getBlueprint() == null)
+        {
+            initStructure(mineNode,
+              new BlockPos(mineNode.getX(), building.getFirstModuleOccurance(MinerLevelManagementModule.class).getCurrentLevel().getDepth(), mineNode.getZ()),
+              building,
+              world,
+              job);
+            return LOAD_STRUCTURE;
+        }
+
         workingNode = null;
```

**E2E test:** `FarmingGatheringTests.gat04_minerSealsWater`.

---

### E2E-GAT-06-1: Nether worker stands at the portal for up to 3 days after an expedition

**Applies to:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`).

**Steps to reproduce**
1. Build a nether mine (level 1) with a worker, research "nether expedition log", give the hut the trip's inputs, tools, armor and food.
2. Let the worker go through the portal and wait the travel time.
3. Watch the worker after he reappears at the portal, and the hut's expedition log.

**Expected:** the worker works off the trip (fights, finds), walks home and puts the loot into the hut; the log says "completed".

**Actual:** he stands idle at the portal. The log stays "in progress" and the loot stays in the job until the hut allows the next trip (one
trip per 3-day period, reset in the morning), i.e. up to 3 colony days.

**Root cause:** the respawned worker's AI starts in `IDLE`, which `AbstractEntityAICrafting` only leaves when `hasWorkToDo()`
(`core/entity/ai/workers/crafting/AbstractEntityAICrafting.java:162`; `version/main` 164). `EntityAIWorkNether.hasWorkToDo()`
(`core/entity/ai/workers/production/EntityAIWorkNether.java:156`; `version/main` 149) is "crafting task or `building.isReadyForTrip()`"; the trip was just
recorded, so it is false and `decide()` never reaches its `job.isInNether()` → `NETHER_AWAY` branch.

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/production/EntityAIWorkNether.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/production/EntityAIWorkNether.java
@@ hasWorkToDo()
-        return super.hasWorkToDo() || building.isReadyForTrip();
+        // A worker back from a trip still has its results to work off (decide() -> NETHER_AWAY), even if the next trip isn't allowed yet.
+        return super.hasWorkToDo() || building.isReadyForTrip() || job.isInNether();
```

**E2E test:** `FarmingGatheringTests.gat06_netherWorkerReturnsAfterTravel`.

---

### E2E-FRM-06-1: Herder (and beekeeper) feeds animals that are too far apart to mate

**Applies to:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`).

**Steps to reproduce**
1. Build a chicken herder's hut with a worker and wheat seeds.
2. In its area, put two chickens 9 blocks apart that can't reach each other quickly (e.g. separated by a fence, or slowed).

**Expected:** the herder only feeds a pair that can find each other; vanilla's `BreedGoal` looks for a partner within 8 blocks.

**Actual:** the herder feeds both; they are in love but never look for each other (more than 8 blocks), the love runs out after 30 s, the
food is wasted and the herder waits 60 s (`breedTimeOut`) before the next try. In a normal pen this shows as breeding that sometimes takes
much longer than it should.

**Root cause:** `AbstractEntityAIHerder.DISTANCE_TO_BREED = 10`
(`core/entity/ai/workers/production/herders/AbstractEntityAIHerder.java:78`, used at line 454; `version/main` 76/452) is larger than vanilla's mate search range
(`BreedGoal.PARTNER_TARGETING` range 8, box inflated by 8). `EntityAIWorkBeekeeper` has the same constant (line 79, used at 287; bees use
`BreedGoal` too).

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/production/herders/AbstractEntityAIHerder.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/production/herders/AbstractEntityAIHerder.java
     /**
-     * Distance two animals need to be inside to breed.
+     * Distance two animals need to be inside to breed. Vanilla BreedGoal only looks for a partner within 8 blocks.
      */
-    private static final int DISTANCE_TO_BREED = 10;
+    private static final int DISTANCE_TO_BREED = 8;
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/production/agriculture/EntityAIWorkBeekeeper.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/production/agriculture/EntityAIWorkBeekeeper.java
     /**
-     * Distance two animals need to be inside to breed.
+     * Distance two animals need to be inside to breed. Vanilla BreedGoal only looks for a partner within 8 blocks.
      */
-    private static final int DISTANCE_TO_BREED = 10;
+    private static final int DISTANCE_TO_BREED = 8;
```

**E2E test:** `FarmingGatheringTests.frm06_herderFeedsPairOutOfMateRange` (herder only; the beekeeper change is untested).

---

### E2E-FRM-03-1: Florist: compost on a bed that sat idle expires at once

**Applies to:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`), same line numbers.

**Steps to reproduce**
1. Build a flower shop with a florist and several plant beds; give the florist one compost per bed, then let the beds sit without compost
   for more than 5 minutes (e.g. the florist ran out of compost).
2. Give the florist compost again and watch the first bed he composts.

**Expected:** the compost lasts 300 s (`TICKER_LIMIT`) and flowers grow on the bed during that time.

**Actual:** the bed loses its compost at its first update (within 1 s); the compost is wasted. The florist always composts the first
non-composted bed, so he composts the same bed twice before moving on.

**Root cause:** `TileEntityCompostedDirt.tick()` (`core/tileentities/TileEntityCompostedDirt.java:75`) increments `ticker` on every tick,
also while the bed is not composted. The reset (`ticker >= TICKER_LIMIT * TICKS_SECOND` → `composted = false`, line 122) runs in
`updateTick` only while composted, so after a long idle time the first update ends the compost. `compost()` (line 158) doesn't reset the
timer.

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/tileentities/TileEntityCompostedDirt.java
+++ b/src/main/java/com/minecolonies/core/tileentities/TileEntityCompostedDirt.java
@@ compost(...)
         }
+        // The ticker also counts while not composted; restart it so the new compost lasts the full TICKER_LIMIT.
+        this.ticker = 0;
         this.composted = true;
     }
```

**E2E test:** `FarmingGatheringTests.frm03_compostLastsOnIdleBed`.

---

### E2E-CIT01-1: A citizen who just finished a meal sometimes eats once more

**Applies to:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`), line numbers below.

**Steps to reproduce**
1. A hungry citizen (saturation about 1) with 16 bread in his inventory, no restaurant.
2. Let him eat; note the bread count when he stops (saturation 60), then watch for a few seconds.

**Expected:** he eats until saturation 60 and then stops; the bread count stays.

**Actual:** in about 1 of 5 meals he starts eating again within half a second of finishing and eats one more bread at saturation
59.7 (the extra food is wasted; saturation is capped at 60).

**Root cause:** when the meal ends, `EntityAIEatTask.eat` sets `justAte` and returns `CitizenAIState.IDLE`
(`core/entity/ai/minimal/EntityAIEatTask.java:244-245`; `version/main` 241-242). `CitizenAI.lastState` stays `EATING` until the next
`decideAiTask` (an event target, every 10 ticks). `CitizenData.decreaseSaturation` clears `justAte` on every saturation drop
(`core/colony/CitizenData.java:1182`; `version/main` 1169). If a drop lands in that window, `CitizenAI.shouldEat`
(`core/entity/ai/workers/CitizenAI.java:281`; `version/main` 280) sees `lastState == EATING` and returns true although the meal is over,
and `decideAiTask` (line 118) starts a new meal because the state is `IDLE`.

**Suggested patch:** keep a meal going only while it really is going on.
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/CitizenAI.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/CitizenAI.java
@@ public boolean shouldEat()
-        if (lastState == CitizenAIState.EATING)
+        if (lastState == CitizenAIState.EATING && citizen.getCitizenAI().getState() != CitizenAIState.IDLE)
         {
             return true;
         }
```

**E2E test:** `CitizensServicesTests.cit01_eatingWhenHungry` (before the fix it failed in 4 of 20 parallel repeats).

---

### E2E-FRM-01-2: Farmer skips a field cell after every interruption

**Applies to:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`), line numbers below.

**Steps to reproduce**
1. A farm with a farmer and a large untilled field (e.g. radius 3, 48 cells).
2. While the farmer tills the field, interrupt him once (he eats, goes on a leisure break, or it starts raining), then let him finish.

**Expected:** after the interruption he goes on with the cell he was about to till.

**Actual:** that cell stays untilled (and unplanted) until the field's next cycle, a colony day or more later. The same happens to a ripe
crop during harvesting.

**Root cause:** after an interruption the farmer's AI starts again at `prepareForFarming`, which calls `checkIfShouldExecute` for the
harvest and the hoe stage (`core/entity/ai/workers/production/agriculture/EntityAIWorkFarmer.java:261, 269`; `version/main` 263, 271).
`checkIfShouldExecute` (line 311; `version/main` 313) advances the working offset (`nextValidCell`, line 316; `version/main` 318) before it
tests a cell, so the cell at the saved offset, which wasn't worked yet, is never tested.

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/production/agriculture/EntityAIWorkFarmer.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/production/agriculture/EntityAIWorkFarmer.java
@@ private boolean checkIfShouldExecute(...)
     {
         BlockPos position;
+        // An interrupted pass resumes at the cell it hadn't worked yet.
+        if (building.getWorkingOffset() != null)
+        {
+            position = farmField.getPosition().below().south(building.getWorkingOffset().getZ()).east(building.getWorkingOffset().getX());
+            if (predicate.test(position))
+            {
+                return true;
+            }
+        }
         do
         {
             building.setWorkingOffset(nextValidCell(farmField));
```

**E2E test:** `FarmingGatheringTests.frm01_farmerTillsOtherSoils` (the test's short leisure breaks interrupt the farmer; before the fix it
failed in 3 of 60 parallel repeats, each time with one cell left).

---

### E2E-FAB-05-1: The pharao scepter shoots whatever is in the off hand or the first hotbar slot

**Applies to:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`).

**Steps to reproduce**
1. Survival player with the pharao scepter in the second hotbar slot, arrows somewhere in the inventory, a torch in the first hotbar slot.
2. Draw and release the scepter. Then empty the first hotbar slot and try again. Then put the scepter itself in the first hotbar slot.

**Expected:** each shot uses one arrow from the inventory (like a bow) and fires a fire arrow.

**Actual:** the first shot uses up the torch; with an empty first slot nothing is shot at all, although there are arrows; with the scepter
in the first slot it uses itself as ammo (in the Fabric port this made the shot throw `IllegalArgumentException: Invalid weapon firing an
arrow` on the server). An item in the off hand is always taken first.

**Root cause:** `ItemPharaoScepter.getAllSupportedProjectiles` returns `itemStack -> true`
(`core/items/ItemPharaoScepter.java:96-98`; `version/main` 128-130). Vanilla's `Player#getProjectile` takes the off hand, then the main
hand, then the first inventory slot whose stack matches that predicate, so it never gets past the first slot (an empty stack matches too).

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/items/ItemPharaoScepter.java
+++ b/src/main/java/com/minecolonies/core/items/ItemPharaoScepter.java
     public Predicate<ItemStack> getAllSupportedProjectiles()
     {
-        return itemStack -> true;
+        return itemStack -> itemStack.getItem() instanceof ArrowItem;
     }
```

**E2E test:** `TransferBridgeGameTests.pharaoScepterShootsFireArrows` (FAB-05).

---

### E2E-FAB-06-1: Dropped fire arrows vanish

**Applies to:** `version/1.21` (`d66bdaae3a`, NeoForge); also `version/main` (`b2fb29ec11`, Forge).

**Steps to reproduce**
1. Get fire arrows (e.g. from the pharao's loot) and drop them with Q, or break a chest that holds some.

**Expected:** the fire arrows lie on the ground as an item and can be picked up.

**Actual:** the item disappears at once; a fire arrow entity appears at (0, 0, 0) of the dimension instead.

**Root cause:** `ItemFireArrow` returns true from `hasCustomEntity` (`core/items/ItemFireArrow.java:29`; `version/main` 30), so the
loader's entity join handler (NeoForge `NeoForgeEventHandler#onEntityJoinWorld`, Forge `ForgeInternalHandler`) replaces every item entity
holding it with `createEntity`'s result and discards the item. `createEntity` (line 44; `version/main` 46) returns a new fire arrow
entity without copying the position, the motion or the stack: `ModEntities.FIREARROW.create(world)`.

**Suggested patch:** fire arrows need no custom item entity; shooting already creates the fire arrow through `createArrow`.
```diff
--- a/src/main/java/com/minecolonies/core/items/ItemFireArrow.java
+++ b/src/main/java/com/minecolonies/core/items/ItemFireArrow.java
-    @Override
-    public boolean hasCustomEntity(final ItemStack stack)
-    {
-        return true;
-    }
-
@@
-    @Nullable
-    @Override
-    public Entity createEntity(final Level world, final Entity location, final ItemStack itemstack)
-    {
-        return ModEntities.FIREARROW.create(world);
-    }
```

**E2E test:** `TransferBridgeGameTests.droppedFireArrowStaysAnItem` (FAB-06; the Fabric port doesn't have the hook at all, so the test
guards against porting it back).

---

### Visual quirks (not filed as bugs)

Seen while testing the spear's renderer (UI-10); the port keeps the 1.21.1 look, so they are noted here for the maintainers only.

- **The spear is drawn with the vanilla trident mesh.** `SpearModel#createLayer` (`core/client/model/SpearModel.java:31`) defines the
  spear's own mesh, but it is never registered as a model layer; the item renderer and the thrown spear bake `ModelLayers.TRIDENT` instead
  (`core/client/render/SpearItemTileEntityRenderer.java:30`, `core/client/render/projectile/RendererSpear.java:37`).
- **In an item frame the spear sits mostly inside the wall.** The `fixed` transform of `models/item/spear_in_hand.json` (lines 34-37,
  translation `[-7.75, -6.5, -8.5]`) pushes the two-block model into the block behind the frame; only the tip shows below the frame.

---

## Structurize

This one is in Structurize, not MineColonies; file it with Structurize. It shows up in MineColonies' build windows and builders.

| ID | Title | Structurize `version/1.21` |
|---|---|---|
| [E2E-BLD-07-1](#e2e-bld-07-1-material-list-and-builders-request-list-misses-blueprint-entities-that-share-a-block-structurize) | Material list misses blueprint entities that share a block | yes |

### E2E-BLD-07-1: Material list (and builder's request list) misses blueprint entities that share a block (Structurize)

**Where to file:** Structurize (the counting code is there); affects MineColonies' build/decoration windows and builders.
**Applies to:** Structurize `version/1.21` at `9ad76cfd3a` (vendored by the port; MineColonies `version/1.21` builds against
Structurize `1.0.837-1.21.1-snapshot`). In MineColonies `version/1.21`, `WindowBuildBuilding#updateResources` (line 335),
`WindowBuildDecoration#updateResources` (line 232) and the builder's `ENTITIES` request stage
(`AbstractEntityAIStructureWithWorkOrder.java:313`) all use this `StructurePlacer` `GET_RES_REQUIREMENTS` count. The 1.20.1 Structurize
used by `version/main` (`1.20.1-1.0.825`) was not checked.

**Steps to reproduce**
1. Open the build tool and choose `decorations/statues/pedastal_oak` of the default style (it has 4 entities; item frames holding items).
2. Look at the material list in the decoration window: 3 item frames, 6 torches.
3. Give a builder exactly that list and order the decoration.

**Expected:** the list contains every item the build needs; the builder finishes.

**Actual:** the list is one item frame and one torch short; the builder stops at the end (`NEEDS_ITEM`, "1x torch, 1x item_frame") and
requests them one by one.

**Root cause:** `StructurePlacer.handleEntitySpawn(..., simulate = true)` (`placement/StructurePlacer.java:346-437`, the `simulate` branch at
line 407) returns `MISSING_ITEMS` with the first entity's items. Entities are grouped by block position, so a second entity on the same
block (e.g. two item frames on different faces of one block) is never counted; for `pedastal_oak` that is one item frame with a torch. `getResourceRequirements` (line 522) uses only that result.

**Suggested patch** (Structurize)
```diff
--- a/src/main/java/com/ldtteam/structurize/placement/StructurePlacer.java
+++ b/src/main/java/com/ldtteam/structurize/placement/StructurePlacer.java
@@ public BlockPlacementResult handleEntitySpawn(...)
     {
+        // Several entities can share a block position (e.g. two item frames on different faces): collect the items of all of them.
+        final List<ItemStack> simulatedItems = new ArrayList<>();
         for (final CompoundTag compound : this.iterator.getBluePrintPositionInfo(localPos).getEntities())
@@
-                                if (simulate || !this.handler.hasRequiredItems(requiredItems))
+                                if (simulate)
+                                {
+                                    simulatedItems.addAll(requiredItems);
+                                    continue;
+                                }
+
+                                if (!this.handler.hasRequiredItems(requiredItems))
                                 {
                                     return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.MISSING_ITEMS, requiredItems);
                                 }
@@
         }
 
+        if (!simulatedItems.isEmpty())
+        {
+            return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.MISSING_ITEMS, simulatedItems);
+        }
         return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.SUCCESS);
```

**E2E test:** `ConstructionResearchTests.bld07_decorationWithEntitiesMaterialList`.
