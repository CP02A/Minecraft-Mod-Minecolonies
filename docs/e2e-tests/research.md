[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Research

The university, the research tree and the effects research has on the colony.

---

## RES-01 Researcher finishes a research

**Tests:** a researcher works on a research until it is done.

**Setup**
- A university with a researcher. A research from the civilian branch whose requirements are met, started by the fixture, and its
  cost items paid.

**What happens**
1. The researcher works until the research time has passed.

**Passes when**
- The research progress rises while the researcher works, and not while the university has no researcher.
- The research ends in the finished state.
- Its effect is active in the colony's research effects.

*As implemented:* First Aid (civilian, depth 1) is started like the research window's button by a survival player, paying 8 hay bales
from the university; then the test skips ahead to 3 steps before the end (a depth 1 research needs 72 progress steps; the university
adds one per researcher every colony tick of 500 game ticks, the researcher's study adds more). The "no researcher" check lets 1,100
ticks (two colony ticks) pass.

**Time budget:** 15 minutes.

---

## RES-02 Requirements block a research

**Tests:** research can't start while its requirements are missing.

**Setup**
- A colony whose university level and buildings don't meet a research's requirements (for example a building level requirement).
- The research's cost items available.

**What happens**
1. The test tries to start the research through the same code as the research GUI.
2. The fixture fulfills the requirement and tries again.

**Passes when**
- The first attempt is refused and no items are taken.
- The second attempt starts the research and takes the cost.

*As implemented:* Keen (needs residences with levels adding up to 3, costs 3 books from the university). The attempt is made by a
survival player: for a creative player the server skips both the requirement check and the cost. The requirement is fulfilled by
adding a level 3 residence.

**Time budget:** 30 seconds.

---

## RES-03 A research changes behavior

**Tests:** research effects really change what citizens do, not only a number in the GUI.

**Setup**
- One colony (the fixture supports one colony per test), measured before and after the research: First Aid (+health).

**What happens**
1. Both colonies run the same task.

**Passes when**
- The measured value differs as the research describes: the citizen entity's maximum health attribute rises by the effect's
  strength, for a citizen alive when the research finishes and for one spawned later, and the citizen can really be healed above
  the old maximum.

**Time budget:** 10 minutes.

---

## RES-04 Unlock research

**Tests:** buildings and guard types locked behind research become available once it's done.

**Setup**
- A colony without the research that unlocks a building (or a guard type such as the cavalry).

**What happens**
1. The test tries to place or hire the locked thing.
2. The fixture completes the unlock research and tries again.

**Passes when**
- The first attempt is refused.
- After the research, it works.

*As implemented:* the hospital (unlocked by Stamina). The hut can be placed; the refusal happens when its build is requested
(`AbstractBuilding#requestUpgrade` checks the unlock effect). Guard types are not covered.

**Time budget:** 1 minute.

---

## RES-05 Undoing a research

**Tests:** a finished research can be reset, and its effect goes away.

**Setup**
- A colony with a finished research that has a visible effect, and the items an undo costs.

**What happens**
1. The test undoes the research through the same code as the GUI.

**Passes when**
- The research is back in the not started state.
- Its effect is gone from the colony's research effects.
- Research that depends on it is **not** reset with it: the undo is refused while a child research is started or finished
  (`LocalResearchTree#attemptResetResearch`); undoing the child first works, and then the parent.
- The undo cost (config `researchResetCost`, default one ancient tome) is taken from the player's inventory each time; a refused undo
  takes nothing. The test uses a survival player (creative players don't pay).

**Time budget:** 30 seconds.
