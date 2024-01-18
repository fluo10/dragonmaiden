package net.fireturtle.dragonmaiden;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Dragonmaiden implements ModInitializer {
	public static final String MOD_NAME = "dragonmaiden";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
	public static final String MOD_ID = "net.fireturtle.dragonmaiden";
	public static final String NAMESPACE = "net-fireturtle-dragonmaiden";
	public static final EntityType<HumanDragonmaidenEntity> HUMAN_DRAGONMAIDEN = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(NAMESPACE, "human_dragonmaiden"),
			FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, HumanDragonmaidenEntity::new).dimensions(EntityDimensions.fixed(0.75f, 0.75f)).build()
	);
	public static final EntityType<BeastDragonmaidenEntity> BEAST_DRAGONMAIDEN = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(NAMESPACE, "beast_dragonmaiden"),
			FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, BeastDragonmaidenEntity::new).dimensions(EntityDimensions.fixed(0.75f, 0.75f)).build()
	);

	public static final Integer DEFAULT_SPAWN_TIMER = 60;
	Integer spawnTimer = DEFAULT_SPAWN_TIMER;

	public static boolean dragonmaidenExistsFlag = false;
	public static List<?extends AbstractDragonmaidenEntity> getDragonmaidenEntities(MinecraftServer server) {
		Iterable<ServerWorld> worlds = server.getWorlds();
		Stream<?extends AbstractDragonmaidenEntity> rufinaEntities = Stream.empty();
		for (ServerWorld world2 : worlds) {
			rufinaEntities = Stream.concat(rufinaEntities, world2.getEntitiesByType(BEAST_DRAGONMAIDEN, EntityPredicates.VALID_LIVING_ENTITY).stream());
			rufinaEntities = Stream.concat(rufinaEntities, world2.getEntitiesByType(HUMAN_DRAGONMAIDEN, EntityPredicates.VALID_LIVING_ENTITY).stream());
		}
		return rufinaEntities.toList();
	}
	public static boolean trySpawnUnique(MinecraftServer server) {
		if (getDragonmaidenEntities(server).size() == 0) {
			ServerWorld overWorld = server.getOverworld();
			BeastDragonmaidenEntity beastRufinaEntity = BEAST_DRAGONMAIDEN.create(overWorld);
			BlockPos pos = overWorld.getSpawnPos();
			beastRufinaEntity.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0 , 0.0f);
			overWorld.spawnEntity(beastRufinaEntity);
			LOGGER.info("Create BeastRufinaEntity: {}", beastRufinaEntity.getUuidAsString());	
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onInitialize() {

		FabricDefaultAttributeRegistry.register(HUMAN_DRAGONMAIDEN, HumanDragonmaidenEntity.createMobAttributes());
		FabricDefaultAttributeRegistry.register(BEAST_DRAGONMAIDEN, BeastDragonmaidenEntity.createMobAttributes());

		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			dragonmaidenExistsFlag = false;		
		});
		ServerTickEvents.END_SERVER_TICK.register((server) -> {
			if (!dragonmaidenExistsFlag) {
				trySpawnUnique(server);
				dragonmaidenExistsFlag = true;
			}	
		});
	}
}
