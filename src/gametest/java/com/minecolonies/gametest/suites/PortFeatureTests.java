package com.minecolonies.gametest.suites;

import com.ldtteam.domumornamentum.block.decorative.AllBrickBlock;
import com.ldtteam.domumornamentum.block.decorative.TimberFrameBlock;
import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.ldtteam.domumornamentum.entity.block.MateriallyTexturedBlockEntity;
import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.operations.PlaceStructureOperation;
import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.structure.CreativeStructureHandler;
import com.minecolonies.gametest.mixin.EntitySoundInvoker;
import com.minecolonies.gametest.mixin.LivingEntitySoundInvoker;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tests for features the Fabric port had to re-implement (docs/e2e-tests/fabric-port.md, PRT-n):
 * <ul>
 *     <li>PRT-01: Structurize's solid placeholder takes the block the dimension's worldgen material (surface) rule gives at that spot.</li>
 *     <li>PRT-02: Domum Ornamentum blocks sound like their material (place, step and fall sounds).</li>
 * </ul>
 */
public class PortFeatureTests
{
    // --------------------------------------------------------------------------------------------------------------------------------------
    // PRT-01 Solid placeholder follows the worldgen material rule
    // --------------------------------------------------------------------------------------------------------------------------------------

    /**
     * PRT-01: a blueprint with one solid placeholder block ({@code structurize:blocksolidsubstitution}) is pasted (creative build tool
     * placement) into the Nether, on a warped forest and a crimson forest spot. The Nether uses a noise generator with the vanilla Nether
     * material rule, so the placeholder must become what world generation would put there (nylium or wart block on top), not the
     * dimension's default block (netherrack), which was the port's fallback before. Expected blocks come from vanilla's own single-position
     * evaluation ({@code MaterialSystem#topMaterial}). A placeholder buried under netherrack must become netherrack (no top block).
     */
    @GameTest(maxTicks = 100)
    public void prt01_solidPlaceholderFollowsMaterialRule(final GameTestHelper helper)
    {
        final ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "test server has no Nether");
        helper.assertTrue(nether.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator,
          "Nether should use a noise generator, uses " + nether.getChunkSource().getGenerator().getClass().getSimpleName());

