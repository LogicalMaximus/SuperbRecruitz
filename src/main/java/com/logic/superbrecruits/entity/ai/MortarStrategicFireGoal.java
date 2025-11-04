package com.logic.superbrecruits.entity.ai;

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.item.common.ammo.MortarShell;
import com.logic.superbrecruits.mixin.VehicleEntityAccessor;
import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MortarStrategicFireGoal extends Goal {
    private static final double MAX_MORTAR_DISTANCE = 3.0;

    private static final double SPREAD = 12;

    private Random random = new Random();

    private BlockPos pos;
    private BowmanEntity bowman;

    private MortarEntity nearestMortar;

    public MortarStrategicFireGoal(BowmanEntity bowman) {
        this.bowman = bowman;
    }

    public boolean canUse() {
        Item item = this.bowman.getMainHandItem().getItem();

        if (this.bowman.getTarget() == null && this.bowman.getShouldStrategicFire()  && this.bowman.getFollowState() != 5 && !this.bowman.getShouldMount() && item instanceof MortarShell) {
            return true;
        } else {
            this.pos = null;
        }

        return false;
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void stop() {
        super.stop();

        if(!this.bowman.isDeadOrDying()) {
            ItemStack itemStack = this.bowman.getMainHandItem();

            if(itemStack.getItem() instanceof MortarShell) {
                itemStack.getOrCreateTag().remove("TargetX");
                itemStack.getOrCreateTag().remove("TargetY");
                itemStack.getOrCreateTag().remove("TargetZ");
            }
        }

        this.bowman.stopUsingItem();
        this.bowman.clearArrowsPos();
    }

    @Override
    public void tick() {
        this.pos = this.bowman.StrategicFirePos();

        if(this.pos == null)return;

        if(nearestMortar == null) {
            List<MortarEntity> mortarEntities = this.bowman.level().getEntitiesOfClass(MortarEntity.class, this.bowman.getBoundingBox().inflate(32));

            if(!mortarEntities.isEmpty()) {
                mortarEntities.sort(Comparator.comparingDouble((e) -> e.distanceToSqr(this.bowman)));

                nearestMortar = mortarEntities.get(0);

            }
        }

        if(this.nearestMortar != null) {
            if(this.bowman.distanceTo(nearestMortar) <= MAX_MORTAR_DISTANCE) {
                ItemStack stack = this.bowman.getMainHandItem();

                if(stack.getItem() instanceof MortarShell) {
                    stack.getOrCreateTag().putDouble("TargetX", this.random.nextDouble(this.pos.getX() - SPREAD, this.pos.getX() + SPREAD));
                    stack.getOrCreateTag().putDouble("TargetY", this.pos.getY());
                    stack.getOrCreateTag().putDouble("TargetZ", this.random.nextDouble(this.pos.getZ() - SPREAD, this.pos.getZ() + SPREAD));

                    nearestMortar.setTarget(stack, this.bowman);

                    this.bowman.swing(InteractionHand.MAIN_HAND);

                    if (stack.getItem() instanceof MortarShell  && (Integer) nearestMortar.getEntityData().get(MortarEntity.FIRE_TIME) == 0 && (((VehicleEntityAccessor)nearestMortar).getItems().get(0)).isEmpty()) {
                        ((VehicleEntityAccessor)nearestMortar).getItems().set(0, stack.copyWithCount(1));

                        stack.shrink(1);

                        nearestMortar.fire(this.bowman);
                    }
                }
            }
            else {
                this.bowman.getNavigation().moveTo(nearestMortar, 1.0);
            }
        }

        super.tick();
    }



}
