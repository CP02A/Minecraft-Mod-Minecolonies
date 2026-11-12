[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Jobs: education, care and the dead

Buildings that look after citizens rather than producing goods.

---

## SRV-01 School

**Tests:** the teacher teaches children and they get smarter.

**Setup**
- A school with a teacher, two children as pupils, and paper in the school.

**What happens**
1. Teacher and pupils go to class.

**Passes when**
- The pupils sit at the school's desks during class.
- The pupils' skills rise (the skills the school teaches).
- Paper is used up, and without paper the teacher requests it.
- Adults are never assigned as pupils.

**Time budget:** 10 minutes.

**As tested (`srv01_school`):** two children with all skills at 1 (homeless citizens get no XP for skills ≥ 10) and two sheets of paper. Class is before noon (pupils idle after 6000). "Sitting at a desk" = riding a sitting entity. An adult put into a pupil place isn't refused, it is removed again on the next colony tick (`ChildrenBuildingModule.onColonyTick`). After both sheets are used up, the teacher has an open request for paper.

---

## SRV-02 Library

**Tests:** students study and gain knowledge with study items.

**Setup**
- A library with two students and study items from `data/minecolonies/study_items` in the library.

**What happens**
1. The students study.

**Passes when**
- The students' intelligence rises.
- Study items are used up at their configured rate.
- Studying without study items still works, but slower.

**Time budget:** 10 minutes.

**As tested (`srv02_library`, `srv02b_studyItemsHelp`):** paper (break chance 100%) is used, so its use is visible. A student studies once per 1200 ticks. Whether studying without items is slower is checked in `srv02b` with the exact chances `EntityAIStudy` passes to `CitizenSkillHandler.tryLevelUpIntelligence` (4000 rolls each); before the fix of bug E2E-SRV02-1 items made studying *less* likely to succeed.

---

## SRV-03 Hospital

**Tests:** the healer handles several patients and asks for what it needs.

**Setup**
- A hospital with a healer, two sick citizens with different diseases and one injured citizen.
- Cure items for only one of the diseases in the hospital.

**What happens**
1. The patients come in. The healer works.

**Passes when**
- The disease with cure items is cured.
- The healer requests the cure items for the other disease.
- The injured citizen is healed.
- Each patient is in its own hospital bed.

**Time budget:** 10 minutes.

**As tested (`srv03_hospital`):** a level 2 hospital; influenza (cure in stock) and measles (no cure). "Each in its own bed" = both sick patients were seen asleep in a hospital bed and never in the same bed. The injured citizen counts as healed at 10 health. Also checked: no influenza items were requested.

---

## SRV-04 Graveyard

**Tests:** the undertaker buries dead citizens and their names go on the graveyard.

**Setup**
- A graveyard with an undertaker and a shovel. A citizen dies near the colony (as in [CIT-09](citizens.md#cit-09-death-grave-and-mourning)).

**What happens**
1. The undertaker digs up the grave and buries the citizen in the graveyard.

**Passes when**
- The grave block is gone and its items are in the graveyard's inventory.
- The citizen's name is in the graveyard's list of buried citizens and a burial spot is used.

**Time budget:** 10 minutes.

**As tested (`srv04_graveyard`):** the undertaker gets a wooden shovel (a level 1 hut refuses better tools). Without research the undertaker still resurrects with about 3% (level 1 graveyard x 0.005 + Mana 20 x 0.00125); the citizen then lives and is never buried. The test forces that roll to miss by replacing the undertaker AI's `random` with one whose `nextDouble()` returns 1. Resurrection is [SRV-06](#srv-06-revival).

---

## SRV-05 Mystical site

**Tests:** a mystical site makes citizens happier, more at higher levels.

**Setup**
- Two identical colonies, one with a mystical site, and a third with the site at a higher level.

**What happens**
1. The colonies run long enough for happiness to update.

**Passes when**
- Citizens in the colony with the site have the mystical site happiness modifier, citizens without it don't.
- The higher level site gives a bigger bonus.

**Time budget:** 2 minutes.

**As tested (`srv05_mysticalSite`):** one colony instead of three: sites of level 1, 3 and 5 are added one after the other and the factor is read after each. The factor is `max(1, highest site level / 2)`, so a level 1 (or 2) site gives *no* bonus; levels 3 and 5 give 1.5 and 2.5. The factor is colony wide (same for every citizen); total happiness grows with it.

---

## SRV-06 Revival

**Tests:** when the undertaker's resurrection roll hits, the dead citizen comes back to life with its data instead of being buried.

**Setup**
- A graveyard with an undertaker and a shovel. A citizen with known skill levels dies near the colony (as in SRV-04).
- The test controls the roll: it hits. No resurrection research.

**What happens**
1. The undertaker digs up the grave, takes its items and tries to resurrect the citizen.

**Passes when**
- The citizen is alive again, with the same name, gender and skill levels.
- The grave block is gone and the colony has no grave left.
- The citizen is not in the graveyard's list of buried citizens, and there is only one citizen with its name.

**Time budget:** 12 minutes.

**As tested (`srv06_revival`):** the only random number of the roll is `random.nextDouble()` in `EntityAIWorkUndertaker.tryResurrect`
(resurrects when `chance >= random.nextDouble()`). The test replaces that undertaker AI's own `random` field (test-only reflection,
nothing else changes) with a `Random` whose `nextDouble()` returns 0, so the roll hits with the base chance. The citizen data comes
from the grave (`GraveData.getCitizenDataNBT`); job and home are cleared on resurrection (`CitizenData.onResurrect`), so they aren't
checked. Resurrection takes about 6,000 ticks (the "effort" counter grows with the Mana skill); the wait allows 14,000.
