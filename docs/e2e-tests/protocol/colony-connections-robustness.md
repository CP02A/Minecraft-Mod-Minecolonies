[← Back to the test overview](../../E2E-tests.md)

# Protocol: colony, connections, robustness

Suite class `ColonyConnectionsRobustnessTests` (filter `colony_connections_robustness_tests`). Final run (after the bug fixes): all 18 tests passed and are required;
the three bug reproductions below (COL-05b, CON-01b, CON-02b) pass since their bugs were fixed. Runtimes are game ticks of one run; colony behaviour is random, so they vary.

| ID | Status | Runtime (ticks) | Notes |
|---|---|---|---|
| COL-01 | ✅ | ~1 | existed before |
| COL-02 | ✅ | ~5100 | 4 citizens arrive one at a time; also checks genders are mixed |
| COL-03 | ✅ | ~4600 | growth is by children; reproduction start timer zeroed (design corrected in colony.md) |
| COL-04 | ✅ | ~1 | `initialColonySize` 1; tower placed inside the claim near the border |
| COL-05 | ✅ | ~1 | owner/officer/friend/outsider, protection on and off |
| COL-05b | ✅ | ~1 | friend opens a hut; was E2E-COL-05-1, FIXED, now required |
| COL-06 | ✅ | ~300 | in-place reload of one colony from its saved NBT (no server restart possible) |
| COL-07 | ✅ | ~1 | |
| CON-01 | ✅ | ~35 | second colony created without the distance check (arena too small) |
| CON-01b | ✅ | ~460 | re-linking after a sign was removed; was E2E-CON-01-1, FIXED, now required |
| CON-02 | ✅ | ~40 | ally teleport checked from A's own gate house (since the E2E-CON-02-1 fix; before, the player stood near B's gate); CON-03 and CON-04 likewise |
| CON-02b | ✅ | ~40 | refused 20 blocks from the own gate house, works from it (gates 28 blocks apart); was E2E-CON-02-1, FIXED, now required |
| CON-03 | ✅ | ~40 | |
| CON-04 | ✅ | ~40 | "disconnect" does not exist in the mod; tests neutral + gate house removal (design corrected) |
| ROB-01 | ⛔ | - | chunks of a running game test can't be unloaded/reloaded |
| ROB-02 | ⛔ | - | needs a server restart; colony data part covered by COL-06 |
| ROB-03 | ⛔ | - | 100 citizens / 30 huts don't fit the arena; needs a reference-machine budget |
| ROB-04 | ⛔ | - | no 1.21.1 world in the repo; can't load another world mid-run |
| ROB-05 | ✅ | ~1000 | two colonies, axe requests, warehouse + courier in A only |
| ROB-06 | ✅ | ~6000 | respawn happens on the colony's 5-minute entity check |
| ROB-07 | ✅ | 600 | lumberjack, guard, small raid; no fisherman |

Counts: ✅ 18 (plus the existing framework test `workerWalksToHut`), ❌ 0, ⛔ 4, 📝 0.

## Bugs found

