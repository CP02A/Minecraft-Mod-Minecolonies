/*
 * Derived from NeoForge 26.3 (net.neoforged.neoforge.registries.DeferredRegister),
 * Copyright (c) Forge Development LLC and contributors, SPDX-License-Identifier: LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim (plus NeoForge 21.1 compatible overloads).
 */
package com.ldtteam.shim.neoforge.registries;

import com.ldtteam.shim.bus.api.IEventBus;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

/**
 * Deferred registration helper (NeoForge shim). Entries are registered when the Fabric entrypoint fires {@link RegisterEvent} for the registry.
 */
public class DeferredRegister<T>
{
    public static <T> DeferredRegister<T> create(final Registry<T> registry, final String namespace)
    {
        return new DeferredRegister<>(registry.key(), namespace);
    }

    public static <T> DeferredRegister<T> create(final ResourceKey<? extends Registry<T>> key, final String namespace)
    {
        return new DeferredRegister<>(key, namespace);
    }

    public static <B> DeferredRegister<B> create(final Identifier registryName, final String modid)
    {
        return new DeferredRegister<>(ResourceKey.createRegistryKey(registryName), modid);
    }

    public static Items createItems(final String modid)
    {
        return new Items(modid);
    }

    public static Blocks createBlocks(final String modid)
    {
        return new Blocks(modid);
    }

    public static DataComponents createDataComponents(final ResourceKey<Registry<DataComponentType<?>>> registryKey, final String modid)
    {
        return new DataComponents(registryKey, modid);
    }

    public static DataComponents createDataComponents(final String modid)
    {
        return new DataComponents(Registries.DATA_COMPONENT_TYPE, modid);
    }

    public static Entities createEntities(final String modid)
    {
        return new Entities(modid);
    }

    private final ResourceKey<? extends Registry<T>>                         registryKey;
    private final String                                                     namespace;
    private final Map<DeferredHolder<T, ? extends T>, Supplier<? extends T>> entries     = new LinkedHashMap<>();
    private final Set<DeferredHolder<T, ? extends T>>                        entriesView = Collections.unmodifiableSet(entries.keySet());
    private final Map<Identifier, Identifier>                                aliases     = new HashMap<>();

    @Nullable
    private RegistryBuilder<T> customRegistryBuilder;
    @Nullable
    private Registry<T>        customRegistry;
    @Nullable
    private RegistryHolder<T>  registryHolder;
    private boolean            seenRegisterEvent    = false;
    private boolean            seenNewRegistryEvent = false;
    private boolean            registeredEventBus   = false;

    protected DeferredRegister(final ResourceKey<? extends Registry<T>> registryKey, final String namespace)
    {
        this.registryKey = Objects.requireNonNull(registryKey);
        this.namespace = Objects.requireNonNull(namespace);
    }

    public <I extends T> DeferredHolder<T, I> register(final String name, final Supplier<? extends I> sup)
    {
        return this.register(name, key -> sup.get());
    }

    public <I extends T> DeferredHolder<T, I> register(final String name, final Function<Identifier, ? extends I> func)
    {
        if (seenRegisterEvent)
        {
            throw new IllegalStateException("Cannot register new entries to DeferredRegister after RegisterEvent has been fired.");
        }
        Objects.requireNonNull(name);
        Objects.requireNonNull(func);
        final Identifier key = Identifier.fromNamespaceAndPath(namespace, name);

        final DeferredHolder<T, I> ret = createHolder(this.registryKey, key);

        if (entries.putIfAbsent(ret, () -> func.apply(key)) != null)
        {
            throw new IllegalArgumentException("Duplicate registration " + name);
        }

        return ret;
    }

    protected <I extends T> DeferredHolder<T, I> createHolder(final ResourceKey<? extends Registry<T>> registryKey, final Identifier key)
    {
        return DeferredHolder.create(registryKey, key);
    }

