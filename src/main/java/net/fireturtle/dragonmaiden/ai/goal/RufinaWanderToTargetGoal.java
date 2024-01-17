package net.fireturtle.dragonmaiden.ai.goal;

import net.fireturtle.dragonmaiden.AbstractRufinaEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class RufinaWanderToTargetGoal
        extends Goal {
    final AbstractRufinaEntity trader;
    final double proximityDistance;
    final double speed;

    public RufinaWanderToTargetGoal(AbstractRufinaEntity trader, double proximityDistance, double speed) {
        this.trader = trader;
        this.proximityDistance = proximityDistance;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public void stop() {
        this.trader.setWanderTarget(null);
        this.trader.getNavigation().stop();
    }

    @Override
    public boolean canStart() {
        BlockPos blockPos = this.trader.getWanderTarget();
        return blockPos != null && this.isTooFarFrom(blockPos, this.proximityDistance) && !this.trader.isTamed();
    }

    @Override
    public void tick() {
        BlockPos blockPos = this.trader.getWanderTarget();
        if (blockPos != null && this.trader.getNavigation().isIdle()) {
            if (this.isTooFarFrom(blockPos, 10.0)) {
                Vec3d vec3d = new Vec3d((double)blockPos.getX() - this.trader.getX(), (double)blockPos.getY() - this.trader.getY(), (double)blockPos.getZ() - this.trader.getZ()).normalize();
                Vec3d vec3d2 = vec3d.multiply(10.0).add(this.trader.getX(), this.trader.getY(), this.trader.getZ());
                this.trader.getNavigation().startMovingTo(vec3d2.x, vec3d2.y, vec3d2.z, this.speed);
            } else {
                this.trader.getNavigation().startMovingTo(blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.speed);
            }
        }
    }

    private boolean isTooFarFrom(BlockPos pos, double proximityDistance) {
        return !pos.isWithinDistance(this.trader.getPos(), proximityDistance);
    }
}