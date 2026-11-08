package com.minecolonies.core.client.render.worldevent;

import com.ldtteam.structurize.items.ModItems;
import com.ldtteam.structurize.util.WorldRenderMacros;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.claim.IChunkClaimData;
import com.minecolonies.core.MineColonies;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;

public class ColonyBorderRenderer
{
    private static final int RENDER_DIST_THRESHOLD = 3;
    private static final int CHUNK_SIZE = 16;
    private static final int PLAYER_CHUNK_STEP = CHUNK_SIZE / 4;

    // FABRIC-PORT: VertexBuffer is gone in 26.x, the line geometry is cached in recording consumers and replayed each frame.
    private static WorldRenderMacros.RecordingVertexConsumer colonies     = null;
    private static WorldRenderMacros.RecordingVertexConsumer chunktickets = null;
    private static ChunkPos                     lastPlayerChunkPos = null;
    private static IColonyView lastColony = null;

    static void render(final WorldEventContext ctx)
    {
        if (ctx.mainHandItem.getItem() != ModItems.buildTool.get() || !ctx.hasNearestColony())
        {
            return;
        }

        final ChunkPos playerChunkPos = ChunkPos.containing(ctx.clientPlayer.blockPosition());

        if (lastColony != ctx.nearestColony || !lastPlayerChunkPos.equals(playerChunkPos))
        {
            lastColony = ctx.nearestColony;
            lastPlayerChunkPos = playerChunkPos;

            final Map<ChunkPos, Integer> coloniesMap = new HashMap<>();
            final Map<ChunkPos, Integer> chunkticketsMap = new HashMap<>();
            final int nearestColonyId = ctx.nearestColony.getID();
            final int playerRenderDist = Math.max(ctx.clientRenderDist - RENDER_DIST_THRESHOLD, 2);
            final int range = Math.max(ctx.clientRenderDist, MineColonies.getConfig().getServer().maxColonySize.get());

            for (int chunkX = -range; chunkX <= range; chunkX++)
            {
                for (int chunkZ = -range; chunkZ <= range; chunkZ++)
                {
                    final LevelChunk chunk = ctx.clientLevel.getChunk(playerChunkPos.x() + chunkX, playerChunkPos.z() + chunkZ);
                    if (chunk.isEmpty()) { continue; }
                    final ChunkPos chunkPos = chunk.getPos();

                    final IChunkClaimData cap = IColonyManager.getInstance().getClaimData(ctx.nearestColony.getDimension(), chunkPos);;
                    if (cap != null)
                    {
                        coloniesMap.put(chunkPos, cap.getOwningColony());
                    }

                    if (ctx.nearestColony.getTicketedChunks().contains(chunkPos.pack()))
                    {
                        chunkticketsMap.put(chunkPos, nearestColonyId);
                    }
                    else
                    {
                        chunkticketsMap.put(chunkPos, 0);
                    }
                }
            }

            colonies = draw(ctx, coloniesMap, nearestColonyId, playerChunkPos, playerRenderDist);
            chunktickets = draw(ctx, chunkticketsMap, nearestColonyId, playerChunkPos, playerRenderDist);
        }

        final WorldRenderMacros.RecordingVertexConsumer p = Minecraft.getInstance().hasControlDown() ? chunktickets : colonies;
        if (p == null)
        {
            return;
        }

        ctx.pushPoseCameraToPos(lastPlayerChunkPos.getWorldPosition());
        ctx.submitNodeCollector.submitCustomGeometry(ctx.poseStack, WorldRenderMacros.LINES, (pose, consumer) -> p.replay(consumer, pose, 1.0f));
        ctx.popPose();
    }

    /**
     * Vertex consumer with a default color.
     */
    private static final class ColouredBuffer
    {
        private final WorldRenderMacros.RecordingVertexConsumer buffer = new WorldRenderMacros.RecordingVertexConsumer();
        private int defaultColor = 0xffffffff;

