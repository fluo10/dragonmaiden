package net.fireturtle.rufina_mc.ai.goal;

import java.util.function.Predicate;

import net.fireturtle.rufina_mc.AbstractRufinaEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.passive.TameableEntity;
import org.jetbrains.annotations.Nullable;

/**
 * An active target goal that only starts for untamed tameable animals.
 * In addition, the continue condition for maintaining the target uses the
 * target predicate than that of the standard track target goal.
 */
public class RufinaUntamedActiveTargetGoal<T extends LivingEntity>
        extends ActiveTargetGoal<T> {
    private final AbstractRufinaEntity tameable;

    public RufinaUntamedActiveTargetGoal(AbstractRufinaEntity tameable, Class<T> targetClass, boolean checkVisibility, @Nullable Predicate<LivingEntity> targetPredicate) {
        super(tameable, targetClass, 10, checkVisibility, false, targetPredicate);
        this.tameable = tameable;
    }

    @Override
    public boolean canStart() {
        return !this.tameable.isTamed() && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        if (this.targetPredicate != null) {
            return this.targetPredicate.test(this.mob, this.targetEntity);
        }
        return super.shouldContinue();
    }
}

