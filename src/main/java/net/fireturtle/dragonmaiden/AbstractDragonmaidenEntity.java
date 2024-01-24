package net.fireturtle.dragonmaiden;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.fireturtle.dragonmaiden.ai.goal.DragonmaidenAttackWithOwnerGoal;
import net.fireturtle.dragonmaiden.ai.goal.DragonmaidenFollowOwnerGoal;
import net.fireturtle.dragonmaiden.ai.goal.DragonmaidenTrackOwnerAttackerGoal;
import net.fireturtle.dragonmaiden.ai.goal.DragonmaidenUntamedActiveTargetGoal;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.PounceAtTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.UniversalAngerGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.ServerConfigHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public abstract class AbstractDragonmaidenEntity extends PassiveEntity implements Tameable, Angerable, InventoryOwner{
    public static final String ANGRY_NBT_KEY = "Angry";
    public static final String CURSED_NBT_KEY = "Cursed";
    public static final String ENCHANTED_NBT_KEY = "Enchanted";
    //public static final String DANCING_NBT_KEY = "Dancing";
    public static final String OWNER_NBT_KEY = "Owner";
    public static final String SADDLED_NBT_KEY = "Saddled";
    public static final String SITTING_NBT_KEY = "Sitting";
    public static final String TRANSFORMATION_TIMER_NBT_KEY = "TransformationTimer";
    public static final String TRANSFORMING_NBT_KEY = "Transforming";
    
    private static final TrackedData<Integer> DRAGONMAIDEN_FLAGS = DataTracker.registerData(AbstractDragonmaidenEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
 
    
    protected static final Integer ANGRY_FLAG = 2;
    protected static final Integer CURSED_FLAG = 4;
    protected static final Integer ENCHANTED_FLAG = 8;
    protected static final Integer DANCING_FLAG = 16;
    protected static final Integer SADDLED_FLAG = 32;
    protected static final Integer SITTING_FLAG = 64;
    protected static final Integer TAMED_FLAG = 128;
    protected static final Integer TRANSFORMING_FLAG = 256;


    protected static final TrackedData<Optional<UUID>> OWNER_UUID = DataTracker.registerData(AbstractDragonmaidenEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> ANGER_TIME;
    
    public static final Predicate<LivingEntity> FOLLOW_TAMED_PREDICATE;
    private static final UniformIntProvider ANGER_TIME_RANGE;
    private int transformationTimer;

    @Nullable
    private UUID angryAt;
    protected final SimpleInventory items = new SimpleInventory(36);


    @Nullable
    private BlockPos wanderTarget;
    private int despawnDelay;
    private int angryTicks;

    protected static final TrackedData<Byte> PLAYER_MODEL_PARTS = DataTracker.registerData(AbstractDragonmaidenEntity.class, TrackedDataHandlerRegistry.BYTE);

    protected AbstractDragonmaidenEntity(EntityType<?extends PassiveEntity> entityType, World world) {
        super(entityType, world);
        this.onTamedChanged();
        this.setTamed(false);
        this.setPathfindingPenalty(PathNodeType.POWDER_SNOW, -1.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_POWDER_SNOW, -1.0F);
    }


   protected boolean getDragonmaidenFlag(int bitmask) {
      return ((Integer)this.dataTracker.get(DRAGONMAIDEN_FLAGS) & bitmask) != 0;
   }

   protected void setDragonmaidenFlag(int bitmask, boolean flag) {
      Integer i = (Integer)this.dataTracker.get(DRAGONMAIDEN_FLAGS);
      if (flag) {
         this.dataTracker.set(DRAGONMAIDEN_FLAGS, (Integer)(i | bitmask));
      } else {
         this.dataTracker.set(DRAGONMAIDEN_FLAGS, (Integer)(i & ~bitmask));
      }

   }

    public EntityView method_48926() {
        return super.getWorld();
    }

    public boolean isTamed() {
        return this.getDragonmaidenFlag(TAMED_FLAG);
    }
    public void setTamed(boolean tamed) {
        this.setDragonmaidenFlag(TAMED_FLAG, tamed);
    }

    public boolean isCursed() {
        return this.getDragonmaidenFlag(CURSED_FLAG);
    }
    public void setCursed(boolean cursed) {
        this.setDragonmaidenFlag(CURSED_FLAG, cursed);
    }

   public boolean isAngry() {
        return this.getDragonmaidenFlag(ANGRY_FLAG);
   }
   public void setAngry(boolean angry) {
        this.setDragonmaidenFlag(ANGRY_FLAG, angry);
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
    public AbstractDragonmaidenEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
        return null;
    }
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(1, new AbstractDragonmaidenEntity.WolfEscapeDangerGoal(1.5));
        this.goalSelector.add(4, new PounceAtTargetGoal(this, 0.4F));
        this.goalSelector.add(5, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(6, new DragonmaidenFollowOwnerGoal(this, 1.0, 10.0F, 2.0F, false));
        this.goalSelector.add(8, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(10, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.targetSelector.add(1, new DragonmaidenTrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new DragonmaidenAttackWithOwnerGoal(this));
        this.targetSelector.add(3, (new RevengeGoal(this, new Class[0])).setGroupRevenge(new Class[0]));
        this.targetSelector.add(4, new ActiveTargetGoal<PlayerEntity>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(5, new DragonmaidenUntamedActiveTargetGoal<AnimalEntity>(this, AnimalEntity.class, false, FOLLOW_TAMED_PREDICATE));
        //this.targetSelector.add(6, new DragonmaidenUntamedActiveTargetGoal<TurtleEntity>(this, TurtleEntity.class, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
        //this.targetSelector.add(7, new ActiveTargetGoal(this, AbstractSkeletonEntity.class, false));
        this.targetSelector.add(8, new UniversalAngerGoal(this, true));

    }

    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30000001192092896).add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0)
        .add(EntityAttributes.HORSE_JUMP_STRENGTH, 2.0);
    }

    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(OWNER_UUID, Optional.empty());


        this.dataTracker.startTracking(ANGER_TIME, 0);
        this.dataTracker.startTracking(PLAYER_MODEL_PARTS, (byte)127);
        this.dataTracker.startTracking(DRAGONMAIDEN_FLAGS, 0);
    }

    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_WOLF_STEP, 0.15F, 1.0F);
    }

    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean(CURSED_NBT_KEY, this.isCursed());
        nbt.putBoolean(ENCHANTED_NBT_KEY, this.getDragonmaidenFlag(ENCHANTED_FLAG));
        if (this.getOwnerUuid() != null) {
            nbt.putUuid(OWNER_NBT_KEY, this.getOwnerUuid());
        }
        nbt.putBoolean(SADDLED_NBT_KEY, this.getDragonmaidenFlag(SADDLED_FLAG));
        nbt.putInt(TRANSFORMATION_TIMER_NBT_KEY, this.transformationTimer);
        this.writeAngerToNbt(nbt);
    }

    public void readCustomDataFromNbt(NbtCompound nbt) {
        UUID uUID;
        super.readCustomDataFromNbt(nbt);

        if (nbt.contains(TRANSFORMATION_TIMER_NBT_KEY)){
            setTransforming(nbt.getInt(TRANSFORMATION_TIMER_NBT_KEY));
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
        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld)this.getWorld(), true);
        }

    }

    public void tick() {
        
        if (!this.getWorld().isClient && this.isAlive() && this.isTransforming()) {
            this.transformationTimer -= 1;
            if (this.transformationTimer <= 0) {
                this.finishTransformation((ServerWorld)this.getWorld());
            }
        }
        super.tick();
    }

    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
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


    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        Item item = itemStack.getItem();
        if (this.isOwner(player) && player.shouldCancelInteraction() && !this.isCursed()) {
            if (!this.getWorld().isClient()) {
                this.setTransforming(60);
            }
            return ActionResult.SUCCESS;
        }
        if (this.getWorld().isClient) {
            boolean bl = this.isOwner(player) || this.isTamed() || itemStack.isOf(Items.GOLDEN_APPLE) && !this.isTamed() && !this.hasAngerTime();
            return bl ? ActionResult.CONSUME : ActionResult.PASS;
        } else {
            label90: {
                if (this.isTamed()) {
                    if (this.isFeedingItem(itemStack) && this.getHealth() < this.getMaxHealth()) {
                        if (!player.getAbilities().creativeMode) {
                            itemStack.decrement(1);
                        }

                        this.heal((float)item.getFoodComponent().getHunger());
                        return ActionResult.SUCCESS;
                    } else if (itemStack.isOf(Items.GOLDEN_APPLE)) {
                      if (!player.getAbilities().creativeMode) {
                        itemStack.decrement(1);
                      }
                      this.setCursed(false);
                      
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
                        this.setCursed(false);

                    return ActionResult.SUCCESS;
                } else if (isFeedingItem(itemStack) && !this.hasAngerTime()) {
                    if (!player.getAbilities().creativeMode) {
                        itemStack.decrement(1);
                    }
                    if (this.random.nextInt(3) == 0) {
                        this.setOwner(player);
                        this.navigation.stop();
                        this.setTarget((LivingEntity)null);
                        this.getWorld().sendEntityStatus(this, (byte)7);
                    } else {
                        this.getWorld().sendEntityStatus(this, (byte)6);
                     }
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
        if (status == EntityStatuses.PLAY_CURE_ZOMBIE_VILLAGER_SOUND) {
            if (!this.isSilent()) {
                    this.getWorld().playSound(this.getX(), this.getEyeY(), this.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, this.getSoundCategory(), 1.0f + this.random.nextFloat(), this.random.nextFloat() * 0.7f + 0.3f, false);
                }
                return;
        } else {
            super.handleStatus(status);
        }

    }

   public boolean areInventoriesDifferent(Inventory inventory) {
      return this.items != inventory;
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

    public abstract boolean isFeedingItem(ItemStack stack);

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

    public static boolean canSpawn(EntityType<AbstractDragonmaidenEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        return world.getBlockState(pos.down()).isIn(BlockTags.WOLVES_SPAWNABLE_ON);
    }

    static {
        ANGER_TIME = DataTracker.registerData(AbstractDragonmaidenEntity.class, TrackedDataHandlerRegistry.INTEGER);
        FOLLOW_TAMED_PREDICATE = (entity) -> {
            EntityType<?> entityType = entity.getType();
            return entityType == EntityType.SHEEP || entityType == EntityType.RABBIT || entityType == EntityType.FOX;
        };
        ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
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
            super(AbstractDragonmaidenEntity.this, speed);
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
        return !this.isTransforming();
    }

    public boolean isTransforming() {
        return this.getDragonmaidenFlag(TRANSFORMING_FLAG);
    }
    protected void setTransforming(int delay) {
        this.transformationTimer = delay;
        this.getDragonmaidenFlag(TRANSFORMING_FLAG);
        //this.getWorld().sendEntityStatus(this, EntityStatuses.PLAY_CURE_ZOMBIE_VILLAGER_SOUND);
    }
    
    protected abstract void finishTransformation(ServerWorld world);

    @Override
     protected int computeFallDamage(float fallDistance, float damageMultiplier) {
        return 0;
     }
  
     public void updateAnger() {
         if (this.shouldAmbientStand() && this.canMoveVoluntarily()) {
             this.angryTicks = 1;
             this.setAngry(true);
         }
     }
     protected boolean shouldAmbientStand() {
        return true;
     }

 
}


