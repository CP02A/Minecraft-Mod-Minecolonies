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

**Time budget:** 30 seconds.

---

## RES-03 A research changes behavior

**Tests:** research effects really change what citizens do, not only a number in the GUI.

**Setup**
- Two otherwise identical colonies. One has finished a research with a measurable effect (for example more citizen health, or a
  faster worker), the other hasn't.

**What happens**
1. Both colonies run the same task.

**Passes when**
- The measured value differs as the research describes (for example a higher maximum health, or the task finished faster).

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
- Research that depends on it is reset too, as the rules say.
- The undo cost is taken.

**Time budget:** 30 seconds.
