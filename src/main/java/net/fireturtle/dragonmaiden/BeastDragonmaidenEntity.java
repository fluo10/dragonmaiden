package net.fireturtle.dragonmaiden;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.UnmodifiableIterator;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Dismounting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Saddleable;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BeastDragonmaidenEntity extends AbstractDragonmaidenEntity implements Saddleable{

    protected float jumpStrength;
    protected boolean jumping;
   protected int soundTicks;
   protected boolean inAir;
   private int angryTicks;
   private float angryAnimationProgress;
   private float lastAngryAnimationProgress;

    protected BeastDragonmaidenEntity(EntityType<? extends PassiveEntity> entityType, World world) {
        super(entityType, world);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        boolean isOwner = player.getUuid() == this.getOwnerUuid();
        if (isOwner && !this.isSaddled() && itemStack.isOf(Items.SADDLE)) {
            if (player.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
            this.saddle(null);
        }
        if (isOwner && this.isSaddled() && !player.shouldCancelInteraction()) {
            if (!this.getWorld().isClient) {
                player.startRiding(this);
            }
            return ActionResult.success(this.getWorld().isClient);
        }

        return super.interactMob(player, hand);
    }

    @Override
    protected void finishConversion(ServerWorld world) {
        PlayerEntity playerEntity;
        HumanDragonmaidenEntity rufinaEntity = this.convertTo(Dragonmaiden.HUMAN_DRAGONMAIDEN, false);

        rufinaEntity.initialize(world, world.getLocalDifficulty(rufinaEntity.getBlockPos()), SpawnReason.CONVERSION,
                null, null);
        rufinaEntity.setOwnerUuid(this.getOwnerUuid());
        rufinaEntity.setAngerTime(this.getAngerTime());
        rufinaEntity.setTamed(true);

        // if (super.converter != null && (playerEntity =
        // world.getPlayerByUuid(this.converter)) instanceof ServerPlayerEntity) {
        // Criteria.CURED_ZOMBIE_VILLAGER.trigger((ServerPlayerEntity)playerEntity,
        // this, villagerEntity);
        // world.handleInteraction(EntityInteraction.ZOMBIE_VILLAGER_CURED,
        // playerEntity, villagerEntity);
        // }
        // rufinaEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,
        // 200, 0));
        // if (!this.isSilent()) {
        // world.syncWorldEvent(null, WorldEvents.ZOMBIE_VILLAGER_CURED,
        // this.getBlockPos(), 0);
        // }
    }

    @Override
    public boolean canBeSaddled() {
        return this.isAlive() && !this.isBaby() && this.isTamed();
    }

    @Override
    public void saddle(@Nullable SoundCategory sound) {
        this.items.setStack(0, new ItemStack(Items.SADDLE));
        this.setDragonmaidenFlag(SADDLED_FLAG, true);
    }

    @Override
    public boolean isSaddled() {
        return this.getDragonmaidenFlag(SADDLED_FLAG);
    }

    @Override
    public boolean isFeedingItem(ItemStack stack) {
        Item item = stack.getItem();
        return item.isFood() && item.getFoodComponent().isMeat();
    }
@Override
    protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {

        Dragonmaiden.LOGGER.info("Call tickControlled");
        Dragonmaiden.LOGGER.info("tickControlled: {}", this.isLogicalSideForUpdatingMovement());
        super.tickControlled(controllingPlayer, movementInput);
        Vec2f vec2f = this.getControlledRotation(controllingPlayer);
        this.setRotation(vec2f.y, vec2f.x);
        this.prevYaw = this.bodyYaw = this.headYaw = this.getYaw();
        if (this.isLogicalSideForUpdatingMovement()) {
            if (movementInput.z <= 0.0) {
                this.soundTicks = 0;
            }

            if (this.isOnGround()) {
                this.setInAir(false);
                if (this.jumpStrength > 0.0F && !this.isInAir()) {
                    this.jump(this.jumpStrength, movementInput);
                }

                this.jumpStrength = 0.0F;
            }
        }

    }

    @Override
   protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
      super.updatePassengerPosition(passenger, positionUpdater);
      if (this.lastAngryAnimationProgress > 0.0F) {
         float f = MathHelper.sin(this.bodyYaw * 0.017453292F);
         float g = MathHelper.cos(this.bodyYaw * 0.017453292F);
         float h = 0.7F * this.lastAngryAnimationProgress;
         float i = 0.15F * this.lastAngryAnimationProgress;
         positionUpdater.accept(passenger, this.getX() + (double)(h * f), this.getY() + this.getMountedHeightOffset() + passenger.getHeightOffset() + (double)i, this.getZ() - (double)(h * g));
         if (passenger instanceof LivingEntity) {
            ((LivingEntity)passenger).bodyYaw = this.bodyYaw;
         }
        }
    }


    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity var3 = this.getFirstPassenger();
        if (var3 instanceof MobEntity mobEntity) {
            return mobEntity;
        } else {
            if (this.isSaddled()) {
                var3 = this.getFirstPassenger();
                if (var3 instanceof PlayerEntity) {
                    PlayerEntity playerEntity = (PlayerEntity) var3;
                    return playerEntity;
                }
            }
        }
        return null;
    }
   protected void jump(float strength, Vec3d movementInput) {
        double d = this.getJumpStrength() * (double) strength * (double) this.getJumpVelocityMultiplier();
        double e = d + (double) this.getJumpBoostVelocityModifier();
        Vec3d vec3d = this.getVelocity();
        this.setVelocity(vec3d.x, e, vec3d.z);
        this.setInAir(true);
        this.velocityDirty = true;
        if (movementInput.z > 0.0) {
            float f = MathHelper.sin(this.getYaw() * 0.017453292F);
            float g = MathHelper.cos(this.getYaw() * 0.017453292F);
            this.setVelocity(
                    this.getVelocity().add((double) (-0.4F * f * strength), 0.0, (double) (0.4F * g * strength)));
        }
    }

    public void setJumpStrength(int strength) {
        if (this.isSaddled()) {
            if (strength < 0) {
                strength = 0;
            } else {
                this.jumping = true;
                // this.updateAnger();
            }

            if (strength >= 90) {
                this.jumpStrength = 1.0F;
            } else {
                this.jumpStrength = 0.4F + 0.4F * (float) strength / 90.0F;
            }

        }
    }

    public double getJumpStrength() {
        return this.getAttributeValue(EntityAttributes.HORSE_JUMP_STRENGTH);
    }

    public boolean canJump() {
        return this.isSaddled();
    }

    public void startJumping(int height) {
        this.jumping = true;
        this.playJumpSound();
    }

    public void stopJumping() {
    }

    protected Vec2f getControlledRotation(LivingEntity controllingPassenger) {
        return new Vec2f(controllingPassenger.getPitch() * 0.5F, controllingPassenger.getYaw());
    }

    protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
        if (this.isOnGround() && this.jumpStrength == 0.0F && this.isAngry() && !this.jumping) {
            return Vec3d.ZERO;
        } else {
            float f = controllingPlayer.sidewaysSpeed * 0.5F;
            float g = controllingPlayer.forwardSpeed;
            if (g <= 0.0F) {
                g *= 0.25F;
            }

            return new Vec3d((double) f, 0.0, (double) g);
        }
    }

    protected void playJumpSound() {
        this.playSound(SoundEvents.ENTITY_HORSE_JUMP, 0.4F, 1.0F);
    }

   protected float getSaddledSpeed(PlayerEntity controllingPlayer) {
      return (float)this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
   }
    @Nullable
    private Vec3d locateSafeDismountingPos(Vec3d offset, LivingEntity passenger) {
        double d = this.getX() + offset.x;
        double e = this.getBoundingBox().minY;
        double f = this.getZ() + offset.z;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        UnmodifiableIterator var10 = passenger.getPoses().iterator();

        while (var10.hasNext()) {
            EntityPose entityPose = (EntityPose) var10.next();
            mutable.set(d, e, f);
            double g = this.getBoundingBox().maxY + 0.75;

            while (true) {
                double h = this.getWorld().getDismountHeight(mutable);
                if ((double) mutable.getY() + h > g) {
                    break;
                }

                if (Dismounting.canDismountInBlock(h)) {
                    Box box = passenger.getBoundingBox(entityPose);
                    Vec3d vec3d = new Vec3d(d, (double) mutable.getY() + h, f);
                    if (Dismounting.canPlaceEntityAt(this.getWorld(), passenger, box.offset(vec3d))) {
                        passenger.setPose(entityPose);
                        return vec3d;
                    }
                }

                mutable.move(Direction.UP);
                if (!((double) mutable.getY() < g)) {
                    break;
                }
            }
        }

        return null;
    }

    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        Vec3d vec3d = getPassengerDismountOffset((double) this.getWidth(), (double) passenger.getWidth(),
                this.getYaw() + (passenger.getMainArm() == Arm.RIGHT ? 90.0F : -90.0F));
        Vec3d vec3d2 = this.locateSafeDismountingPos(vec3d, passenger);
        if (vec3d2 != null) {
            return vec3d2;
        } else {
            Vec3d vec3d3 = getPassengerDismountOffset((double) this.getWidth(), (double) passenger.getWidth(),
                    this.getYaw() + (passenger.getMainArm() == Arm.LEFT ? 90.0F : -90.0F));
            Vec3d vec3d4 = this.locateSafeDismountingPos(vec3d3, passenger);
            return vec3d4 != null ? vec3d4 : this.getPos();
        }
    }

    public boolean isInAir() {
        return this.inAir;
    }

    public void setInAir(boolean inAir) {
        this.inAir = inAir;
    }
}
