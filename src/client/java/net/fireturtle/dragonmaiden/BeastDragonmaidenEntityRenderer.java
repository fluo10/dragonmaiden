package net.fireturtle.dragonmaiden;

import net.fireturtle.dragonmaiden.BeastDragonmaidenEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class BeastDragonmaidenEntityRenderer extends MobEntityRenderer<BeastDragonmaidenEntity, BeastDragonmaidenEntityModel> {
    private static final Identifier WILD_TEXTURE = new Identifier(Dragonmaiden.NAMESPACE, "textures/entity/rufina/beast_rufina.png");
    private static final Identifier TAMED_TEXTURE = new Identifier(Dragonmaiden.NAMESPACE, "textures/entity/rufina/beast_rufina.png");
    private static final Identifier ANGRY_TEXTURE = new Identifier(Dragonmaiden.NAMESPACE, "textures/entity/rufina/beast_rufina.png");
    //return new Identifier("rufina_mc", "textures/entity/rufina/rufina.png");

    public BeastDragonmaidenEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BeastDragonmaidenEntityModel(context.getPart(EntityModelLayers.WOLF)), 0.5f);
    }

    @Override
    protected float getAnimationProgress(BeastDragonmaidenEntity rufinaEntity, float f) {
        return rufinaEntity.getTailAngle();
    }

    @Override
    public void render(BeastDragonmaidenEntity wolfEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        super.render(wolfEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }

    @Override
    public Identifier getTexture(BeastDragonmaidenEntity wolfEntity) {
        if (wolfEntity.isTamed()) {
            return TAMED_TEXTURE;
        }
        if (wolfEntity.hasAngerTime()) {
            return ANGRY_TEXTURE;
        }
        return WILD_TEXTURE;
    }
}