package net.fireturtle.rufina_mc;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class BeastRufinaEntity extends AbstractRufinaEntity{

    protected BeastRufinaEntity(EntityType<? extends PassiveEntity> entityType, World world) {
        super(entityType, world);
        //TODO Auto-generated constructor stub
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack= player.getStackInHand(hand);
        if ((player.getUuid() == this.getOwnerUuid())) {
            if (!this.getWorld().isClient) {
                this.setConverting(player.getUuid(), 600);

            }
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    @Override
    protected void finishConversion(ServerWorld world) {
        PlayerEntity playerEntity;
        HumanRufinaEntity rufinaEntity = this.convertTo(Rufina.HUMAN_RUFINA, false);


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


}
