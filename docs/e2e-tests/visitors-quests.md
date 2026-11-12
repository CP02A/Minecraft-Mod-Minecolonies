[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Visitors and quests

The tavern, visitors who can be recruited, and the quest system.

---

## VIS-01 Visitors come to the tavern

**Tests:** a tavern attracts visitors who stay in it.

**Setup**
- A colony with a tavern (level 1) and free tavern beds.

**What happens**
1. The colony runs.

**Passes when**
- Visitors appear in the visitor manager and as entities at the tavern.
- There are no more visitors than the tavern's capacity.
- Visitors don't count as citizens and don't take jobs.

**Time budget:** 10 minutes.

---

## VIS-02 Recruiting a visitor

**Tests:** paying a visitor's recruitment cost turns it into a citizen.

**Setup**
- A tavern with one visitor and free housing in the colony.
- The visitor's recruitment items (from `recruitment_items`) in the fake owner's inventory.

**What happens**
1. The test recruits the visitor on behalf of the fake owner, using the same code as the GUI.

**Passes when**
- The recruitment items are taken from the owner.
- The visitor is gone from the visitor list and is now a citizen with the same name and skills.
- Without the items (second run), recruiting is refused and nothing changes.

**Time budget:** 1 minute.

---

## VIS-03 A quest from start to reward

**Tests:** a quest goes through all its steps: trigger, dialogue, objectives and reward.

**Setup**
- A colony that meets the trigger of a simple quest from `data/minecolonies/colony/quests` (for example one of the general quests).
- Everything its objectives need (items to deliver, a building level to reach) prepared by the fixture.

**What happens**
1. The quest manager offers the quest to a citizen.
2. The test answers the dialogue on behalf of the fake owner and completes each objective.

**Passes when**
- The quest becomes available after the trigger, and active after the owner accepts.
- Each objective is marked done when its condition is met.
- The reward is given (items, happiness or whatever the quest defines) and the quest is in the finished list.
- The quest happiness modifier is applied to the involved citizen.

**Time budget:** 3 minutes.

---

## VIS-04 Declining and cancelling quests

**Tests:** a declined or cancelled quest ends cleanly.

**Setup**
- Like VIS-03, two quests available.

**What happens**
1. The owner declines the first quest at the dialogue.
2. The owner accepts the second quest, then cancels it.

**Passes when**
- Neither quest gives a reward.
- Neither is stuck as active, and the quest manager's state matches what the quest files say happens on decline and cancel.

**Time budget:** 2 minutes.
