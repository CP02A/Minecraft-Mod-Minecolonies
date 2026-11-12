[← Citizen life](../citizens.md) · [Services](../services.md)

# Protocol: citizens and services (CIT-01..14, SRV-01..05)

Suite class: `src/gametest/java/com/minecolonies/gametest/suites/CitizensServicesTests.java`, run with
`./gradlew --no-daemon runGameTest -Pe2eFilter=citizens_services_tests`. Runtimes are the test ticks of the final run, rounded
(they vary from run to run because of random AI choices).

| ID | Status | Runtime (ticks) | Notes |
|---|---|---|---|
| CIT-01 | ✅ passing | ~1430 | Eats until saturation 60 (not 20); then 200 ticks without eating. |
| CIT-02 | ✅ passing | ~1080 | Food happiness factor not usable (needs home + 10-meal history); checks food history and `food_served` instead. |
| CIT-03 | ✅ passing | ~230 | Interaction `ai.norestaurant`; social factor instead of food factor. |
| CIT-04 | ✅ passing | ~360 | Sleep in own beds, distinct beds, "slept tonight" counter reset, wake up in the morning. |
| CIT-05 | ✅ passing | ~470 | Lumberjack + teacher (rain-capable hut) instead of lumberjack + farmer. |
| CIT-06 | ✅ passing | ~1 | Per-citizen factors and totals for homeless / well fed. |
| CIT-06b | ❌ fails due to mod bug | ~1 | E2E-CIT06-1: unemployed and sick citizens are *happier* than the baseline. `required = false`. |
| CIT-07 | ✅ passing | ~1030–1130 | Sequential: natural healing (~765 ticks) vs. with hospital (~265–365). |
| CIT-08 | ✅ passing | ~2120 | Hospital bed, cure items used, disease removed. |
| CIT-08b | ✅ passing | ~380–475 | Without items the healer requests exactly carrot + potato. |
| CIT-09 | ✅ passing | ~370 | Grave with items, grave manager, death modifier, partner mourns from the next morning. |
| CIT-10 | ✅ passing | ~580 | Birth via timer (reflection), growth via child AI active time (reflection), then hired. |
| CIT-11 | ✅ passing | ~1630–1805 | Lumberjack needs axe + hoe; complementary/adverse skills also change. |
| CIT-12 | ✅ passing | ~1060–1115 | Door, stairs, ladder, bridge, ford; door closed again; no damage. |
| CIT-13 | ✅ passing | ~2340–2740 | Gets out by the stuck handler's teleport (log: "Entity complete stuck action ... teleport:true"); then at its hut. Slower than the 2 minute design budget. |
| CIT-14 | ✅ passing | ~90 | "Cancel" answer on a request interaction cancels the request; interaction closes. |
| SRV-01 | ✅ passing | ~960–1490 | Pupils sit, gain Intelligence, paper used up, teacher requests paper, adult removed from pupil place. |
| SRV-02 | ✅ passing | ~1410–1450 | Students gain Intelligence, paper used up. |
| SRV-02b | ❌ fails due to mod bug | 0 | E2E-SRV02-1: study items lower the chance of an Intelligence gain. `required = false`. |
| SRV-03 | ✅ passing | ~2120–2165 | Influenza cured, measles items requested, injured healed, distinct hospital beds. |
| SRV-04 | ✅ passing | ~1080 | Grave dug up, citizen buried (resting list), items in the graveyard. |
| SRV-04b | ✅ passing | ~6080 | Resurrection with forced roll. |
| SRV-05 | ✅ passing | 0 | Factor 1.0 / 1.0 / 1.5 / 2.5 for no site / level 1 / 3 / 5. |

Totals: 21 ✅ passing (19 designed tests, some split into a and b parts), 2 ❌ fail due to mod bugs, 0 ⛔, 0 📝.

## Bugs found

### E2E-CIT06-1 Negative happiness factors can raise total happiness

