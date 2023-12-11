package net.fireturtle.rufina_mc;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;
import net.minecraft.world.spawner.Spawner;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import java.util.Optional;
        import net.minecraft.entity.EntityType;
        import net.minecraft.entity.SpawnReason;
        import net.minecraft.entity.SpawnRestriction;
        import net.minecraft.entity.passive.TraderLlamaEntity;
        import net.minecraft.entity.passive.WanderingTraderEntity;
        import net.minecraft.registry.tag.BiomeTags;
        import net.minecraft.server.network.ServerPlayerEntity;
        import net.minecraft.server.world.ServerWorld;
        import net.minecraft.util.math.BlockPos;
        import net.minecraft.util.math.MathHelper;
        import net.minecraft.util.math.random.Random;
        import net.minecraft.world.BlockView;
        import net.minecraft.world.GameRules;
        import net.minecraft.world.Heightmap;
        import net.minecraft.world.SpawnHelper;
        import net.minecraft.world.WorldView;
        import net.minecraft.world.level.ServerWorldProperties;
        import net.minecraft.world.poi.PointOfInterestStorage;
        import net.minecraft.world.poi.PointOfInterestTypes;
        import net.minecraft.world.spawner.Spawner;
        import org.jetbrains.annotations.Nullable;

public class RufinaManager
        implements Spawner {
    private static final int DEFAULT_SPAWN_TIMER = 1200;
    public static final int DEFAULT_SPAWN_DELAY = 24000;
    private static final int MIN_SPAWN_CHANCE = 25;
    private static final int MAX_SPAWN_CHANCE = 75;
    private static final int DEFAULT_SPAWN_CHANCE = 25;
    private static final int field_30635 = 10;
    private static final int field_30636 = 10;
    private final Random random = Random.create();
    private final ServerWorldProperties properties;
    private int spawnTimer;
    private int spawnDelay;
    private int spawnChance;

    public RufinaManager(ServerWorldProperties properties) {
        this.properties = properties;
        this.spawnTimer = 1200;
        this.spawnDelay = properties.getWanderingTraderSpawnDelay();
        this.spawnChance = properties.getWanderingTraderSpawnChance();
        if (this.spawnDelay == 0 && this.spawnChance == 0) {
            this.spawnDelay = 24000;
            properties.setWanderingTraderSpawnDelay(this.spawnDelay);
            this.spawnChance = 25;
            properties.setWanderingTraderSpawnChance(this.spawnChance);
        }
    }

    @Override
    public int spawn(ServerWorld world, boolean spawnMonsters, boolean spawnAnimals) {
        if (!world.getGameRules().getBoolean(GameRules.DO_TRADER_SPAWNING)) {
            return 0;
        }
        if (--this.spawnTimer > 0) {
            return 0;
        }
        this.spawnTimer = 1200;
        this.spawnDelay -= 1200;
        this.properties.setWanderingTraderSpawnDelay(this.spawnDelay);
        if (this.spawnDelay > 0) {
            return 0;
        }
        this.spawnDelay = 24000;
        if (!world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
            return 0;
        }
        int i = this.spawnChance;
        this.spawnChance = MathHelper.clamp(this.spawnChance + 25, 25, 75);
        this.properties.setWanderingTraderSpawnChance(this.spawnChance);
        if (this.random.nextInt(100) > i) {
            return 0;
        }
        if (this.trySpawn(world)) {
            this.spawnChance = 25;
            return 1;
        }
        return 0;
    }

    private boolean trySpawn(ServerWorld world) {
        ServerPlayerEntity playerEntity = world.getRandomAlivePlayer();
        if (playerEntity == null) {
            return true;
        }
        if (this.random.nextInt(10) != 0) {
            return false;
        }
        BlockPos blockPos = playerEntity.getBlockPos();
        int i = 48;
        PointOfInterestStorage pointOfInterestStorage = world.getPointOfInterestStorage();
        Optional<BlockPos> optional = pointOfInterestStorage.getPosition(poiType -> poiType.matchesKey(PointOfInterestTypes.MEETING), pos -> true, blockPos, 48, PointOfInterestStorage.OccupationStatus.ANY);
        BlockPos blockPos2 = optional.orElse(blockPos);
        BlockPos blockPos3 = this.getNearbySpawnPos(world, blockPos2, 48);
        if (blockPos3 != null && this.doesNotSuffocateAt(world, blockPos3)) {
            if (world.getBiome(blockPos3).isIn(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)) {
                return false;
            }
            RufinaEntity RufinaEntity = Rufina.RUFINA.spawn(world, blockPos3, SpawnReason.EVENT);
            if (RufinaEntity != null) {
                for (int j = 0; j < 2; ++j) {
                    this.spawnLlama(world, RufinaEntity, 4);
                }
                this.properties.setWanderingTraderId(RufinaEntity.getUuid());
                RufinaEntity.setDespawnDelay(48000);
                RufinaEntity.setWanderTarget(blockPos2);
                RufinaEntity.setPositionTarget(blockPos2, 16);
                return true;
            }
        }
        return false;
    }

    private void spawnLlama(ServerWorld world, RufinaEntity wanderingTrader, int range) {
        BlockPos blockPos = this.getNearbySpawnPos(world, wanderingTrader.getBlockPos(), range);
        if (blockPos == null) {
            return;
        }
        TraderLlamaEntity traderLlamaEntity = EntityType.TRADER_LLAMA.spawn(world, blockPos, SpawnReason.EVENT);
        if (traderLlamaEntity == null) {
            return;
        }
        traderLlamaEntity.attachLeash(wanderingTrader, true);
    }

    @Nullable
    private BlockPos getNearbySpawnPos(WorldView world, BlockPos pos, int range) {
        BlockPos blockPos = null;
        for (int i = 0; i < 10; ++i) {
            int k;
            int l;
            int j = pos.getX() + this.random.nextInt(range * 2) - range;
            BlockPos blockPos2 = new BlockPos(j, l = world.getTopY(Heightmap.Type.WORLD_SURFACE, j, k = pos.getZ() + this.random.nextInt(range * 2) - range), k);
            if (!SpawnHelper.canSpawn(SpawnRestriction.Location.ON_GROUND, world, blockPos2, EntityType.WANDERING_TRADER)) continue;
            blockPos = blockPos2;
            break;
        }
        return blockPos;
    }

    private boolean doesNotSuffocateAt(BlockView world, BlockPos pos) {
        for (BlockPos blockPos : BlockPos.iterate(pos, pos.add(1, 2, 1))) {
            if (world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty()) continue;
            return false;
        }
        return true;
    }
}