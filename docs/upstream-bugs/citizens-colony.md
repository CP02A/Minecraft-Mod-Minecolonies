# Upstream bug reports: citizens, colony, connections, visitors, raids

Ready-to-file MineColonies issues for bugs that the Fabric port's end-to-end tests found in shared MineColonies code. Each is fixed in
the Fabric port; the suggested patches are the same changes, written against the upstream sources.

Branches checked:

- **`version/1.21`** (NeoForge 1.21.1, head `d66bdaae3a` "Packet range (#11888)"): the branch the Fabric port is based on. All line
  numbers below are for this branch unless noted.
- **`version/main`** (Forge 1.20.1, head `b2fb29ec11` "Hotfix"): line numbers given separately where the bug exists there too.

| ID | Title | version/1.21 | version/main (1.20.1) |
|---|---|---|---|
| E2E-CIT06-1 | Negative happiness factors can raise total happiness | yes | yes |
| E2E-SRV02-1 | Library study items make studying less successful | yes | yes |
| E2E-COL-05-1 | Friends can't open hut menus | yes | no (different container check) |
| E2E-CON-01-1 | Removed connection sign leaves a stale link in the other colony | yes | yes |
| E2E-CON-02-1 | Ally teleport only works within 15 blocks of the target gate | yes | yes |
| E2E-VIS04-1 | Cancelling an accepted quest at a delivery step does nothing | yes | yes |
| E2E-VIS02-1 | Removing a tavern right after a recruit throws | yes | yes |
| E2E-DEF-S1 | Raid end reads the first raid instead of the last | yes | yes |

---

## E2E-CIT06-1: Unemployed or sick citizens can be happier than healthy employed ones

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

---

## E2E-SRV02-1: Library study items make studying less successful

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

---

## E2E-COL-05-1: Friends can't open hut menus

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

---

## E2E-CON-01-1: A removed connection sign leaves a stale link in the other colony

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

---

## E2E-CON-02-1: Ally teleport only works when the player is within 15 blocks of the target gate

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

---

## E2E-VIS04-1: Cancelling an accepted quest at a delivery step does nothing

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

---

## E2E-VIS02-1: Removing a tavern right after recruiting a visitor throws and leaves visitors behind

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

---

## E2E-DEF-S1: Raid end uses the colony's first raid instead of the one that just ended

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
