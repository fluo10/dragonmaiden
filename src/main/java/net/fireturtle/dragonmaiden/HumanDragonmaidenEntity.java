package net.fireturtle.dragonmaiden;

import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class HumanDragonmaidenEntity extends AbstractDragonmaidenEntity implements CrossbowUser {
    private static final TrackedData<Boolean> CHARGING = DataTracker.registerData(AbstractDragonmaidenEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CHARGING, false);
    }
    protected HumanDragonmaidenEntity(EntityType<? extends PassiveEntity> entityType, World world) {
        super(entityType, world);
        //TODO Auto-generated constructor stub
    }
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack= player.getStackInHand(hand);
        Dragonmaiden.LOGGER.info("ShouldCancelInteraction: {}", player.shouldCancelInteraction());
        if ((player.getUuid() == this.getOwnerUuid())) {
            if (!this.getWorld().isClient) {
                this.setTransforming(60);
            }
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }
    
    @Override
    protected void finishTransformation(ServerWorld world) {
        PlayerEntity playerEntity;
        BeastDragonmaidenEntity rufinaEntity = this.convertTo(Dragonmaiden.BEAST_DRAGONMAIDEN, false);

       
        rufinaEntity.initialize(world, world.getLocalDifficulty(rufinaEntity.getBlockPos()), SpawnReason.CONVERSION, null, null);
        rufinaEntity.setOwnerUuid(this.getOwnerUuid());
        rufinaEntity.setAngerTime(this.getAngerTime());
        rufinaEntity.setTamed(true);


        //if (super.converter != null && (playerEntity = world.getPlayerByUuid(this.converter)) instanceof ServerPlayerEntity) {
        //    Criteria.CURED_ZOMBIE_VILLAGER.trigger((ServerPlayerEntity)playerEntity, this, villagerEntity);
        //    world.handleInteraction(EntityInteraction.ZOMBIE_VILLAGER_CURED, playerEntity, villagerEntity);
        //}
        //rufinaEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
        //if (!this.isSilent()) {
        //    world.syncWorldEvent(null, WorldEvents.ZOMBIE_VILLAGER_CURED, this.getBlockPos(), 0);
        //}
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

    public void shootAt(LivingEntity target, float pullProgress) {
        this.shoot(this, 1.6f);
    }
    private boolean isCharging() {
        return this.dataTracker.get(CHARGING);
    }
   public void attack(LivingEntity target, float pullProgress) {
        this.shootAt(target, pullProgress);
    }
    @Override
    public boolean isFeedingItem(ItemStack stack) {
        Item item = stack.getItem();
        return item.isFood() && (item.getFoodComponent().getStatusEffects().size() == 0);
    }

}
