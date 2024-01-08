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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;

import java.util.List;

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
	public static final EntityType<RufinaEntity> RUFINA = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(MOD_ID, "rufina"),
			FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, RufinaEntity::new).dimensions(EntityDimensions.fixed(0.75f, 0.75f)).build()
	);

	@Nullable
	public RufinaEntity rufinaEntity = null;
	
	@Nullable
	public PlayerEntity playerEntity = null;

	@Nullable
	public World rufinaWorld = null;

	public static final Integer DEFAULT_SPAWN_TIMER = 60;
	Integer spawnTimer = DEFAULT_SPAWN_TIMER;

	@Nullable
	RufinaPersistentState state;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(Registries.ITEM, new Identifier("rufina_mc", "halberd"), HALBERD);
		LOGGER.info("Hello Fabric world!");
		FabricDefaultAttributeRegistry.register(RUFINA, RufinaEntity.createMobAttributes());
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
		ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
			this.rufinaEntity = null;
		});
		ServerTickEvents.END_SERVER_TICK.register((server) -> {
			if (this.rufinaEntity == null) {
				LOGGER.info("Spawn Timer: {}", this.spawnTimer);
				if (this.spawnTimer > 0) {
					Iterable<ServerWorld> worlds = server.getWorlds();

					for (ServerWorld world : worlds) {
						List<? extends RufinaEntity> entities = world.getEntitiesByType(RUFINA, EntityPredicates.VALID_LIVING_ENTITY);
						if (entities.size() != 0) {
							this.rufinaWorld = world;
							this.rufinaEntity = entities.get(0);
							LOGGER.info("Found Rufina Entity: {}", this.rufinaEntity.getUuid());
							this.spawnTimer = DEFAULT_SPAWN_TIMER;
							return;
						}
					}
					this.spawnTimer -= 1;
				} else {
					this.rufinaWorld = server.getOverworld();
					this.rufinaEntity = RUFINA.create(this.rufinaWorld);
					BlockPos pos = rufinaWorld.getSpawnPos();
					this.rufinaEntity.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0 , 0.0f);
	
					this.rufinaWorld.spawnEntity(this.rufinaEntity);
					LOGGER.info("Create Rufina Entity: {}", this.rufinaEntity.getUuidAsString());
					
					this.spawnTimer = DEFAULT_SPAWN_TIMER;
				}
			}
		});
	}

}