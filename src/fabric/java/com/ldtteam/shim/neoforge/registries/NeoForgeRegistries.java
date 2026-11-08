package com.ldtteam.shim.neoforge.registries;

import com.ldtteam.shim.neoforge.common.crafting.IngredientType;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * NeoForge-specific registries (NeoForge shim). Only the registries used by the bundled mods are provided.
 */
public final class NeoForgeRegistries
{
    /**
     * Custom ingredient types. Entries are forwarded to Fabric custom ingredient serializers via IngredientType#register.
     */
    public static final Registry<IngredientType<?>> INGREDIENT_TYPES;

    static
    {
        INGREDIENT_TYPES = FabricRegistryBuilder.create(Keys.INGREDIENT_TYPES).attribute(RegistryAttribute.SYNCED).buildAndRegister();
        RegistryEntryAddedCallback.event(INGREDIENT_TYPES).register((rawId, id, type) -> IngredientType.register(id, type));
    }

    private NeoForgeRegistries()
    {
    }

    /**
     * Force class init (creates the registries). Called by the Fabric entrypoint before NewRegistryEvent.
     */
    public static void init()
    {
    }

    public static final class Keys
    {
        public static final ResourceKey<Registry<IngredientType<?>>> INGREDIENT_TYPES = key("ingredient_serializer");

        private Keys()
        {
        }

        private static <T> ResourceKey<Registry<T>> key(final String name)
        {
            return ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("neoforge", name));
        }
    }
}
