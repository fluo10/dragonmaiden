package net.fireturtle.rufina_mc;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class RufinaEntityRenderer extends MobEntityRenderer<RufinaEntity, RufinaEntityModel> {

    public RufinaEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new RufinaEntityModel(context.getPart(RufinaClient.MODEL_RUFINA_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(RufinaEntity entity) {
        return new Identifier("rufina", "textures/entity/cube/cube.png");
    }
}