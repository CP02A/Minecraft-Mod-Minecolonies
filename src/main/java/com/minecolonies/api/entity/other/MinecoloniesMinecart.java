package com.minecolonies.api.entity.other;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Special minecolonies minecart that doesn't collide.
 * <p>
 * FABRIC-PORT: in 26.x the rail movement lives in MinecartBehavior; the 1.21.1 copy of the vanilla rail logic (moveAlongTrack/tick) was
 * replaced by the vanilla behavior. Picking up other entities is prevented through {@link #isRideable()}.
 */
public class MinecoloniesMinecart extends Minecart
{
    private static final Vec3 LOWERED_PASSENGER_ATTACHMENT = new Vec3(0.0, 0.0, 0.0);

    /**
     * Constructor to create the minecart.
     *
     * @param type  the entity type.
     * @param world the world.
     */
    public MinecoloniesMinecart(final EntityType<?> type, final Level world)
    {
        super(type, world);
    }

    @Override
    protected void destroy(final ServerLevel level, final DamageSource source)
    {
        this.kill(level);
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location)
    {
        return InteractionResult.FAIL;
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    /**
     * Not rideable in the vanilla sense: prevents the minecart behavior from picking up nearby entities.
     */
    @Override
    public boolean isRideable()
    {
        return false;
    }

    @Override
    public void push(@NotNull final Entity entityIn)
    {
        // Do nothing
    }

    @Override
    public void playerTouch(final Player entityIn)
    {
        // Do nothing
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean canCollideWith(final Entity p_38168_)
    {
        return false;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 20 == 19 && getPassengers().isEmpty())
        {
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    @NotNull
    protected Vec3 getPassengerAttachmentPoint(@NotNull Entity p_294753_, @NotNull EntityDimensions p_295251_, float p_296054_)
    {
        return LOWERED_PASSENGER_ATTACHMENT;
    }
}
