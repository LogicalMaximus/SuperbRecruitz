package com.logic.superbrecruits.entity.ai;

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.item.common.ammo.MortarShell;
import com.logic.superbrecruits.mixin.VehicleEntityAccessor;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MortarAttackGoal<T extends AbstractRecruitEntity> extends Goal {

    private static final double MAX_MORTAR_DISTANCE = 3.0;

    private static final double SPREAD = 16;

    private Random random = new Random();

    private final T recruit;

    private LivingEntity target;

    private MortarEntity nearestMortar;

    public MortarAttackGoal(T recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.recruit.getTarget();

        if (livingentity != null && livingentity.isAlive() && this.recruit.getMainHandItem().getItem() instanceof MortarShell && this.recruit.getVehicle() == null) {
            this.target = livingentity;

            boolean canAttack = this.recruit.canAttack(this.target);
            boolean notPassive = this.recruit.getState() != 3;

            return canAttack && notPassive;
        }

        return false;
    }

    @Override
    public void stop() {
        if(!this.recruit.isDeadOrDying()) {
            ItemStack itemStack = this.recruit.getMainHandItem();

            if(itemStack.getItem() instanceof MortarShell) {
                itemStack.getOrCreateTag().remove("TargetX");
                itemStack.getOrCreateTag().remove("TargetY");
                itemStack.getOrCreateTag().remove("TargetZ");
            }
        }

        super.stop();
    }

    @Override
    public void tick() {
        if(nearestMortar == null) {
            List<MortarEntity> mortarEntities = this.recruit.level().getEntitiesOfClass(MortarEntity.class, this.recruit.getBoundingBox().inflate(32));

            if(!mortarEntities.isEmpty()) {
                mortarEntities.sort(Comparator.comparingDouble((e) -> e.distanceToSqr(this.recruit)));

                nearestMortar = mortarEntities.get(0);

            }
        }

        if(this.nearestMortar != null) {
            if(this.recruit.distanceTo(nearestMortar) <= MAX_MORTAR_DISTANCE) {
                ItemStack stack = this.recruit.getMainHandItem();

                if(stack.getItem() instanceof MortarShell) {
                    stack.getOrCreateTag().putDouble("TargetX", this.random.nextDouble(this.target.getX() - SPREAD, this.target.getX() + SPREAD));
                    stack.getOrCreateTag().putDouble("TargetY", this.target.getY());
                    stack.getOrCreateTag().putDouble("TargetZ", this.random.nextDouble(this.target.getZ() - SPREAD, this.target.getZ() + SPREAD));

                    nearestMortar.setTarget(stack, this.recruit);

                    this.recruit.swing(InteractionHand.MAIN_HAND);

                    if (stack.getItem() instanceof MortarShell  && (Integer) nearestMortar.getEntityData().get(MortarEntity.FIRE_TIME) == 0 && (((VehicleEntityAccessor)nearestMortar).getItems().get(0)).isEmpty()) {
                        ((VehicleEntityAccessor)nearestMortar).getItems().set(0, stack.copyWithCount(1));

                        stack.shrink(1);

                        nearestMortar.fire(this.recruit);
                    }
                }
            }
            else {
                this.recruit.getNavigation().moveTo(nearestMortar, 1.0);
            }
        }

        super.tick();
    }
}
