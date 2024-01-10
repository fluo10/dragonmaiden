package net.fireturtle.rufina_mc;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class BeastRufinaEntityRenderer extends MobEntityRenderer<BeastRufinaEntity, BeastRufinaEntityModel> {
    private static final Identifier WILD_TEXTURE = new Identifier("textures/entity/rufina/beast_rufina.png");
    private static final Identifier TAMED_TEXTURE = new Identifier("textures/entity/rufina/beast_rufina_tamed.png");
    private static final Identifier ANGRY_TEXTURE = new Identifier("textures/entity/rufina/beast_rufina_angry.png");
    //return new Identifier("rufina_mc", "textures/entity/rufina/rufina.png");

    public BeastRufinaEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BeastRufinaEntityModel(context.getPart(EntityModelLayers.WOLF)), 0.5f);
    }

    @Override
    protected float getAnimationProgress(BeastRufinaEntity rufinaEntity, float f) {
        return rufinaEntity.getTailAngle();
    }

    @Override
    public void render(BeastRufinaEntity wolfEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if (wolfEntity.isFurWet()) {
            float h = wolfEntity.getFurWetBrightnessMultiplier(g);
            ((BeastRufinaEntityModel)this.model).setColorMultiplier(h, h, h);
        }
        super.render(wolfEntity, f, g, matrixStack, vertexConsumerProvider, i);
        if (wolfEntity.isFurWet()) {
            ((BeastRufinaEntityModel)this.model).setColorMultiplier(1.0f, 1.0f, 1.0f);
        }
    }

    @Override
    public Identifier getTexture(BeastRufinaEntity wolfEntity) {
        if (wolfEntity.isTamed()) {
            return TAMED_TEXTURE;
        }
        if (wolfEntity.hasAngerTime()) {
            return ANGRY_TEXTURE;
        }
        return WILD_TEXTURE;
    }
}