package net.fireturtle.dragonmaiden;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class DragonmaidenClient implements ClientModInitializer {
	public static final EntityModelLayer MODEL_HUMAN_RUFINA_LAYER = new EntityModelLayer(new Identifier("fireturtle", "human_rufina"), "main");
	public static final EntityModelLayer MODEL_BEAST_RUFINA_LAYER = new EntityModelLayer(new Identifier("fireturtle", "beast_rufina"), "main");
	
	@Override
	public void onInitializeClient() {
		/*
		 * Registers our Cube Entity's renderer, which provides a model and texture for the entity.
		 *
		 * Entity Renderers can also manipulate the model before it renders based on entity context (EndermanEntityRenderer#render).
		 */
		EntityRendererRegistry.INSTANCE.register(Dragonmaiden.HUMAN_RUFINA, (context) -> {
			return new HumanDragonmaidenEntityRenderer(context);
		});
		EntityRendererRegistry.INSTANCE.register(Dragonmaiden.BEAST_RUFINA, (context) -> {
			return new BeastDragonmaidenEntityRenderer(context);
		});
		EntityModelLayerRegistry.registerModelLayer(MODEL_HUMAN_RUFINA_LAYER, HumanDragonmaidenEntityModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(MODEL_BEAST_RUFINA_LAYER, BeastDragonmaidenEntityModel::getTexturedModelData);

	}
}