### E2E-COL-05-1 Friends can't open hut menus
- **FIXED:** the generic `OPEN_CONTAINER` check skips hut blocks (`!(block instanceof AbstractBlockHut)`); huts are governed by
  `ACCESS_HUTS`, checked just before. `col05b_friendOpensHut` is required and passes. Upstream issue:
  [docs/upstream-bugs/citizens-colony.md](../../upstream-bugs/citizens-colony.md#e2e-col-05-1-friends-cant-open-hut-menus).
- **Severity:** minor (a default rank can't use a permission it has).
- **Upstream or port:** upstream (same code in NeoForge MineColonies; not in the Fabric shim).
- **Evidence:** `ColonyPermissionEventHandler.onPlayerInteract` (src/main/java/com/minecolonies/core/colony/permissions/ColonyPermissionEventHandler.java:372)
  lets a hut click through when the player has `ACCESS_HUTS`, then continues to the generic container check (line 412). Hut block
  entities expose an item handler (`MineColonies.java:248`, `registerBlockEntity(ItemHandler.BLOCK, BUILDING, ...)`), so the click is
  treated as opening a container and refused with `OPEN_CONTAINER`, which the friend rank does not have. Found: interaction result `Fail`,
  town hall permission event `E2EFriend OPEN_CONTAINER at <hut>`. Expected: the hut menu opens (friend rank has `ACCESS_HUTS`).
- **Reproduction:** `col05b_friendOpensHut`.

### E2E-CON-01-1 A removed connection sign leaves a stale link in the other colony
- **FIXED:** `removeConnectionNode` also unlinks the partner sign in the connected colony (node's `targetColonyId`) when its next node
  is the removed sign. `con01b_reconnectAfterSignRemoved` is required and passes. Upstream issue:
  [docs/upstream-bugs/citizens-colony.md](../../upstream-bugs/citizens-colony.md#e2e-con-01-1-a-removed-connection-sign-leaves-a-stale-link-in-the-other-colony).
- **Severity:** minor (the sign chain between two colonies can't be repaired after a sign of the connecting pair is broken).
- **Upstream or port:** upstream (pure MineColonies logic).
- **Evidence:** `ColonyConnectionManager.removeConnectionNode` (src/main/java/com/minecolonies/core/colony/managers/ColonyConnectionManager.java:169)
  only resets the previous/next pointers of nodes in the *same* colony's map. The partner sign of the other colony keeps `nextNode` =
  the removed sign. A new link to it is then refused in `connectToColony` (line 372: `targetNode.hasNextNode()` -> CONNECTION_FAIL).
  Found after re-placing A's sign and sneak-using the item on B's sign: `B sign ... next <removed sign of A>`, new A sign `next 0,0,0`.
- **Reproduction:** `con01b_reconnectAfterSignRemoved`.

### E2E-CON-02-1 Ally teleport only works when the gates are at most 15 blocks apart
- **FIXED:** the range check of #11888 now measures to the gate houses of the origin colony (where the window is opened) instead of
  the target gate; a player away from all his colony's gate houses is still refused. CON-02, CON-03 and CON-04 now send the message
  from A's gate house; `con02b_allyTeleportFromOwnGate` (also checks the refusal) is required and passes. Upstream issue:
  [docs/upstream-bugs/citizens-colony.md](../../upstream-bugs/citizens-colony.md#e2e-con-02-1-ally-teleport-only-works-when-the-player-is-within-15-blocks-of-the-target-gate).
- **Severity:** major for the feature (the gate house "teleport to ally" button practically never works).
- **Upstream or port:** upstream, recent: introduced by "Packet range (#11888)" (commit d66bdaae3a), which added `IRangeRestrictedPacket`
  to `TeleportToColonyMessage`.
- **Evidence:** `TeleportToColonyMessage.onExecute` (src/main/java/com/minecolonies/core/network/messages/server/colony/TeleportToColonyMessage.java:89)
  returns unless `isInRange(player.blockPosition(), pos)` (15 blocks), where `pos` is the *target* colony's gate. The client sends it from
  the player's own gate house window (`ConnectionModuleWindow.teleportToColony`, `connectedColonyData.pos`), so the player stands at his own
  gate. Found: gates 28 blocks apart, player 6 blocks from his own gate, position unchanged. With the player within 15 blocks of the target
  gate (CON-02) the same message teleports.
- **Reproduction:** `con02b_allyTeleportFromOwnGate`.

## Design changes

- COL-03: growth beyond `initialCitizenAmount` is only by children (needs a free bed, a male and a female, "move in" on); the test zeroes
  the 5-15 minute initial reproduction delay.
- COL-04: `initialColonySize` 1 so a building can extend the claim inside the arena; huts can only be placed inside the claim.
- COL-05: the friend can't open huts (bug, split into COL-05b); hut menus stay rank-protected with protection off (by design of the
  `ACCESS_HUTS` checks, which ignore `enableColonyProtection`).
- COL-06: in-place reload of one colony (NBT of colony and citizen entities) instead of a server restart.
- CON-01: removing a sign does not disconnect the colonies; only the node is removed.
- CON-04: there is no disconnect in the mod (`DISCONNECTED` is never sent, events are never removed); the test checks neutral and gate
  house removal (relation kept, gate position zero).
- ROB-05: B's request first goes to B's retrying resolver (then the player resolver); the test accepts either, the point is that it never
  reaches A's warehouse. Couriers must be hired at a courier hut, the warehouse takes them from there.
- ROB-06: respawn is driven by the colony's 5-minute entity check, so it takes about 6000 ticks.
- ROB-07: no fisherman; real path jobs = submitted jobs minus one marker job per server tick.

## Framework issues

- Only one colony per fixture: `placeBuilding`, `spawnCitizen` and the cleanup only know the fixture's colony. The suite has private helpers
  for a second colony (`createSecondColony`, `createBareColony`, `placeIn`, `spawnIn`, `registerSecondColonyCleanup`) - candidates for the
  framework. Two full-size town halls don't fit the 64x64 arena at a normal colony distance, so two-colony tests bypass the distance check
  and use `initialColonySize` 1.
- `fixture.createColony` can't rotate the town hall and always pastes the full 24x18 structure.
- No getter for the town hall's permission events on the server side (read by reflection in COL-05 diagnostics).
- `ReproductionManager.childCreationTimer` and `CitizenManager`'s 5-minute entity check make natural growth/respawn slow; COL-03 sets the
  first by reflection.
- Worktree setup: `src/main/resources/assets/minecolonies/lang/en_us.json` is git-ignored, so a fresh worktree crashes at server start
  until it is copied in.