- **Severity:** medium (happiness drives skill caps, e.g. `tryLevelUpIntelligence` caps at happiness × 9, and citizens leaving).
- **Port-specific or upstream:** upstream. The code is shared MineColonies code (`src/main`), unchanged by the Fabric port; it dates
  back to before the core refactor (#9707).
- **Evidence:** `CitizenHappinessHandler.getHappiness` (`src/main/java/com/minecolonies/core/entity/citizen/citizenhandlers/CitizenHappinessHandler.java:148`)
  skips every factor that is exactly 1.0 and averages the rest. A below-neutral factor whose value is above the current average
  pulls the average *up*. In the test the baseline citizen (home, job, healthy) has 3.00; the same citizen without a job 3.50, the
  same citizen sick 3.50 (homeless 2.00, well fed 10.00). Expected: unemployed and sick < baseline.
- **Reproduction:** `cit06b_negativeFactorsLowerHappiness`.

### E2E-SRV02-1 Study items make studying less successful

- **Severity:** medium (the library's study items have the opposite effect of their configuration; book (60% "skill increase
  chance") is the worst option, studying with nothing the best).
- **Port-specific or upstream:** upstream. Introduced in #7349 ("Fix library issues ...", 2021): the old check
  `random.nextInt(customChance) > 0` (fail, i.e. success 1/customChance) became `random.nextDouble() * customChance < 1` (fail,
  i.e. success 1 − 1/customChance), and the item factor changed from `100 / pct` to `10 / chance`.
- **Evidence:** `CitizenSkillHandler.tryLevelUpIntelligence`
  (`src/main/java/com/minecolonies/core/entity/citizen/citizenhandlers/CitizenSkillHandler.java:152`), called from
  `EntityAIStudy.study` with `ONE_IN_X_CHANCE` (8) without items and `8 * (10 / skill_increase_chance)` with items. 4000 rolls each:
  no item 3565 successes (89%), paper 1984 (50%), book 1057 (26%). Expected: no item lowest.
- **Reproduction:** `srv02b_studyItemsHelp`.

## Design changes

Each changed section in [citizens.md](../citizens.md) and [services.md](../services.md) has an "As tested" paragraph. In short:

- **CIT-01:** full saturation is 60 (`ICitizenData.MAX_SATURATION`), not 20.
- **CIT-02, CIT-03:** the food happiness factor measures diversity/quality of the last 10 meals and only applies with a home; it
  doesn't react to one meal or to hunger. CIT-02 checks the food history and `food_served`, CIT-03 the social factor.
- **CIT-05:** no "take shelter" behaviour exists; in the rain workers go to citizen AI IDLE with status BAD_WEATHER. Teacher
  instead of farmer (a farmer without a field is idle anyway; the teacher's hut works in the rain).
- **CIT-06:** "health" is about disease, not injury; security/social/mystical are colony wide. Totals split into `cit06b` (bug).
- **CIT-07:** the healer heals every citizen under 10 HP near the hospital, so the comparison is sequential in one colony.
- **CIT-08:** infection via reflection (`setDisease` refuses with ≤ 4 citizens); the "without items" run is `cit08b`.
- **CIT-09:** mourning starts at the next wake-up, not at the death.
- **CIT-11:** complementary skills gain and adverse skills lose a share; lumberjack needs axe and hoe; level 1 hut needs low tools.
- **CIT-13:** recovery took 2300–2800 ticks (teleport), more than the 2 minute budget.
- **SRV-01:** an adult in a pupil place is removed on the next colony tick, not refused.
- **SRV-02:** "slower without items" checked statistically in `srv02b` with the AI's own chances (a real study session is 1200 ticks).
- **SRV-04:** "forced success" = the undertaker AI's `random` replaced by one returning 0; resurrection research isn't required.
- **SRV-05:** one colony with sites added one after the other; level 1 and 2 sites give no bonus (`max(1, level / 2)`).

## Framework issues

- **Fresh worktree:** `src/main/resources/assets/minecolonies/lang/en_us.json` is git-ignored, so the server crashes at start
  (`LanguageHandler$LanguageCache.load` NPE) until it is copied from the main checkout.
- **No accessor for open interactions:** `CitizenData.citizenChatOptions` is protected; the suite reads it by reflection
  (`chatOptions` helper). A fixture helper `interactions(ICitizenData)` would help other suites.
- **No helper to infect a citizen:** `setDisease` refuses while the colony has ≤ `initialCitizenAmount` (4) citizens; the suite sets
  the field by reflection (`infect` helper). With more than four citizens a disease also spreads on contact within a few ticks.
- **Killing a citizen:** damage per hit is capped, one `hurtServer` call is not enough; the suite hits once per tick (`killStep`).
- **Child growth / reproduction:** no fixture helpers; the suite sets `ReproductionManager.childCreationTimer` and
  `EntityAICitizenChild.aiActiveTime` by reflection (the child AI is found by walking the citizen AI's transitions).
- **Happiness cache:** `getHappiness` is cached until a modifier changes; the suite clears it with `resetModifier(HOMELESSNESS)`.
- Not a framework bug, but worth knowing: helper classes of the suite are `private static` methods in the suite class; candidates
  for the framework are `give`, `infect`, `killStep`, `chatOptions`, `requestedItems`, `walkTo` (same as `/mc citizens walk`).
