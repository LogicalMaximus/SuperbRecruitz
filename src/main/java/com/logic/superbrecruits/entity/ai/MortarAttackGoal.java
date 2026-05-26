package com.logic.superbrecruits.entity.ai;

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.item.projectile.MortarShellItem;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
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

        if (livingentity != null && livingentity.isAlive() && this.recruit.getMainHandItem().getItem() instanceof MortarShellItem && this.recruit.getVehicle() == null) {
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

            if(itemStack.getItem() instanceof MortarShellItem) {
                itemStack.getOrCreateTag().remove("TargetX");
                itemStack.getOrCreateTag().remove("TargetY");
                itemStack.getOrCreateTag().remove("TargetZ");
            }
        }

        super.stop();
    }

    @Override
    public void tick() {
        //this.mergeItemstacks();

        if(nearestMortar == null || nearestMortar.isRemoved()) {
            List<MortarEntity> mortarEntities = this.recruit.level().getEntitiesOfClass(MortarEntity.class, this.recruit.getBoundingBox().inflate(32));

            if(!mortarEntities.isEmpty()) {
                mortarEntities.sort(Comparator.comparingDouble((e) -> e.distanceToSqr(this.recruit)));

                nearestMortar = mortarEntities.get(0);

            }
        }

        if(this.nearestMortar != null) {
            if(this.recruit.distanceTo(nearestMortar) <= MAX_MORTAR_DISTANCE) {
                ItemStack stack = this.recruit.getMainHandItem();

                if(stack.getItem() instanceof MortarShellItem) {
                    stack.getOrCreateTag().putDouble("TargetX", this.random.nextDouble(this.target.getX() - SPREAD, this.target.getX() + SPREAD));
                    stack.getOrCreateTag().putDouble("TargetY", this.target.getY());
                    stack.getOrCreateTag().putDouble("TargetZ", this.random.nextDouble(this.target.getZ() - SPREAD, this.target.getZ() + SPREAD));

                    nearestMortar.setTarget(stack, this.recruit, "Main");

                    this.recruit.swing(InteractionHand.MAIN_HAND);

                    if (stack.getItem() instanceof MortarShellItem  && (Integer) nearestMortar.getEntityData().get(MortarEntity.FIRE_TIME) == 0 && ((nearestMortar).getItems().get(0)).isEmpty()) {
                        nearestMortar.getItems().set(0, stack.copyWithCount(1));

                        stack.shrink(1);

                        nearestMortar.vehicleShoot(this.recruit, "Main");
                    }
                }
            }
            else {
                this.recruit.getNavigation().moveTo(nearestMortar, 1.0);
            }
        }

        super.tick();
    }

    private void mergeItemstacks() {
        if(this.recruit != null) {
            ItemStack mainHandItem = this.recruit.getMainHandItem();

            if(mainHandItem.isEmpty() && this.recruit.getInventory().getItem(5).isEmpty()) {
                int gunSlot = -1;
                ItemStack itemStack = null;

                for(int i = 0; i < this.recruit.getInventory().getContainerSize(); i++) {
                    SimpleContainer inventory = this.recruit.getInventory();

                    ItemStack itemInSlot = inventory.getItem(i);

                    if(itemInSlot.getItem() instanceof MortarShellItem mortarShell) {
                        itemStack = itemInSlot;

                        gunSlot = i;

                        break;
                    }
                }

                if(itemStack != null && gunSlot != -1) {
                    ItemStack beforeItem = this.recruit.inventory.getItem(5);
                    this.recruit.getInventory().setItem(gunSlot, beforeItem);

                    this.recruit.getInventory().setItem(5, itemStack);
                    this.recruit.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
                }
            }
        }
    }
}