        final List<String> found = new ArrayList<>();
        found.add(checkTopPlaceholder(helper, nether, Biomes.WARPED_FOREST, new BlockPos(300_000, 80, 300_000),
          Set.of(Blocks.WARPED_NYLIUM, Blocks.WARPED_WART_BLOCK)));
        found.add(checkTopPlaceholder(helper, nether, Biomes.CRIMSON_FOREST, new BlockPos(-300_000, 80, 300_000),
          Set.of(Blocks.CRIMSON_NYLIUM, Blocks.NETHER_WART_BLOCK)));
        LoggerFactory.getLogger(PortFeatureTests.class).info("PRT-01: {}", found);
        helper.succeed();
    }

    /**
     * Finds a column in {@code biome} where vanilla's material rule puts one of {@code topBlocks} on a floating block, pastes a solid
     * placeholder there and checks the result; then buries the spot and checks again.
     */
    private static String checkTopPlaceholder(
        final GameTestHelper helper,
        final ServerLevel nether,
        final ResourceKey<Biome> biome,
        final BlockPos searchOrigin,
        final Set<Block> topBlocks)
    {
        final Pair<BlockPos, Holder<Biome>> biomePos = nether.findClosestBiome3d(h -> h.is(biome), searchOrigin, 6400, 32, 64);
        helper.assertTrue(biomePos != null, "no " + biome.identifier() + " found near " + searchOrigin);

        final BlockPos base = biomePos.getFirst().atY(searchOrigin.getY());
        BlockPos spot = null;
        BlockState expected = null;
        for (int i = 0; i < 64 && spot == null; i++)
        {
            final BlockPos candidate = base.offset((i % 8) * 2, 0, (i / 8) * 2);
            if (!nether.getBiome(candidate).is(biome))
            {
                continue;
            }
            final BlockState vanilla = vanillaTopMaterial(nether, candidate).orElse(null);
            if (vanilla != null && topBlocks.contains(vanilla.getBlock()))
            {
                spot = candidate;
                expected = vanilla;
            }
        }
        helper.assertTrue(spot != null, "no spot near " + base + " where vanilla's material rule gives " + topBlocks);

        // Floating spot: air above and below (the context vanilla's single-position evaluation assumes).
        nether.setBlockAndUpdate(spot.above(), Blocks.AIR.defaultBlockState());
        nether.setBlockAndUpdate(spot, Blocks.AIR.defaultBlockState());
        nether.setBlockAndUpdate(spot.below(), Blocks.AIR.defaultBlockState());
        pasteSolidPlaceholder(nether, spot);
        final BlockState top = nether.getBlockState(spot);
        helper.assertTrue(top.equals(expected),
          "placeholder on the surface in " + biome.identifier() + " at " + spot + " became " + top + ", expected " + expected);

        // Buried spot: netherrack above and below, so it is no longer the floor.
        for (int dy = 1; dy <= 3; dy++)
        {
            nether.setBlockAndUpdate(spot.above(dy), Blocks.NETHERRACK.defaultBlockState());
            nether.setBlockAndUpdate(spot.below(dy), Blocks.NETHERRACK.defaultBlockState());
        }
        nether.setBlockAndUpdate(spot, Blocks.AIR.defaultBlockState());
        pasteSolidPlaceholder(nether, spot);
        final BlockState buried = nether.getBlockState(spot);
        helper.assertTrue(buried.is(Blocks.NETHERRACK),
          "buried placeholder in " + biome.identifier() + " at " + spot + " became " + buried + ", expected netherrack");

        return biome.identifier().getPath() + " " + spot + " -> " + top.getBlock() + " / buried " + buried.getBlock();
    }

    /**
     * Vanilla's own evaluation of the dimension's material rule at one floating position ({@code MaterialSystem#topMaterial}, used by
     * the carvers to replace exposed dirt).
     */
    @SuppressWarnings("deprecation")
    private static Optional<BlockState> vanillaTopMaterial(final ServerLevel level, final BlockPos pos)
    {
        final NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        final RandomState randomState = level.getChunkSource().randomState();
        final MaterialRule rule = generator.generatorSettings().value().materialRule().value();
        return randomState.surfaceSystem().topMaterial(rule,
          randomState,
          new WorldGenerationContext(generator, level),
          level.getBiomeManager()::getBiome,
          level.getChunk(pos),
          randomState.samplersWithContext(SamplerContext.EMPTY_UNCACHED),
          pos,
          false);
    }

    /**
     * Pastes a 1x1x1 blueprint holding a solid placeholder at {@code pos}, as the creative build tool does ("pretty" placement).
     */
    private static void pasteSolidPlaceholder(final ServerLevel level, final BlockPos pos)
    {
        final Blueprint blueprint = new Blueprint((short) 1, (short) 1, (short) 1, level.registryAccess());
        blueprint.addBlockState(BlockPos.ZERO, com.ldtteam.structurize.blocks.ModBlocks.blockSolidSubstitution.get().defaultBlockState());
        blueprint.setCachePrimaryOffset(BlockPos.ZERO);
        final CreativeStructureHandler handler = new CreativeStructureHandler(level, pos, blueprint, RotationMirror.NONE, true);
        final PlaceStructureOperation operation = new PlaceStructureOperation(new StructurePlacer(handler), null);
        int steps = 0;
        while (!operation.apply(level))
        {
            if (++steps > 10_000)
            {
                throw new IllegalStateException("pasting the placeholder did not finish");
            }
        }
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // PRT-02 Domum Ornamentum sounds follow the material
    // --------------------------------------------------------------------------------------------------------------------------------------

    private record PlayedSound(ServerLevel level, SoundEvent event, double x, double y, double z, float volume, float pitch)
    {
    }

    private static final List<PlayedSound> SOUNDS = new ArrayList<>();
    private static boolean capturing = false;

    /**
     * Called by {@code SoundCaptureMixin} for every positioned sound the server plays.
     */
    public static void recordSound(final ServerLevel level, final SoundEvent event, final double x, final double y, final double z, final float volume,
        final float pitch)
    {
        if (capturing)
        {
            SOUNDS.add(new PlayedSound(level, event, x, y, z, volume, pitch));
        }
    }

    /**
     * PRT-02: Domum Ornamentum blocks with materials are placed from their item (with the material data, as a player places them) by a fake
     * player, then the player steps on and lands on them. The place, step and fall sounds must be the sounds of the block's main material
     * (NeoForge's positional {@code getSoundType}), not the block's own sound: light brick with moss and with amethyst, and a timber frame
     * whose frame (main component) is deepslate and centre is oak planks.
     */
    @GameTest(maxTicks = 100)
    public void prt02_domumSoundFollowsMaterial(final GameTestHelper helper)
    {
        final com.ldtteam.domumornamentum.block.ModBlocks blocks = com.ldtteam.domumornamentum.block.ModBlocks.getInstance();
        final AllBrickBlock brick = blocks.getAllBrickBlocks().get(0);
        final TimberFrameBlock frame = blocks.getTimberFrames().get(0);

        checkMaterialSound(helper, brick, Map.of(AllBrickBlock.COMPONENTS.get(0).getId(), Blocks.MOSS_BLOCK), SoundType.MOSS, new BlockPos(1, 2, 1));
        checkMaterialSound(helper, brick, Map.of(AllBrickBlock.COMPONENTS.get(0).getId(), Blocks.AMETHYST_BLOCK), SoundType.AMETHYST, new BlockPos(4, 2, 1));
        checkMaterialSound(helper, frame,
          Map.of(TimberFrameBlock.COMPONENTS.get(0).getId(), Blocks.DEEPSLATE, TimberFrameBlock.COMPONENTS.get(1).getId(), Blocks.OAK_PLANKS),
          SoundType.DEEPSLATE, new BlockPos(1, 2, 4));
        helper.succeed();
    }

    private static void checkMaterialSound(
        final GameTestHelper helper,
        final Block block,
        final Map<Identifier, Block> components,
        final SoundType expected,
        final BlockPos rel)
    {
        final ServerLevel level = helper.getLevel();
        final String what = block.getDescriptionId() + " with " + components.values().stream().map(Block::getDescriptionId).collect(Collectors.toList());
        helper.assertTrue(block.defaultBlockState().getSoundType() != expected, what + ": block's own sound is already the expected one");

        helper.setBlock(rel.below(), Blocks.STONE);
        final BlockPos support = helper.absolutePos(rel.below());
        final BlockPos pos = support.above();

        final FakePlayer player = FakePlayer.get(level, new GameProfile(UUID.nameUUIDFromBytes("prt02".getBytes()), "prt02"));
        player.setPos(pos.getX() + 2.5, pos.getY(), pos.getZ() + 2.5);
        final ItemStack stack = new ItemStack(block);
        new MaterialTextureData(components).writeToItemStack(stack);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        // Place: BlockItem#place plays getPlaceSound with the sound type's volume and pitch.
        final List<PlayedSound> placed = capture(() -> {
            final InteractionResult result = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
              new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false)));
            helper.assertTrue(result.consumesAction(), what + ": placing failed: " + result);
        });
        helper.assertTrue(level.getBlockState(pos).is(block), what + ": not placed, found " + level.getBlockState(pos));
        helper.assertTrue(level.getBlockEntity(pos) instanceof final MateriallyTexturedBlockEntity be
                            && be.getTextureData().components().equals(components),
          what + ": block entity has no / other material data");
        assertSound(helper, what + " place", placed, expected.getPlaceSound(), (expected.getVolume() + 1.0F) / 2.0F, expected.getPitch() * 0.8F);

        // Step: what walking over the block calls.
        player.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        final List<PlayedSound> stepped = capture(() -> ((EntitySoundInvoker) player).minecoloniesGametest$playStepSound(pos, level.getBlockState(pos)));
        assertSound(helper, what + " step", stepped, expected.getStepSound(), expected.getVolume() * 0.15F, expected.getPitch());

        // Fall: what landing on the block calls (looks up the block 0.2 below the feet).
        final List<PlayedSound> fell = capture(() -> ((LivingEntitySoundInvoker) player).minecoloniesGametest$playBlockFallSound());
        assertSound(helper, what + " fall", fell, expected.getFallSound(), expected.getVolume() * 0.5F, expected.getPitch() * 0.75F);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private static List<PlayedSound> capture(final Runnable action)
    {
        SOUNDS.clear();
        capturing = true;
        try
        {
            action.run();
        }
        finally
        {
            capturing = false;
        }
        final List<PlayedSound> result = new ArrayList<>(SOUNDS);
        SOUNDS.clear();
        return result;
    }

    private static void assertSound(
        final GameTestHelper helper,
        final String what,
        final List<PlayedSound> played,
        final SoundEvent event,
        final float volume,
        final float pitch)
    {
        final boolean ok = played.stream().anyMatch(s -> s.level() == helper.getLevel() && s.event().equals(event)
                                                           && Math.abs(s.volume() - volume) < 1e-4 && Math.abs(s.pitch() - pitch) < 1e-4);
        helper.assertTrue(ok, what + ": expected " + event.location() + " (volume " + volume + ", pitch " + pitch + "), played "
                                + played.stream().map(s -> s.event().location() + " v" + s.volume() + " p" + s.pitch()).toList());
    }
}
