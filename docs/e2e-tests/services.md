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
- With the resurrection research done (fixture) and a forced success, the citizen comes back to life instead.

**Time budget:** 10 minutes.

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
