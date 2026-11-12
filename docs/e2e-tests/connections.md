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
