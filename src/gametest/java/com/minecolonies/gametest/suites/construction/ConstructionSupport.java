package com.minecolonies.gametest.suites.construction;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.placement.BlockPlacementResult;
import com.ldtteam.structurize.placement.StructurePhasePlacementResult;
import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import com.ldtteam.structurize.util.BlockInfo;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.workorders.IServerWorkOrder;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.api.util.LoadOnlyStructureHandler;
import com.minecolonies.core.network.messages.server.DecorationBuildRequestMessage;
import com.minecolonies.core.placementhandlers.main.SurvivalHandler;
import com.minecolonies.gametest.framework.ColonyFixture;
import com.google.common.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ldtteam.structurize.placement.AbstractBlueprintIterator.NULL_POS;

/**
 * Helpers for the construction tests (ConstructionResearchTests): blueprint lookup, survival hut placement, material lists and
 * blueprint-vs-world comparison. No game tests in here.
 */
public final class ConstructionSupport
{
    private ConstructionSupport()
    {
    }

    /**
     * @return the pack-relative path of the blueprint {@code name} (e.g. {@code residence1}) in the default pack.
     */
    public static String subPath(final String name)
    {
        final StructurePackMeta meta = StructurePacks.getStructurePack(ColonyFixture.DEFAULT_PACK);
        final Path file = StructurePacks.findBlueprint(ColonyFixture.DEFAULT_PACK, name);
        if (meta == null || file == null)
        {
            throw new IllegalStateException("No blueprint '" + name + "' in pack " + ColonyFixture.DEFAULT_PACK);
        }
        return meta.getSubPath(file).replace('\\', '/');
    }

    /**
     * Loads a blueprint of the default pack, unrotated.
     */
    public static Blueprint blueprint(final Level level, final String subPath)
    {
        final Blueprint blueprint = StructurePacks.getBlueprint(ColonyFixture.DEFAULT_PACK, subPath, level.registryAccess());
        if (blueprint == null)
        {
            throw new IllegalStateException("Could not load blueprint " + subPath);
        }
        blueprint.setRotationMirror(RotationMirror.NONE, level);
        return blueprint;
    }

    /**
     * Places a hut the way a survival player does with the build tool (MineColonies' {@link SurvivalHandler}): only the hut block is set,
     * the building is registered at level 0 and waits for a build work order.
     *
     * @param relPos relative position of the hut block.
     * @return the registered level 0 building.
     */
    public static IBuilding placeHutForConstruction(final ColonyFixture fixture, final BuildingEntry type, final BlockPos relPos)
    {
        final String name = type.getRegistryName().getPath() + "1";
        final String subPath = subPath(name);
        final Blueprint blueprint = blueprint(fixture.level(), subPath);
        final BlockPos absPos = fixture.abs(relPos);
        new SurvivalHandler().handle(blueprint, ColonyFixture.DEFAULT_PACK, subPath, false, fixture.level(), fixture.owner(), absPos, RotationMirror.NONE);
        final IBuilding building = fixture.colony().getServerBuildingManager().getBuilding(absPos);
        if (building == null)
        {
            throw new IllegalStateException("Survival placement of " + name + " at " + absPos + " registered no building");
        }
        return building;
    }

    /**
     * Material list of a blueprint at a position, computed the way the hut window's resource list does
     * ({@code WindowBuildBuilding#updateResources}): every block's required items, without the hut block itself.
     */
    public static List<ItemStack> materials(final Level level, final Blueprint blueprint, final BlockPos absAnchor)
    {
        final LoadOnlyStructureHandler handler = new LoadOnlyStructureHandler(level, absAnchor, blueprint, RotationMirror.NONE);
        final StructurePlacer placer = new StructurePlacer(handler);
        final Map<String, ItemStack> resources = new LinkedHashMap<>();
        BlockPos progress = NULL_POS;
        StructurePhasePlacementResult result;
        int guard = 0;
        do
        {
            result = placer.executeStructureStep(level, null, progress, StructurePlacer.Operation.GET_RES_REQUIREMENTS,
              () -> placer.getIterator().increment((info, pos, h) -> false), true);
            progress = result.getIteratorPos();
            for (final ItemStack stack : result.getBlockResult().getRequiredItems())
            {
                if (stack.isEmpty())
                {
                    continue;
                }
                final String key = stack.getItem().getDescriptionId() + "-" + stack.getComponentsPatch().hashCode();
                final ItemStack existing = resources.get(key);
                if (existing == null)
                {
                    resources.put(key, stack.copy());
                }
                else
                {
                    existing.grow(stack.getCount());
                }
            }
        }
        while (result.getBlockResult().getResult() != BlockPlacementResult.Result.FINISHED && ++guard < 100_000);
        return new ArrayList<>(resources.values());
    }

