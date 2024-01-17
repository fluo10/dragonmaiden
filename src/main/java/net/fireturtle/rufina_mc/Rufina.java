package net.fireturtle.rufina_mc;

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


public class Rufina implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("rufina_mc");
	public static final Item HALBERD = new Item(new FabricItemSettings());
	public static final String MOD_ID = "net.fireturtle.rufina_mc";
	public static final Identifier RUFINA_UUID = new Identifier(MOD_ID, "rufina_uuid");
	public static final EntityType<HumanRufinaEntity> HUMAN_RUFINA = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(MOD_ID, "human_rufina"),
			FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, HumanRufinaEntity::new).dimensions(EntityDimensions.fixed(0.75f, 0.75f)).build()
	);
	public static final EntityType<BeastRufinaEntity> BEAST_RUFINA = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(MOD_ID, "beast_rufina"),
			FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, BeastRufinaEntity::new).dimensions(EntityDimensions.fixed(0.75f, 0.75f)).build()
	);


	@Nullable
	public AbstractRufinaEntity rufinaEntity = null;
	
	@Nullable
	public PlayerEntity playerEntity = null;

	@Nullable
	public World rufinaWorld = null;

	public static final Integer DEFAULT_SPAWN_TIMER = 60;
	Integer spawnTimer = DEFAULT_SPAWN_TIMER;

	public static List<?extends AbstractRufinaEntity> getRufinaEntities(MinecraftServer server) {
		Iterable<ServerWorld> worlds = server.getWorlds();
		Stream<?extends AbstractRufinaEntity> rufinaEntities = Stream.empty();
		for (ServerWorld world2 : worlds) {
			rufinaEntities = Stream.concat(rufinaEntities, world2.getEntitiesByType(BEAST_RUFINA, EntityPredicates.VALID_LIVING_ENTITY).stream());
			rufinaEntities = Stream.concat(rufinaEntities, world2.getEntitiesByType(HUMAN_RUFINA, EntityPredicates.VALID_LIVING_ENTITY).stream());
		}
		return rufinaEntities.toList();
	}
	public static boolean trySpawnUnique(MinecraftServer server) {
		if (getRufinaEntities(server).size() == 0) {
			ServerWorld overWorld = server.getOverworld();
			BeastRufinaEntity beastRufinaEntity = BEAST_RUFINA.create(overWorld);
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
		Registry.register(Registries.ITEM, new Identifier("rufina_mc", "halberd"), HALBERD);
		LOGGER.info("Hello Fabric world!");
		FabricDefaultAttributeRegistry.register(HUMAN_RUFINA, HumanRufinaEntity.createMobAttributes());
		FabricDefaultAttributeRegistry.register(BEAST_RUFINA, BeastRufinaEntity.createMobAttributes());


		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			//state = RufinaPersistentState.getServerState(server);
			//if (state.rufinaUuid == null) {
			// 	rufinaWorld = server.getOverworld();
			// 	RufinaEntity rufinaEntity = RUFINA.create(rufinaWorld);
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
			
		});
		ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
			this.rufinaEntity = null;
		});
		ServerTickEvents.END_SERVER_TICK.register((server) -> {
			trySpawnUnique(server);	
		});
	}
}