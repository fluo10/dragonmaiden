package net.fireturtle.rufina_mc;

import net.fireturtle.rufina_mc.ai.goal.*;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.ServerConfigHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.EnumSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;


public abstract class AbstractRufinaEntity extends PassiveEntity implements Tameable, Angerable, CrossbowUser, InventoryOwner{
    protected static final TrackedData<Byte> RUFINA_FLAGS = DataTracker.registerData(AbstractRufinaEntity.class, TrackedDataHandlerRegistry.BYTE);
    protected static final int TAMED_FLAG = 2;
    protected static final int SADDLED_FLAG = 4;
    protected static final TrackedData<Optional<UUID>> OWNER_UUID = DataTracker.registerData(AbstractRufinaEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> CHARGING = DataTracker.registerData(AbstractRufinaEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ANGER_TIME;
    protected static final TrackedData<Boolean> CONVERTING = DataTracker.registerData(AbstractRufinaEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> LOYAL_TIME = DataTracker.registerData(AbstractRufinaEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> ALLOWED_TRANSFORMATION_FLAG = DataTracker.registerData(AbstractRufinaEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    protected static final TrackedData<Boolean> SADDLE_FLAG = DataTracker.registerData(AbstractRufinaEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    

    
    public static final Predicate<LivingEntity> FOLLOW_TAMED_PREDICATE;
    private static final float WILD_MAX_HEALTH = 8.0F;
    private static final float TAMED_MAX_HEALTH = 20.0F;
    private float begAnimationProgress;
    private float lastBegAnimationProgress;
    private boolean furWet;
    private boolean canShakeWaterOff;
    private float shakeProgress;
    private float lastShakeProgress;
    private static final UniformIntProvider ANGER_TIME_RANGE;
    private int conversionTimer;
    @Nullable
    protected UUID converter;

    @Nullable
    private UUID angryAt;
    protected final SimpleInventory items = new SimpleInventory(36);

    private static final int field_30629 = 5;

    @Nullable
    private BlockPos wanderTarget;
    private int despawnDelay;

    protected static final TrackedData<Byte> PLAYER_MODEL_PARTS = DataTracker.registerData(AbstractRufinaEntity.class, TrackedDataHandlerRegistry.BYTE);

    protected AbstractRufinaEntity(EntityType<?extends PassiveEntity> entityType, World world) {
        super(entityType, world);
        this.onTamedChanged();
        this.setTamed(false);
        this.setPathfindingPenalty(PathNodeType.POWDER_SNOW, -1.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_POWDER_SNOW, -1.0F);
    }

    protected boolean getRufinaFlag(int bitmask) {
        return (this.dataTracker.get(RUFINA_FLAGS) & bitmask) != 0;
    }

    protected void setRufinaFlag(int bitmask, boolean flag) {
        byte b = this.dataTracker.get(RUFINA_FLAGS);
        if (flag) {
            this.dataTracker.set(RUFINA_FLAGS, (byte)(b | bitmask));
        } else {
            this.dataTracker.set(RUFINA_FLAGS, (byte)(b & ~bitmask));
        }
    }

    public EntityView method_48926() {
        return super.getWorld();
    }

    public boolean isTamed() {
        return this.getRufinaFlag(TAMED_FLAG);
    }

    protected void onTamedChanged() {
    }
    @Nullable
    public UUID getOwnerUuid() {
        return (UUID)((Optional)this.dataTracker.get(OWNER_UUID)).orElse((Object)null);
    }

    public void setOwnerUuid(@Nullable UUID uuid) {
        this.dataTracker.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public void setOwner(PlayerEntity player) {
        this.setTamed(true);
        this.setOwnerUuid(player.getUuid());
    }

    public boolean canTarget(LivingEntity target) {
        return this.isOwner(target) ? false : super.canTarget(target);
    }

    public boolean isOwner(LivingEntity entity) {
        return entity == this.getOwner();
    }

    @Nullable
    public AbstractRufinaEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
        return null;
    }
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(1, new AbstractRufinaEntity.WolfEscapeDangerGoal(1.5));
        this.goalSelector.add(4, new PounceAtTargetGoal(this, 0.4F));
        this.goalSelector.add(5, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(6, new RufinaFollowOwnerGoal(this, 1.0, 10.0F, 2.0F, false));
        this.goalSelector.add(8, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(10, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.goalSelector.add(2, new RufinaWanderToTargetGoal(this, 2.0, 0.35));
        this.targetSelector.add(1, new RufinaTrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new RufinaAttackWithOwnerGoal(this));
        this.targetSelector.add(3, (new RevengeGoal(this, new Class[0])).setGroupRevenge(new Class[0]));
        this.targetSelector.add(4, new ActiveTargetGoal<PlayerEntity>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(5, new RufinaUntamedActiveTargetGoal<AnimalEntity>(this, AnimalEntity.class, false, FOLLOW_TAMED_PREDICATE));
        this.targetSelector.add(6, new RufinaUntamedActiveTargetGoal<TurtleEntity>(this, TurtleEntity.class, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
        this.targetSelector.add(7, new ActiveTargetGoal(this, AbstractSkeletonEntity.class, false));
        this.targetSelector.add(8, new UniversalAngerGoal(this, true));

    }

    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30000001192092896).add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0);
    }

    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(RUFINA_FLAGS, (byte) 0);
        this.dataTracker.startTracking(OWNER_UUID, Optional.empty());

        this.dataTracker.startTracking(CHARGING, false);
        this.dataTracker.startTracking(ANGER_TIME, 0);
        this.dataTracker.startTracking(PLAYER_MODEL_PARTS, (byte)127);
        this.dataTracker.startTracking(CONVERTING, false);
    }

    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_WOLF_STEP, 0.15F, 1.0F);
    }

    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.getOwnerUuid() != null) {
            nbt.putUuid("Owner", this.getOwnerUuid());
        }
        nbt.putByte("RufinaFlags", this.dataTracker.get(RUFINA_FLAGS));
        this.writeAngerToNbt(nbt);
    }

    public void readCustomDataFromNbt(NbtCompound nbt) {
        UUID uUID;
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("RufinaFlags")) {
            this.dataTracker.set(RUFINA_FLAGS, nbt.getByte("RufinaFlags"));
        }
        
        if (nbt.containsUuid("Owner")) {
            uUID = nbt.getUuid("Owner");
        } else {
            String string = nbt.getString("Owner");
            uUID = ServerConfigHandler.getPlayerUuidByName(this.getServer(), string);
        }

        if (uUID != null) {
            try {
                this.setOwnerUuid(uUID);
                this.setTamed(true);
            } catch (Throwable throwable) {
                this.setTamed(false);
            }
        }
        
        this.readAngerFromNbt(this.getWorld(), nbt);
    }

    protected SoundEvent getAmbientSound() {
        if (this.hasAngerTime()) {
            return SoundEvents.ENTITY_WOLF_GROWL;
        } else if (this.random.nextInt(3) == 0) {
            return this.isTamed() && this.getHealth() < 10.0F ? SoundEvents.ENTITY_WOLF_WHINE : SoundEvents.ENTITY_WOLF_PANT;
        } else {
            return SoundEvents.ENTITY_WOLF_AMBIENT;
        }
    }

    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WOLF_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WOLF_DEATH;
    }

    protected float getSoundVolume() {
        return 0.4F;
    }

    public void tickMovement() {
        super.tickMovement();
        if (!this.getWorld().isClient && this.furWet && !this.canShakeWaterOff && !this.isNavigating() && this.isOnGround()) {
            this.canShakeWaterOff = true;
            this.shakeProgress = 0.0F;
            this.lastShakeProgress = 0.0F;
            this.getWorld().sendEntityStatus(this, (byte)8);
        }

        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld)this.getWorld(), true);
        }

    }

    public void tick() {
        
        if (!this.getWorld().isClient && this.isAlive() && this.isConverting()) {
            this.conversionTimer -= 1;
            if (this.conversionTimer <= 0) {
                this.finishConversion((ServerWorld)this.getWorld());
            }
        }
        super.tick();
        if (this.isAlive()) {
            this.lastBegAnimationProgress = this.begAnimationProgress;

            if (this.isWet()) {
                this.furWet = true;
                if (this.canShakeWaterOff && !this.getWorld().isClient) {
                    this.getWorld().sendEntityStatus(this, (byte)56);
                    this.resetShake();
                }
            } else if ((this.furWet || this.canShakeWaterOff) && this.canShakeWaterOff) {
                if (this.shakeProgress == 0.0F) {
                    this.playSound(SoundEvents.ENTITY_WOLF_SHAKE, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                    this.emitGameEvent(GameEvent.ENTITY_ACTION);
                }

                this.lastShakeProgress = this.shakeProgress;
                this.shakeProgress += 0.05F;
                if (this.lastShakeProgress >= 2.0F) {
                    this.furWet = false;
                    this.canShakeWaterOff = false;
                    this.lastShakeProgress = 0.0F;
                    this.shakeProgress = 0.0F;
                }

                if (this.shakeProgress > 0.4F) {
                    float f = (float)this.getY();
                    int i = (int)(MathHelper.sin((this.shakeProgress - 0.4F) * 3.1415927F) * 7.0F);
                    Vec3d vec3d = this.getVelocity();

                    for(int j = 0; j < i; ++j) {
                        float g = (this.random.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;
                        float h = (this.random.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;
                        this.getWorld().addParticle(ParticleTypes.SPLASH, this.getX() + (double)g, (double)(f + 0.8F), this.getZ() + (double)h, vec3d.x, vec3d.y, vec3d.z);
                    }
                }
            }

        }
    }

    private void resetShake() {
        this.canShakeWaterOff = false;
        this.shakeProgress = 0.0F;
        this.lastShakeProgress = 0.0F;
    }

    public void onDeath(DamageSource damageSource) {
        this.furWet = false;
        this.canShakeWaterOff = false;
        this.lastShakeProgress = 0.0F;
        this.shakeProgress = 0.0F;
        super.onDeath(damageSource);
    }

    public boolean isFurWet() {
        return this.furWet;
    }

    public float getFurWetBrightnessMultiplier(float tickDelta) {
        return Math.min(0.5F + MathHelper.lerp(tickDelta, this.lastShakeProgress, this.shakeProgress) / 2.0F * 0.5F, 1.0F);
    }

    public float getShakeAnimationProgress(float tickDelta, float f) {
        float g = (MathHelper.lerp(tickDelta, this.lastShakeProgress, this.shakeProgress) + f) / 1.8F;
        if (g < 0.0F) {
            g = 0.0F;
        } else if (g > 1.0F) {
            g = 1.0F;
        }

        return MathHelper.sin(g * 3.1415927F) * MathHelper.sin(g * 3.1415927F * 11.0F) * 0.15F * 3.1415927F;
    }

    public float getBegAnimationProgress(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.lastBegAnimationProgress, this.begAnimationProgress) * 0.15F * 3.1415927F;
    }

    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return dimensions.height * 1.6F;
    }

    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            Entity entity = source.getAttacker();

            if (entity != null && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity)) {
                amount = (amount + 1.0F) / 2.0F;
            }

            return super.damage(source, amount);
        }
    }

    public boolean tryAttack(Entity target) {
        boolean bl = target.damage(this.getDamageSources().mobAttack(this), (float)((int)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)));
        if (bl) {
            this.applyDamageEffects(this, target);
        }

        return bl;
    }

    public void setTamed(boolean tamed) {
        this.setRufinaFlag(TAMED_FLAG, tamed);
    }

    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        Item item = itemStack.getItem();
        if (this.isOwner(player) && player.shouldCancelInteraction()) {
            if (!this.getWorld().isClient) {
                this.setConverting(player.getUuid(), 60);
            }
            return ActionResult.SUCCESS;
        }
        if (this.getWorld().isClient) {
            boolean bl = this.isOwner(player) || this.isTamed() || itemStack.isOf(Items.GOLDEN_APPLE) && !this.isTamed() && !this.hasAngerTime();
            return bl ? ActionResult.CONSUME : ActionResult.PASS;
        } else {
            label90: {
                if (this.isTamed()) {
                    if (this.isBreedingItem(itemStack) && this.getHealth() < this.getMaxHealth()) {
                        if (!player.getAbilities().creativeMode) {
                            itemStack.decrement(1);
                        }

                        this.heal((float)item.getFoodComponent().getHunger());
                        return ActionResult.SUCCESS;
                    }

                        break label90;

                } else if (itemStack.isOf(Items.GOLDEN_APPLE) && !this.hasAngerTime()) {
                    if (!player.getAbilities().creativeMode) {
                        itemStack.decrement(1);
                    }

                        this.setOwner(player);
                        this.navigation.stop();
                        this.setTarget((LivingEntity)null);
                        this.getWorld().sendEntityStatus(this, (byte)7);

                    return ActionResult.SUCCESS;
                }

                return super.interactMob(player, hand);
            }

            ActionResult actionResult = super.interactMob(player, hand);
            if ((!actionResult.isAccepted() || this.isBaby()) && this.isOwner(player)) {
                this.jumping = false;
                this.navigation.stop();
                this.setTarget((LivingEntity)null);
                return ActionResult.SUCCESS;
            } else {
                return actionResult;
            }
        }
    }

    public void handleStatus(byte status) {
        if (status == 8) {
            this.canShakeWaterOff = true;
            this.shakeProgress = 0.0F;
            this.lastShakeProgress = 0.0F;
        } else if (status == 56) {
            this.resetShake();
        } else if (status == EntityStatuses.PLAY_CURE_ZOMBIE_VILLAGER_SOUND) {
            if (!this.isSilent()) {
                    this.getWorld().playSound(this.getX(), this.getEyeY(), this.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, this.getSoundCategory(), 1.0f + this.random.nextFloat(), this.random.nextFloat() * 0.7f + 0.3f, false);
                }
                return;
        } else {
            super.handleStatus(status);
        }

    }

    public float getTailAngle() {
        if (this.hasAngerTime()) {
            return 1.5393804F;
        } else {
            return this.isTamed() ? (0.55F - (this.getMaxHealth() - this.getHealth()) * 0.02F) * 3.1415927F : 0.62831855F;
        }
    }

    public boolean isBreedingItem(ItemStack stack) {
        Item item = stack.getItem();
        return item.isFood() && item.getFoodComponent().isMeat();
    }

    public int getLimitPerChunk() {
        return 8;
    }

    public int getAngerTime() {
        return (Integer)this.dataTracker.get(ANGER_TIME);
    }

    public void setAngerTime(int angerTime) {
        this.dataTracker.set(ANGER_TIME, angerTime);
    }

    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGER_TIME_RANGE.get(this.random));
    }

    @Nullable
    public UUID getAngryAt() {
        return this.angryAt;
    }

    public void setAngryAt(@Nullable UUID angryAt) {
        this.angryAt = angryAt;
    }

    public boolean shouldAngerAt(LivingEntity entity) {
        if (!this.canTarget(entity)) {
            return false;
        }
        if (entity.getType() == EntityType.PLAYER && this.isUniversallyAngry(entity.getWorld())) {
            return true;
        }
        return entity.getUuid().equals(this.getAngryAt());
    }




    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        if (!(target instanceof CreeperEntity) && !(target instanceof GhastEntity)) {
            if (target instanceof WolfEntity) {
                WolfEntity wolfEntity = (WolfEntity)target;
                return !wolfEntity.isTamed() || wolfEntity.getOwner() != owner;
            } else if (target instanceof PlayerEntity && owner instanceof PlayerEntity && !((PlayerEntity)owner).shouldDamagePlayer((PlayerEntity)target)) {
                return false;
            } else if (target instanceof AbstractHorseEntity && ((AbstractHorseEntity)target).isTame()) {
                return false;
            } else {
                return !(target instanceof TameableEntity) || !((TameableEntity)target).isTamed();
            }
        } else {
            return false;
        }
    }

    public boolean canBeLeashedBy(PlayerEntity player) {
        return !this.hasAngerTime() && super.canBeLeashedBy(player);
    }

    public Vec3d getLeashOffset() {
        return new Vec3d(0.0, (double)(0.6F * this.getStandingEyeHeight()), (double)(this.getWidth() * 0.4F));
    }

    protected Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0F, dimensions.height - 0.03125F * scaleFactor, -0.0625F * scaleFactor);
    }

    public static boolean canSpawn(EntityType<AbstractRufinaEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        return world.getBlockState(pos.down()).isIn(BlockTags.WOLVES_SPAWNABLE_ON);
    }

    static {
        ANGER_TIME = DataTracker.registerData(AbstractRufinaEntity.class, TrackedDataHandlerRegistry.INTEGER);
        FOLLOW_TAMED_PREDICATE = (entity) -> {
            EntityType<?> entityType = entity.getType();
            return entityType == EntityType.SHEEP || entityType == EntityType.RABBIT || entityType == EntityType.FOX;
        };
        ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
    }
