package com.minecolonies.fabric;

import com.ldtteam.blockui.mod.BlockUI;
import com.ldtteam.domumornamentum.DomumOrnamentum;
import com.ldtteam.domumornamentum.recipe.ModRecipeSerializers;
import com.ldtteam.shim.api.distmarker.Dist;
import com.ldtteam.shim.fabric.ShimModBuses;
import com.ldtteam.shim.fabric.config.ShimConfigs;
import com.ldtteam.shim.fabric.events.ShimGameEvents;
import com.ldtteam.shim.fabric.loot.ShimLoot;
import com.ldtteam.shim.fml.event.lifecycle.FMLCommonSetupEvent;
import com.ldtteam.shim.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import com.ldtteam.shim.fml.event.lifecycle.FMLLoadCompleteEvent;
import com.ldtteam.shim.fml.loading.FMLEnvironment;
import com.ldtteam.shim.neoforge.network.event.RegisterPayloadHandlersEvent;
import com.ldtteam.shim.neoforge.registries.NeoForgeRegistries;
import com.ldtteam.shim.neoforge.registries.NewRegistryEvent;
import com.ldtteam.shim.neoforge.registries.RegisterEvent;
import com.ldtteam.structurize.Structurize;
import com.minecolonies.core.MineColonies;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fabric main entrypoint: emulates the NeoForge mod loading sequence for the bundled mods
 * (BlockUI, Domum Ornamentum, Structurize, MineColonies).
 */
public class MineColoniesFabric implements ModInitializer
{
    private static final Logger LOGGER = LoggerFactory.getLogger("MineColoniesFabric");

    /**
     * EventBusSubscriber classes loaded on both sides. There is no annotation scanning on Fabric: keep this list in sync with
     * grep '@EventBusSubscriber'.
     */
    private static final String[] COMMON_SUBSCRIBERS = {
        "com.minecolonies.apiimp.initializer.EntityInitializer",
        "com.minecolonies.apiimp.initializer.ModBlocksInitializer",
        "com.minecolonies.apiimp.initializer.ModContainerInitializers",
        "com.minecolonies.apiimp.initializer.ModItemsInitializer",
        "com.minecolonies.apiimp.initializer.ModParticleTypesInitializer$CommonRegistration",
        "com.minecolonies.core.entity.other.cavalry.CavalryHorseDebug",
        "com.minecolonies.core.event.QuestObjectiveEventHandler",
        "com.ldtteam.domumornamentum.event.handlers.ModBusEventHandler",
    };

    /**
     * EventBusSubscriber classes with value = Dist.CLIENT (only loaded on the physical client).
     */
    private static final String[] CLIENT_SUBSCRIBERS = {
        "com.minecolonies.apiimp.initializer.ModParticleTypesInitializer$ClientRegistration",
        "com.minecolonies.core.event.ColonyStoryListener",
        "com.minecolonies.core.event.TextureReloadListener",
        "com.ldtteam.domumornamentum.client.event.handlers.ClientTickEventHandler",
        "com.ldtteam.domumornamentum.client.event.handlers.MateriallyTexturedBlockPreviewRenderHandler",
        "com.ldtteam.domumornamentum.client.event.handlers.MateriallyTexturedBlockTintProvider",
        "com.ldtteam.domumornamentum.client.event.handlers.ModBusEventHandler",
        "com.ldtteam.domumornamentum.client.model.loader.MateriallyTexturedModelLoader",
    };

    @Override
    public void onInitialize()
    {
        final Dist dist = FMLEnvironment.dist;

        // NeoForge-specific registries must exist before any DeferredRegister targets them.
        NeoForgeRegistries.init();

        // 1. Mod construction, in dependency order.
        new BlockUI(ShimModBuses.createContainer(BlockUI.MOD_ID), dist);
        new DomumOrnamentum(ShimModBuses.createContainer(com.ldtteam.domumornamentum.util.Constants.MOD_ID), dist);
        new Structurize(ShimModBuses.createContainer(com.ldtteam.structurize.api.constants.Constants.MOD_ID), dist);
        new MineColonies(ShimModBuses.createContainer(com.minecolonies.api.util.constant.Constants.MOD_ID), dist);

        // 2. @EventBusSubscriber classes.
        registerSubscribers(COMMON_SUBSCRIBERS);
        if (dist.isClient())
        {
            registerSubscribers(CLIENT_SUBSCRIBERS);
        }

        // 3. COMMON (+ CLIENT) configs load right after construction, like FML.
        ShimConfigs.init();

        // 4. Registration: new registries, then RegisterEvent per registry.
        ShimModBuses.post(new NewRegistryEvent());
        for (final ResourceKey<? extends Registry<?>> key : registrationOrder())
        {
            final Registry<?> registry = BuiltInRegistries.REGISTRY.getValue(key.identifier());
            if (registry != null)
            {
                ShimModBuses.post(new RegisterEvent(key, registry));
            }
        }
        ShimGameEvents.postEntityAttributeCreation();
        ShimGameEvents.postBlockEntityTypeAddBlocks();

        // 5. Networking.
        ShimModBuses.post(new RegisterPayloadHandlersEvent());

        // 6. Game event bridges (Fabric callbacks / mixins -> NeoForge.EVENT_BUS) and loot modifiers.
        ShimGameEvents.init();
        ShimLoot.init();

        // 26.x no longer syncs recipes to clients: request the ones the client UIs look up.
        RecipeSynchronization.synchronizeRecipeSerializer(ShapedRecipe.SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(ShapelessRecipe.SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(SmeltingRecipe.SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(ModRecipeSerializers.ARCHITECTS_CUTTER.get());

        // 7. Setup lifecycle.
        ShimModBuses.postLifecycle(FMLCommonSetupEvent::new);
        if (dist.isDedicatedServer())
        {
            ShimModBuses.postLifecycle(FMLDedicatedServerSetupEvent::new);
            ShimModBuses.postLifecycle(FMLLoadCompleteEvent::new);
        }
        // On the physical client, FMLClientSetupEvent and FMLLoadCompleteEvent are fired by MineColoniesFabricClient.
    }

    private static void registerSubscribers(final String[] classNames)
    {
        for (final String name : classNames)
        {
            try
            {
                ShimModBuses.registerSubscriber(Class.forName(name, false, MineColoniesFabric.class.getClassLoader()));
            }
            catch (final ClassNotFoundException e)
            {
                throw new IllegalStateException("Missing @EventBusSubscriber class " + name, e);
            }
        }
    }

    /**
     * NeoForge order: attributes and data components first (items depend on them at construction), then every registry
     * in root registry (vanilla) order.
     */
    private static List<ResourceKey<? extends Registry<?>>> registrationOrder()
    {
        final Set<ResourceKey<? extends Registry<?>>> ordered = new LinkedHashSet<>();
        ordered.add(Registries.ATTRIBUTE);
        ordered.add(Registries.DATA_COMPONENT_TYPE);
        for (final Registry<?> registry : BuiltInRegistries.REGISTRY)
        {
            ordered.add(registry.key());
        }
        return new ArrayList<>(ordered);
    }
}
