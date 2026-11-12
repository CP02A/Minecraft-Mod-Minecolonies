[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Colony connections

Gate houses and the relationships between colonies (ally, feud, neutral).

---

## CON-01 Connecting two colonies

**Tests:** two colonies with gate houses and a path of signs between them become connected.

**Setup**
- Two colonies a short distance apart, each with a gate house, owned by two different fake players.
- Connection signs placed along a path between the gate houses.

**What happens**
1. The colonies' connection managers check the path.

**Passes when**
- Each colony lists the other as connected, mapped to the right gate house.
- The pending connection points are resolved into connection nodes.
- Removing a sign in the middle breaks the connection.

**Time budget:** 2 minutes.

**Implemented:** `ColonyConnectionsRobustnessTests.con01_connectTwoColonies` and `ColonyConnectionsRobustnessTests.con01b_reconnectAfterSignRemoved`
(optional, fails: bug E2E-CON-01-1).

**As implemented:** two colonies don't fit into the arena at the normal distance. The test uses `initialColonySize` 1 and creates colony B
like the server side of the create-colony message but without its distance check (B's center is at least 2 chunks from A's, so the static
claims don't overlap). Each owner sneak-uses a colony sign item on his gate house (links the item), places a sign in front of it (pending
node -> path -> node), then owner A sneak-uses his item on B's sign (`attemptEstablishConnection`). Positions are chosen so every hut and sign
lies in its own colony's static claim (otherwise the other colony's protection refuses the sign).

**Design corrected:** removing a sign does not remove the connection: the colonies stay listed as connected (the relation is independent
of the sign chain); only the node disappears. Re-placing the sign and re-linking does not work (bug E2E-CON-01-1).

---

## CON-02 Becoming allies

**Tests:** an ally request and its confirmation make both colonies allies.

**Setup**
- Two connected colonies (as CON-01).

**What happens**
1. Colony A sends an ally request.
2. Colony B confirms it.

**Passes when**
- After the request, B has a pending `ALLY_REQUEST` event from A.
- After the confirmation, both colonies show the `ALLY_CONFIRMED` relation.
- Allied features work (for example ally town hall teleport, if `canPlayerUseAllyTHTeleport` allows it).

**Time budget:** 1 minute.

**Implemented:** `ColonyConnectionsRobustnessTests.con02_becomingAllies` and `ColonyConnectionsRobustnessTests.con02b_allyTeleportFromOwnGate`
(optional, fails: bug E2E-CON-02-1).

**As implemented:** events are sent with `TriggerConnectionEventMessage` (server side, as the alliance page sends them). The allied feature
checked is `TeleportToColonyMessage` for a friend of A: it works when the player stands within 15 blocks of B's gate (CON-02), but not from
A's own gate house window, which is how the client sends it (CON-02b).

---

## CON-03 Feud

**Tests:** starting a feud changes the relation for both sides.

**Setup**
- Two connected colonies.

**What happens**
1. Colony A starts a feud with colony B.

**Passes when**
- Both colonies record the `FEUD_STARTED` relation.
- Ally features are not available while feuding.

**Time budget:** 1 minute.

**Implemented:** `ColonyConnectionsRobustnessTests.con03_feud`. The feud is started from an alliance; both sides show `HOSTILE`
(`FEUD_STARTED`) and the ally teleport is refused (player standing within the message range of B's gate).

---

## CON-04 Neutral and disconnect

**Tests:** a relation can be reset to neutral, and disconnecting removes it.

**Setup**
- Two allied colonies.

**What happens**
1. Colony A sets the relation to neutral.
2. Colony A disconnects.

**Passes when**
- After step 1, both colonies show `NEUTRAL_SET`.
- After step 2, neither colony lists the other as connected (`DISCONNECTED`), and no event for the other colony is left pending.

**Time budget:** 1 minute.

**Implemented:** `ColonyConnectionsRobustnessTests.con04_neutralAndGateRemoval`.

**Design corrected:** the mod has no "disconnect": `ConnectionEventType.DISCONNECTED` is never sent (the alliance page has buttons for
ally request/confirm, feud and neutral only) and nothing removes a directly connected colony while both exist. Events are never removed
either; a newer event of the same colony replaces the older one. The test checks: after `NEUTRAL_SET` both sides are `NEUTRAL`, B's event
from A is `NEUTRAL_SET` (the ally request is gone) and the ally teleport is refused; after A's gate house is broken, B still lists A but
with gate position zero (no teleport target).
