package com.minecolonies.core.client.render.mobs.drownedpirates;

import com.minecolonies.core.client.render.mobs.AbstractRaiderRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer used for Chief Barbarians.
 */
public class RendererDrownedChiefPirate extends AbstractRendererDrownedPirate<AbstractEntityMinecoloniesMonster, HumanoidModel<HumanoidRenderState>>
{
    /**
     * Texture of the entity.
     */
    private static final Identifier TEXTURE1 = Identifier.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/drowned_pirate_nude.png");


    /**
     * Constructor method for renderer
     *
     * @param context the renderManager
     */
    public RendererDrownedChiefPirate(final EntityRendererProvider.Context context)
    {
        super(context, AbstractRaiderRenderer.innerArmorBodyModel(), 0.5F);
    }

    @NotNull
    @Override
    public Identifier getTextureLocation(final AbstractEntityMinecoloniesMonster entity)
    {
        return TEXTURE1;
    }
}