    /**
     * Compares the world with the blueprint using Structurize's own "is this block already done" rule
     * ({@link IPlacementHandler#doesWorldStateMatchBlueprintState}, the check the builder uses to skip finished blocks).
     *
     * @return one line per position that doesn't match (at most {@code limit}), empty if the world matches the blueprint.
     */
    public static List<String> mismatches(final Level level, final Blueprint blueprint, final BlockPos absAnchor, final int limit)
    {
        final LoadOnlyStructureHandler handler = new LoadOnlyStructureHandler(level, absAnchor, blueprint, RotationMirror.NONE);
        final List<String> problems = new ArrayList<>();
        int total = 0;
        for (final BlockInfo info : blueprint.getBlockInfoAsList())
        {
            final BlockPos worldPos = handler.getProgressPosInWorld(info.getPos());
            if (!IPlacementHandler.doesWorldStateMatchBlueprintState(info, worldPos, handler))
            {
                total++;
                if (problems.size() < limit)
                {
                    problems.add("local " + info.getPos().toShortString() + ": expected " + info.getState() + ", found " + level.getBlockState(worldPos));
                }
            }
        }
        if (total > problems.size())
        {
            problems.add("... " + (total - problems.size()) + " more");
        }
        return problems;
    }

    /**
     * World positions of a blueprint's footprint (inclusive min and max corner) at an anchor.
     */
    public static BlockPos[] footprint(final Blueprint blueprint, final BlockPos absAnchor)
    {
        final BlockPos min = absAnchor.subtract(blueprint.getPrimaryBlockOffset());
        final BlockPos max = min.offset(blueprint.getSizeX() - 1, blueprint.getSizeY() - 1, blueprint.getSizeZ() - 1);
        return new BlockPos[] {min, max};
    }

    /**
     * Snapshot of the world states of all blueprint positions.
     */
    public static Map<BlockPos, BlockState> snapshot(final Level level, final Blueprint blueprint, final BlockPos absAnchor)
    {
        final LoadOnlyStructureHandler handler = new LoadOnlyStructureHandler(level, absAnchor, blueprint, RotationMirror.NONE);
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        for (final BlockInfo info : blueprint.getBlockInfoAsList())
        {
            final BlockPos worldPos = handler.getProgressPosInWorld(info.getPos());
            states.put(worldPos, level.getBlockState(worldPos));
        }
        return states;
    }

    /**
     * Tools and food a level 1 builder needs for clearing and digging (stone = the best a level 1 hut allows).
     */
    public static ItemStack[] builderKit()
    {
        return new ItemStack[] {
          new ItemStack(Items.STONE_PICKAXE), new ItemStack(Items.STONE_PICKAXE),
          new ItemStack(Items.STONE_SHOVEL), new ItemStack(Items.STONE_SHOVEL),
          new ItemStack(Items.STONE_AXE), new ItemStack(Items.STONE_AXE),
          new ItemStack(Items.STONE_HOE), new ItemStack(Items.SHEARS),
          new ItemStack(Items.BREAD, 32)};
    }

    /**
     * @return the work order at that location, or null.
     */
    public static IServerWorkOrder orderAt(final IColony colony, final BlockPos absLocation)
    {
        for (final IServerWorkOrder order : colony.getWorkManager().getWorkOrders().values())
        {
            if (order.getLocation().equals(absLocation))
            {
                return order;
            }
        }
        return null;
    }

