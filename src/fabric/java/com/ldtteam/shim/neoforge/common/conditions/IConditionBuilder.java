/*
 * Copied from NeoForge 21.1.80 (net.neoforged.neoforge.common.conditions), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim (26.3 names, shim registries).
 */
package com.ldtteam.shim.neoforge.common.conditions;

import java.util.List;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public interface IConditionBuilder {
    default ICondition and(ICondition... values) {
        return new AndCondition(List.of(values));
    }

    default ICondition FALSE() {
        return FalseCondition.INSTANCE;
    }

    default ICondition TRUE() {
        return TrueCondition.INSTANCE;
    }

    default ICondition not(ICondition value) {
        return new NotCondition(value);
    }

    default ICondition or(ICondition... values) {
        return new OrCondition(List.of(values));
    }

    default ICondition itemExists(String namespace, String path) {
        return new ItemExistsCondition(namespace, path);
    }

    default ICondition modLoaded(String modid) {
        return new ModLoadedCondition(modid);
    }

    default ICondition tagEmpty(TagKey<Item> tag) {
        return new TagEmptyCondition(tag.location());
    }
}