    /**
     * Create a custom registry for this register's key. The registry is created when NewRegistryEvent fires.
     */
    public Supplier<Registry<T>> makeRegistry(final Consumer<RegistryBuilder<T>> consumer)
    {
        if (this.seenNewRegistryEvent)
        {
            throw new IllegalStateException("Cannot create a registry after NewRegistryEvent was fired");
        }
        final RegistryBuilder<T> registryBuilder = new RegistryBuilder<>(this.registryKey);
        consumer.accept(registryBuilder);
        this.customRegistryBuilder = registryBuilder;
        return getRegistry();
    }

    public Supplier<Registry<T>> getRegistry()
    {
        if (this.registryHolder == null)
        {
            this.registryHolder = new RegistryHolder<>(this.registryKey);
        }
        return this.registryHolder;
    }

    public TagKey<T> createTagKey(final String path)
    {
        return createTagKey(Identifier.fromNamespaceAndPath(this.namespace, Objects.requireNonNull(path)));
    }

    public TagKey<T> createTagKey(final Identifier location)
    {
        return TagKey.create(this.registryKey, Objects.requireNonNull(location));
    }

    public void addAlias(final Identifier from, final Identifier to)
    {
        if (seenRegisterEvent)
        {
            throw new IllegalStateException("Cannot add aliases to DeferredRegister after RegisterEvent has been fired.");
        }
        this.aliases.put(from, to);
    }

    public void register(final IEventBus bus)
    {
        if (this.registeredEventBus)
        {
            throw new IllegalStateException("Cannot register DeferredRegister to more than one event bus.");
        }
        this.registeredEventBus = true;
        bus.addListener(RegisterEvent.class, this::addEntries);
        bus.addListener(NewRegistryEvent.class, this::addRegistry);
    }

    public Collection<DeferredHolder<T, ? extends T>> getEntries()
    {
        return entriesView;
    }

    public ResourceKey<? extends Registry<T>> getRegistryKey()
    {
        return this.registryKey;
    }

    public Identifier getRegistryName()
    {
        return this.registryKey.identifier();
    }

