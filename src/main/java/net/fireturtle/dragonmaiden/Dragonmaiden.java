package net.fireturtle.dragonmaiden;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Dragonmaiden implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
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
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		FabricDefaultAttributeRegistry.register(HUMAN_DRAGONMAIDEN, HumanDragonmaidenEntity.createMobAttributes());
		FabricDefaultAttributeRegistry.register(BEAST_DRAGONMAIDEN, BeastDragonmaidenEntity.createMobAttributes());


		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			//state = RufinaPersistentState.getServerState(server);
			//if (state.rufinaUuid == null) {
			// 	rufinaWorld = server.getOverworld();
			// 	RufinaEntity rufinaEntity = DRAGONMAIDEN.create(rufinaWorld);
			// 	BlockPos pos = rufinaWorld.getSpawnPos();
			// 	rufinaEntity.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0 , 0.0f);

			// 	rufinaWorld.spawnEntity(rufinaEntity);
			// 	LOGGER.info("Create Rufina Entity: {}", rufinaEntity.getUuidAsString());
			// 	state.rufinaUuid = rufinaEntity.getUuid();
			// } else {
			// 	Iterable<ServerWorld> worlds = server.getWorlds();

			// 	for (ServerWorld world : worlds) {
			// 		rufinaEntity = (RufinaEntity)world.getEntity(state.rufinaUuid);
			// 		if (rufinaEntity != null) {
			// 			LOGGER.info("Found Rufina Entity: {}", rufinaEntity.getUuidAsString());
			// 			rufinaWorld = world;
			// 			break;
			// 		}
			// 	}
			// 	if (rufinaEntity == null) {
			// 		LOGGER.info("Rufina entiti not found: {}", state.rufinaUuid);
			// 	}
			// }
		});
		ServerWorldEvents.LOAD.register((server, world) -> {
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