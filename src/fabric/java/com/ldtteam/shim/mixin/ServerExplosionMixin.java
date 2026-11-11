package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.event.level.ExplosionEvent;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * ExplosionEvent.Detonate: listeners may remove affected blocks and entities.
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin
{
    @Shadow
    @Final
    private ServerLevel level;

    @Unique
    private List<BlockPos> minecolonies$toBlow;

    @ModifyReturnValue(method = "calculateExplodedPositions", at = @At("RETURN"))
    private List<BlockPos> minecolonies$captureBlocks(final List<BlockPos> original)
    {
        minecolonies$toBlow = new ArrayList<>(original);
        return minecolonies$toBlow;
    }

    @WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> minecolonies$detonate(final ServerLevel instance, final Entity except, final AABB box, final Operation<List<Entity>> original)
    {
        final List<Entity> entities = new ArrayList<>(original.call(instance, except, box));
        minecolonies$fireDetonate(entities);
        return entities;
    }

    @Unique
    private void minecolonies$fireDetonate(final List<Entity> entities)
    {
        NeoForge.EVENT_BUS.post(new ExplosionEvent.Detonate(level, (ServerExplosion) (Object) this, entities, minecolonies$toBlow == null ? new ArrayList<>() : minecolonies$toBlow));
    }
}