    public String getNamespace()
    {
        return this.namespace;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addEntries(final RegisterEvent event)
    {
        if (!event.getRegistryKey().equals(this.registryKey))
        {
            return;
        }
        this.seenRegisterEvent = true;
        final Registry<T> registry = event.getRegistry(this.registryKey);
        if (!this.aliases.isEmpty())
        {
            // FABRIC-PORT TODO: registry aliases are not supported by vanilla registries; aliases are ignored.
        }
        for (final Map.Entry<DeferredHolder<T, ? extends T>, Supplier<? extends T>> e : entries.entrySet())
        {
            event.register(this.registryKey, e.getKey().getId(), () -> e.getValue().get());
            e.getKey().bind(false);
        }
    }

    private void addRegistry(final NewRegistryEvent event)
    {
        this.seenNewRegistryEvent = true;
        if (this.customRegistryBuilder != null && this.customRegistry == null)
        {
            this.customRegistry = event.create(this.customRegistryBuilder);
        }
    }

    public static class Blocks extends DeferredRegister<Block>
    {
        protected Blocks(final String namespace)
        {
            super(Registries.BLOCK, namespace);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <B extends Block> DeferredBlock<B> register(final String name, final Function<Identifier, ? extends B> func)
        {
            return (DeferredBlock<B>) super.register(name, func);
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(final String name, final Supplier<? extends B> sup)
        {
            return this.register(name, key -> sup.get());
        }

        public <B extends Block> DeferredBlock<B> registerBlock(final String name, final Function<BlockBehaviour.Properties, ? extends B> func, final Supplier<BlockBehaviour.Properties> properties)
        {
            return this.register(name, key -> func.apply(properties.get().setId(ResourceKey.create(Registries.BLOCK, key))));
        }

        public <B extends Block> DeferredBlock<B> registerBlock(final String name, final Function<BlockBehaviour.Properties, ? extends B> func, final UnaryOperator<BlockBehaviour.Properties> properties)
        {
            return this.registerBlock(name, func, () -> properties.apply(BlockBehaviour.Properties.of()));
        }

        public <B extends Block> DeferredBlock<B> registerBlock(final String name, final Function<BlockBehaviour.Properties, ? extends B> func, final BlockBehaviour.Properties properties)
        {
            return this.registerBlock(name, func, () -> properties);
        }

        public <B extends Block> DeferredBlock<B> registerBlock(final String name, final Function<BlockBehaviour.Properties, ? extends B> func)
        {
            return this.registerBlock(name, func, UnaryOperator.identity());
        }

        public DeferredBlock<Block> registerSimpleBlock(final String name, final Supplier<BlockBehaviour.Properties> properties)
        {
            return this.registerBlock(name, Block::new, properties);
        }

        public DeferredBlock<Block> registerSimpleBlock(final String name, final UnaryOperator<BlockBehaviour.Properties> properties)
        {
            return this.registerBlock(name, Block::new, properties);
        }

        public DeferredBlock<Block> registerSimpleBlock(final String name, final BlockBehaviour.Properties properties)
        {
            return this.registerBlock(name, Block::new, () -> properties);
        }

        public DeferredBlock<Block> registerSimpleBlock(final String name)
        {
            return this.registerSimpleBlock(name, UnaryOperator.identity());
        }

        @Override
        protected <I extends Block> DeferredBlock<I> createHolder(final ResourceKey<? extends Registry<Block>> registryKey, final Identifier key)
        {
            return DeferredBlock.createBlock(ResourceKey.create(registryKey, key));
        }
    }

    public static class Items extends DeferredRegister<Item>
    {
        protected Items(final String namespace)
        {
            super(Registries.ITEM, namespace);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <I extends Item> DeferredItem<I> register(final String name, final Function<Identifier, ? extends I> func)
        {
            return (DeferredItem<I>) super.register(name, func);
        }

        @Override
        public <I extends Item> DeferredItem<I> register(final String name, final Supplier<? extends I> sup)
        {
            return this.register(name, key -> sup.get());
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(final String name, final Supplier<? extends Block> block, final Supplier<Item.Properties> properties)
        {
            return this.registerItem(name, props -> new BlockItem(block.get(), props), () -> properties.get().useBlockDescriptionPrefix());
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(final String name, final Supplier<? extends Block> block, final UnaryOperator<Item.Properties> properties)
        {
            return this.registerSimpleBlockItem(name, block, () -> properties.apply(new Item.Properties()));
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(final String name, final Supplier<? extends Block> block, final Item.Properties properties)
        {
            return this.registerSimpleBlockItem(name, block, () -> properties);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(final String name, final Supplier<? extends Block> block)
        {
            return this.registerSimpleBlockItem(name, block, UnaryOperator.identity());
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(final Holder<Block> block, final Supplier<Item.Properties> properties)
        {
            return this.registerSimpleBlockItem(block.unwrapKey().orElseThrow().identifier().getPath(), block::value, properties);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(final Holder<Block> block, final UnaryOperator<Item.Properties> properties)
        {
            return this.registerSimpleBlockItem(block, () -> properties.apply(new Item.Properties()));
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(final Holder<Block> block, final Item.Properties properties)
        {
            return this.registerSimpleBlockItem(block, () -> properties);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(final Holder<Block> block)
        {
            return this.registerSimpleBlockItem(block, UnaryOperator.identity());
        }

        public <I extends Item> DeferredItem<I> registerItem(final String name, final Function<Item.Properties, ? extends I> func, final Supplier<Item.Properties> properties)
        {
            return this.register(name, key -> func.apply(properties.get().setId(ResourceKey.create(Registries.ITEM, key))));
        }

        public <I extends Item> DeferredItem<I> registerItem(final String name, final Function<Item.Properties, ? extends I> func, final UnaryOperator<Item.Properties> properties)
        {
            return this.registerItem(name, func, () -> properties.apply(new Item.Properties()));
        }

        public <I extends Item> DeferredItem<I> registerItem(final String name, final Function<Item.Properties, ? extends I> func, final Item.Properties properties)
        {
            return this.registerItem(name, func, () -> properties);
        }

        public <I extends Item> DeferredItem<I> registerItem(final String name, final Function<Item.Properties, ? extends I> func)
        {
            return this.registerItem(name, func, UnaryOperator.identity());
        }

        public DeferredItem<Item> registerSimpleItem(final String name, final Supplier<Item.Properties> properties)
        {
            return this.registerItem(name, Item::new, properties);
        }

        public DeferredItem<Item> registerSimpleItem(final String name, final UnaryOperator<Item.Properties> properties)
        {
            return this.registerItem(name, Item::new, properties);
        }

        public DeferredItem<Item> registerSimpleItem(final String name, final Item.Properties properties)
        {
            return this.registerItem(name, Item::new, () -> properties);
        }

        public DeferredItem<Item> registerSimpleItem(final String name)
        {
            return this.registerItem(name, Item::new, UnaryOperator.identity());
        }

        @Override
        protected <I extends Item> DeferredItem<I> createHolder(final ResourceKey<? extends Registry<Item>> registryKey, final Identifier key)
        {
            return DeferredItem.createItem(ResourceKey.create(registryKey, key));
        }
    }

    public static class DataComponents extends DeferredRegister<DataComponentType<?>>
    {
        protected DataComponents(final ResourceKey<Registry<DataComponentType<?>>> registryKey, final String namespace)
        {
            super(registryKey, namespace);
        }

        public <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> registerComponentType(final String name, final UnaryOperator<DataComponentType.Builder<D>> builder)
        {
            return this.register(name, () -> builder.apply(DataComponentType.builder()).build());
        }
    }

    public static class Entities extends DeferredRegister<EntityType<?>>
    {
        protected Entities(final String namespace)
        {
            super(Registries.ENTITY_TYPE, namespace);
        }

        public <E extends Entity> DeferredHolder<EntityType<?>, EntityType<E>> registerEntityType(final String name, final EntityType.EntityFactory<E> factory, final MobCategory category)
        {
            return this.registerEntityType(name, factory, category, UnaryOperator.identity());
        }

        public <E extends Entity> DeferredHolder<EntityType<?>, EntityType<E>> registerEntityType(final String name, final EntityType.EntityFactory<E> factory, final MobCategory category, final BiFunction<EntityType.EntityFactory<E>, MobCategory, EntityType.Builder<E>> builder)
        {
            return this.register(name, key -> builder.apply(factory, category).build(ResourceKey.create(Registries.ENTITY_TYPE, key)));
        }

        public <E extends Entity> DeferredHolder<EntityType<?>, EntityType<E>> registerEntityType(final String name, final EntityType.EntityFactory<E> factory, final MobCategory category, final UnaryOperator<EntityType.Builder<E>> builder)
        {
            return this.registerEntityType(name, factory, category, (fact, cat) -> builder.apply(EntityType.Builder.of(fact, cat)));
        }
    }

    private static class RegistryHolder<V> implements Supplier<Registry<V>>
    {
        private final ResourceKey<? extends Registry<V>> registryKey;
        private       Registry<V>                        registry = null;

        private RegistryHolder(final ResourceKey<? extends Registry<V>> registryKey)
        {
            this.registryKey = registryKey;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public @Nullable Registry<V> get()
        {
            if (this.registry == null)
            {
                this.registry = (Registry<V>) BuiltInRegistries.REGISTRY.getValue((ResourceKey) this.registryKey);
            }
            return this.registry;
        }
    }
}