    /**
     * Requests a decoration work order with the server side of the build tool's decoration window message
     * ({@link DecorationBuildRequestMessage}), the code a survival player's build tool placement ends in.
     *
     * @param builderAbs a builder's hut to assign it to, or {@link BlockPos#ZERO} for "any builder".
     */
    public static void requestDecoration(final ColonyFixture fixture, final String subPath, final BlockPos absPos, final BlockPos builderAbs)
    {
        final DecorationBuildRequestMessage message = new DecorationBuildRequestMessage(WorkOrderType.BUILD, absPos, ColonyFixture.DEFAULT_PACK, subPath,
          fixture.level().dimension(), RotationMirror.NONE, builderAbs);
        try
        {
            final Method onExecute = DecorationBuildRequestMessage.class.getDeclaredMethod("onExecute",
              com.ldtteam.shim.neoforge.network.handling.IPayloadContext.class, ServerPlayer.class);
            onExecute.setAccessible(true);
            onExecute.invoke(message, null, fixture.owner());
        }
        catch (final ReflectiveOperationException e)
        {
            throw new IllegalStateException("Could not run DecorationBuildRequestMessage on the server", e);
        }
    }

    /**
     * Open "deliver this item stack" requests of a building (building-level and of its citizens).
     */
    public static List<IRequest<? extends Stack>> openStackRequests(final IBuilding building)
    {
        final List<IRequest<? extends Stack>> result = new ArrayList<>(building.getOpenRequestsOfType(-1, TypeToken.of(Stack.class)));
        building.getAllAssignedCitizen().forEach(c -> result.addAll(building.getOpenRequestsOfType(c.getId(), TypeToken.of(Stack.class))));
        return result;
    }

    /**
     * Small decorations of the default pack, tried in this order.
     */
    private static final String[] DECORATIONS = {
      "decorations/statues/pedastal_oak.blueprint", "decorations/statues/pedastal_iron.blueprint", "decorations/statues/pedastal_gold.blueprint",
      "decorations/statues/pillar.blueprint", "decorations/statues/stone_lion.blueprint", "decorations/misc/well.blueprint",
      "decorations/misc/simplefountain.blueprint"};

    /**
     * @return the first small decoration of the pack without entities (item frames etc.), so its material list is complete. Logs every
     * candidate with its entity count.
     */
    public static String entityFreeDecoration(final Level level)
    {
        for (final String candidate : DECORATIONS)
        {
            final Blueprint blueprint = StructurePacks.getBlueprint(ColonyFixture.DEFAULT_PACK, candidate, level.registryAccess());
            if (blueprint == null)
            {
                continue;
            }
            org.slf4j.LoggerFactory.getLogger(ConstructionSupport.class).info("Decoration {}: size {}x{}x{}, entities {}", candidate,
              blueprint.getSizeX(), blueprint.getSizeY(), blueprint.getSizeZ(), blueprint.getEntitiesAsList().size());
            if (blueprint.getEntitiesAsList().isEmpty())
            {
                return candidate;
            }
        }
        throw new IllegalStateException("No entity-free decoration among " + String.join(", ", DECORATIONS));
    }

    /**
     * For failure messages: a builder's open requests (all types), the resources his hut still needs and his inventory.
     */
    public static String describeBuilder(final IBuilding hut, final com.minecolonies.api.colony.ICitizenData citizen)
    {
        final StringBuilder text = new StringBuilder(ColonyFixture.describe(citizen));
        final List<IRequest<?>> requests = new ArrayList<>(hut.getOpenRequests(-1));
        if (citizen != null)
        {
            requests.addAll(hut.getOpenRequests(citizen.getId()));
        }
        text.append("; open requests: ");
        for (final IRequest<?> request : requests)
        {
            text.append(request.getShortDisplayString().getString()).append(" [").append(request.getState()).append("], ");
        }
        if (hut instanceof final com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder structureBuilder)
        {
            text.append("; still needed: ");
            structureBuilder.getNeededResources().values().stream().filter(r -> r.getAmount() > 0)
              .forEach(r -> text.append(r.getAmount()).append("x").append(r.getItemStack().getItem()).append(r.getItemStack().getComponentsPatch().isEmpty() ? "" : r.getItemStack().getComponentsPatch()).append(", "));
        }
        if (citizen != null)
        {
            text.append("; inventory: ");
            for (int i = 0; i < citizen.getInventory().getSlots(); i++)
            {
                final ItemStack stack = citizen.getInventory().getStackInSlot(i);
                if (!stack.isEmpty())
                {
                    text.append(stack.getCount()).append("x").append(stack.getItem()).append(", ");
                }
            }
        }
        return text.toString();
    }
}