private boolean isCharging() {
        return this.dataTracker.get(CHARGING);
}
    @Override
    public void setCharging(boolean charging) {
        this.dataTracker.set(CHARGING, charging);
    }

    @Override
    public void shoot(LivingEntity target, ItemStack crossbow, ProjectileEntity projectile, float multiShotSpray) {
        this.shoot(this, target, projectile, multiShotSpray, 1.6f);
    }

    @Override
    public void postShoot() {

    }

    @Override
    public void shootAt(LivingEntity target, float pullProgress) {
        this.shoot(this, 1.6f);
    }

    public boolean canUseRangedWeapon(RangedWeaponItem weapon) {
        return weapon == Items.CROSSBOW;
    }

    @Override
    public SimpleInventory getInventory() {
        return this.items;
    }

    class WolfEscapeDangerGoal extends EscapeDangerGoal {
        public WolfEscapeDangerGoal(double speed) {
            super(AbstractRufinaEntity.this, speed);
        }

        protected boolean isInDanger() {
            return this.mob.shouldEscapePowderSnow() || this.mob.isOnFire();
        }
    }
    protected void equipToMainHand(ItemStack stack){
        this.equipLootStack(EquipmentSlot.MAINHAND, stack);
    }
    protected boolean canEquipStack(ItemStack stack) {
        EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(stack);
        ItemStack itemStack = this.getEquippedStack(equipmentSlot);
        return this.preferesNewEquipment(stack, itemStack);
    }
    protected boolean preferesNewEquipment(ItemStack newStack, ItemStack oldStack) {
        boolean bl2;
        if (EnchantmentHelper.hasBindingCurse(oldStack)) {
            return false;
        }
        boolean bl = newStack.isOf(Items.CROSSBOW);
        boolean bl3 = bl2 = oldStack.isOf(Items.CROSSBOW);
        if (bl && !bl2) {
            return true;
        }
        if (!bl && bl2) {
            return false;
        }
        return false;
    }

    @Nullable
    public BlockPos getWanderTarget() {
        return this.wanderTarget;
    }

    public void setDespawnDelay(int despawnDelay) {
        this.despawnDelay = despawnDelay;
    }

    public int getDespawnDelay() {
        return this.despawnDelay;
    }

    private void tickDespawnDelay() {
        //if (this.despawnDelay > 0 && !this.hasCustomer() && --this.despawnDelay == 0) {
        if (this.despawnDelay > 0 && !this.isTamed() && --this.despawnDelay == 0) {
            this.discard();
        }
    }
    public void setWanderTarget(@Nullable BlockPos wanderTarget) {
        this.wanderTarget = wanderTarget;
    }
    public boolean isPartVisible(PlayerModelPart modelPart) {
        return (this.getDataTracker().get(PLAYER_MODEL_PARTS) & modelPart.getBitFlag()) == modelPart.getBitFlag();
    }
    protected boolean canConvertInWater() {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return !this.isConverting();
    }

    public boolean isConverting() {
        return this.getDataTracker().get(CONVERTING);
    }

    protected void setConverting(@Nullable UUID uuid, int delay) {
        this.converter = uuid;
        this.conversionTimer = delay;
        this.getDataTracker().set(CONVERTING, true);
        this.getWorld().sendEntityStatus(this, EntityStatuses.PLAY_CURE_ZOMBIE_VILLAGER_SOUND);
    }
    
    protected abstract void finishConversion(ServerWorld world);

}