        private void addVertex(final float x, final float y, final float z)
        {
            buffer.addVertex(x, y, z).setColor(defaultColor);
        }
    }

    private static WorldRenderMacros.RecordingVertexConsumer draw(final WorldEventContext ctx,
        final Map<ChunkPos, Integer> mapToDraw,
        final int playerColonyId,
        final ChunkPos playerChunkPos,
        final int playerRenderDist)
    {
        final Map<Integer, Integer> colonyColours = new HashMap<>();
        final boolean useColonyColour = IMinecoloniesAPI.getInstance().getConfig().getClient().colonyteamborders.get();

        final ColouredBuffer buf = new ColouredBuffer();
        mapToDraw.forEach((chunkPos, colonyId) -> {
            if (colonyId == 0 || chunkPos.x() <= playerChunkPos.x() - playerRenderDist || chunkPos.x() >= playerChunkPos.x() + playerRenderDist
                || chunkPos.z() <= playerChunkPos.z() - playerRenderDist || chunkPos.z() >= playerChunkPos.z() + playerRenderDist)
            {
                return;
            }

            final boolean isPlayerChunkX = colonyId == playerColonyId && chunkPos.x() == playerChunkPos.x();
            final boolean isPlayerChunkZ = colonyId == playerColonyId && chunkPos.z() == playerChunkPos.z();
            final float minX = chunkPos.getMinBlockX() - playerChunkPos.getMinBlockX();
            final float maxX = chunkPos.getMaxBlockX() - playerChunkPos.getMinBlockX() + 1.0f;
            final float minZ = chunkPos.getMinBlockZ() - playerChunkPos.getMinBlockZ();
            final float maxZ = chunkPos.getMaxBlockZ() - playerChunkPos.getMinBlockZ() + 1.0f;
            final int minY = ctx.clientLevel.getMinY();
            final int maxY = ctx.clientLevel.getMaxY() + 1;
            final int testedColonyId = colonyId;

            if (useColonyColour)
            {
                buf.defaultColor = colonyColours.computeIfAbsent(colonyId, id ->
                {
                    final IColonyView colony = IMinecoloniesAPI.getInstance().getColonyManager().getColonyView(id, ctx.clientLevel.dimension());
                    final ChatFormatting team = colony != null ? colony.getTeamColonyColor()
                            : id == playerColonyId ? ChatFormatting.WHITE : ChatFormatting.RED;
                    final TextColor textColor = TextColor.fromLegacyFormat(team);
                    return (textColor == null ? 0xffffff : textColor.getValue()) | 0xff000000;
                });
            }
            else if (colonyId == playerColonyId)
            {
                buf.defaultColor = 0xffffffff;
            }
            else
            {
                buf.defaultColor = 0xffff4646;
            }

            final boolean north = mapToDraw.getOrDefault(new ChunkPos(chunkPos.x(), chunkPos.z() - 1), -1) != testedColonyId;
            final boolean south = mapToDraw.getOrDefault(new ChunkPos(chunkPos.x(), chunkPos.z() + 1), -1) != testedColonyId;
            final boolean east = mapToDraw.getOrDefault(new ChunkPos(chunkPos.x() + 1, chunkPos.z()), -1) != testedColonyId;
            final boolean west = mapToDraw.getOrDefault(new ChunkPos(chunkPos.x() - 1, chunkPos.z()), -1) != testedColonyId;

            // vert lines
            if (north || west)
            {
                buf.addVertex(minX, minY, minZ);
                buf.addVertex(minX, maxY, minZ);
            }
            if (north || east)
            {
                buf.addVertex(maxX, minY, minZ);
                buf.addVertex(maxX, maxY, minZ);
            }
            if (south || west)
            {
                buf.addVertex(minX, minY, maxZ);
                buf.addVertex(minX, maxY, maxZ);
            }
            if (south || east)
            {
                buf.addVertex(maxX, minY, maxZ);
                buf.addVertex(maxX, maxY, maxZ);
            }

            // horizontal lines
            if (north)
            {
                if (isPlayerChunkX)
                {
                    for (int shift = PLAYER_CHUNK_STEP; shift < CHUNK_SIZE; shift += PLAYER_CHUNK_STEP)
                    {
                        buf.addVertex(minX + shift, minY, minZ);
                        buf.addVertex(minX + shift, maxY, minZ);
                    }
                    for (int y = minY + PLAYER_CHUNK_STEP; y < maxY; y += PLAYER_CHUNK_STEP)
                    {
                        buf.addVertex(minX, y, minZ);
                        buf.addVertex(maxX, y, minZ);
                    }
                }
                else
                {
                    for (int y = minY + CHUNK_SIZE; y < maxY; y += CHUNK_SIZE)
                    {
                        buf.addVertex(minX, y, minZ);
                        buf.addVertex(maxX, y, minZ);
                    }
                }
            }
            if (south)
            {
                if (isPlayerChunkX)
                {
                    for (int shift = PLAYER_CHUNK_STEP; shift < CHUNK_SIZE; shift += PLAYER_CHUNK_STEP)
                    {
                        buf.addVertex(minX + shift, minY, maxZ);
                        buf.addVertex(minX + shift, maxY, maxZ);
                    }
                    for (int y = minY + PLAYER_CHUNK_STEP; y < maxY; y += PLAYER_CHUNK_STEP)
                    {
                        buf.addVertex(minX, y, maxZ);
                        buf.addVertex(maxX, y, maxZ);
                    }
                }
                else
                {
                    for (int y = minY + CHUNK_SIZE; y < maxY; y += CHUNK_SIZE)
                    {
                        buf.addVertex(minX, y, maxZ);
                        buf.addVertex(maxX, y, maxZ);
                    }
                }
            }
            if (west)
            {
                if (isPlayerChunkZ)
                {
                    for (int shift = PLAYER_CHUNK_STEP; shift < CHUNK_SIZE; shift += PLAYER_CHUNK_STEP)
                    {
                        buf.addVertex(minX, minY, minZ + shift);
                        buf.addVertex(minX, maxY, minZ + shift);
                    }
                    for (int y = minY + PLAYER_CHUNK_STEP; y < maxY; y += PLAYER_CHUNK_STEP)
                    {
                        buf.addVertex(minX, y, minZ);
                        buf.addVertex(minX, y, maxZ);
                    }
                }
                else
                {
                    for (int y = minY + CHUNK_SIZE; y < maxY; y += CHUNK_SIZE)
                    {
                        buf.addVertex(minX, y, minZ);
                        buf.addVertex(minX, y, maxZ);
                    }
                }
            }
            if (east)
            {
                if (isPlayerChunkZ)
                {
                    for (int shift = PLAYER_CHUNK_STEP; shift < CHUNK_SIZE; shift += PLAYER_CHUNK_STEP)
                    {
                        buf.addVertex(maxX, minY, minZ + shift);
                        buf.addVertex(maxX, maxY, minZ + shift);
                    }
                    for (int y = minY + PLAYER_CHUNK_STEP; y < maxY; y += PLAYER_CHUNK_STEP)
                    {
                        buf.addVertex(maxX, y, minZ);
                        buf.addVertex(maxX, y, maxZ);
                    }
                }
                else
                {
                    for (int y = minY + CHUNK_SIZE; y < maxY; y += CHUNK_SIZE)
                    {
                        buf.addVertex(maxX, y, minZ);
                        buf.addVertex(maxX, y, maxZ);
                    }
                }
            }
        });

        return buf.buffer.isEmpty() ? null : buf.buffer;
    }

    /**
     * Cleanup on logout.
     */
    public static void cleanup()
    {
        colonies = null;
        chunktickets = null;
        lastColony = null;
        lastPlayerChunkPos = null;
    }
}
