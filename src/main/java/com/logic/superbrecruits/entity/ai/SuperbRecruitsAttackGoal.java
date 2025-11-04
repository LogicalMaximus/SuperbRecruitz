package com.logic.superbrecruits.entity.ai;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.logic.superbrecruits.bridge.IAmmoConsumer;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class SuperbRecruitsAttackGoal<T extends AbstractRecruitEntity> extends Goal {
    private final T recruit;
    private final double speedModifier;
    private LivingEntity target;
    private int attackTime = -1;
    private int seeTime;
    private final double stopRange;
    private boolean consumeArrows;
    private GunItem weapon;

    public SuperbRecruitsAttackGoal(T mob, double speedModifier, double stopRange) {
        this.recruit = mob;
        this.speedModifier = speedModifier;
        this.stopRange = stopRange;
        this.setFlags(EnumSet.of(Flag.LOOK));
        this.consumeArrows = (Boolean) RecruitsServerConfig.RangedRecruitsNeedArrowsToShoot.get();
    }

    @Override
    public boolean canUse() {
        if(RecruitsServerConfig.RangedRecruitsNeedArrowsToShoot.get() && !((IAmmoConsumer)recruit).superbrecruits$hasAmmo())
            return false;

        LivingEntity livingentity = this.recruit.getTarget();

        if (livingentity != null && livingentity.isAlive() && isHoldingGun(this.recruit)) {
            this.target = livingentity;
            float distance = this.target.distanceTo(this.recruit);
            boolean canTackMovePos = this.canAttackMovePos();
            boolean shouldRanged = this.recruit.getShouldRanged();
            boolean canAttack = this.recruit.canAttack(this.target);
            boolean notPassive = this.recruit.getState() != 3;
            boolean notNeedsToGetFood = !this.recruit.needsToGetFood();
            boolean canSee = this.recruit.getSensing().hasLineOfSight(this.target);
            if (!canSee) {
                this.recruit.setTarget((LivingEntity)null);
                return false;
            } else {
                ItemStack stack = this.recruit.getMainHandItem();

                if(stack.getItem() instanceof GunItem gunItem) {
                    this.weapon = gunItem;

                    return (double)distance >= this.stopRange && canTackMovePos && notNeedsToGetFood && canAttack && notPassive && shouldRanged;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private boolean hasEnoughAmmoToShoot(ItemStack weapon) {
        if(weapon.getItem() instanceof GunItem) {
            return GunData.from(weapon).hasEnoughAmmoToShoot(this.recruit);
        }

        return !this.consumeArrows;
    }

    private boolean hasAmmo(ItemStack weapon) {
        if(weapon.getItem() instanceof GunItem) {
            return GunData.from(weapon).hasBackupAmmo(this.recruit);
        }

        return !this.consumeArrows;
    }

    private boolean isHoldingGun(T recruit) {
        return recruit.getMainHandItem().getItem() instanceof GunItem;
    }

    public boolean canContinueToUse() {
        return this.canUse() && isHoldingGun(this.recruit);
    }

    public void start() {
        super.start();
        this.recruit.setAggressive(true);
    }

    public void stop() {
        super.stop();
        this.recruit.setAggressive(false);
        this.target = null;
        this.seeTime = 0;
        this.recruit.stopUsingItem();
    }

    /*
    @Override
    public void tick() {
        if (this.target != null && this.target.isAlive()) {
            double distance = this.target.distanceToSqr(this.recruit);
            boolean isClose = distance <= (double)150.0F;
            boolean isFar = distance >= (double)3500.0F;
            boolean isTooFar = distance >= (double)4500.0F;
            boolean inRange = !isFar;
            boolean canSee = this.recruit.getSensing().hasLineOfSight(this.target);
            if (canSee) {
                ++this.seeTime;
            } else {


                this.seeTime = 0;
            }

            if (isTooFar) {
                this.recruit.setTarget((LivingEntity)null);
                this.stop();
                return;
            }

            if (this.recruit.getShouldFollow() && this.recruit.getOwner() != null) {
                this.handleFollow(this.recruit.getOwner(), inRange, isFar, isClose);
            } else if (this.recruit.getShouldHoldPos() && this.recruit.getHoldPos() != null) {
                this.handleHoldPos(this.recruit.getHoldPos(), inRange, isFar, isClose);
            } else {
                this.handleWander(inRange, isFar, isClose);
            }

            this.recruit.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

            ItemStack itemStack = this.recruit.getMainHandItem();

            if (itemStack.getItem() instanceof GunItem) {
                GunData gunData = GunData.from(itemStack);

                if(RecruitsServerConfig.RangedRecruitsNeedArrowsToShoot.get() && !this.hasEnoughAmmoToShoot(itemStack)) {
                    GunEventHandler.tryStartReload(this.recruit, gunData);
                }
                
                handleGunBolt(this.recruit, itemStack);
                if (type == 0) {
                    data.item.onFireKeyPress(data, player, zoom);
                } else if (type == 1) {
                    data.item.onFireKeyRelease(data, player, power, zoom);
                }

                float f = Mth.sqrt((float)distance) / this.weapon.attackRadius();
                float attackIntervalMax = this.weapon.getAttackCooldown() + RecruitsWariumConfig.ADDITIONAL_SHOOT_DELAY.get();
                float attackIntervalMin = this.weapon.getAttackCooldown();

                this.attackTime = Mth.floor(f * (float)(attackIntervalMax - attackIntervalMin) + (float)attackIntervalMin);
            }

        }

        --this.attackTime;

        super.tick();
    }


     */

    private boolean canAttackMovePos() {
        LivingEntity target = this.recruit.getTarget();
        BlockPos pos = this.recruit.getMovePos();
        if (target != null && pos != null && this.recruit.getShouldMovePos()) {
            boolean targetIsFar = target.distanceToSqr(this.recruit) >= (double)320.0F;
            boolean posIsClose = pos.distSqr(this.recruit.getOnPos()) <= (double)150.0F;
            boolean posIsFar = pos.distSqr(this.recruit.getOnPos()) > (double)150.0F;
            if (posIsFar) {
                return false;
            }

            if (posIsClose && targetIsFar) {
                return false;
            }
        }

        return true;
    }

    private void handleFollow(@NotNull LivingEntity owner, boolean inRange, boolean isFar, boolean isClose) {
        boolean ownerClose = owner.distanceToSqr(this.recruit) <= (double)100.0F;
        if (ownerClose) {
            if (inRange) {
                this.recruit.getNavigation().stop();
            }

            if (isFar) {
                this.recruit.getNavigation().moveTo(this.target, this.speedModifier);
            }
        }

    }

    private void handleHoldPos(@NotNull Vec3 pos, boolean inRange, boolean isFar, boolean isClose) {
        boolean posClose = pos.distanceToSqr(this.recruit.position()) <= (double)50.0F;
        if (posClose && inRange) {
            this.recruit.getNavigation().stop();
        }

    }

    private void handleWander(boolean inRange, boolean isFar, boolean isClose) {
        if (inRange) {
            this.recruit.getNavigation().stop();
        }

        if (isFar) {
            this.recruit.getNavigation().moveTo(this.target, this.speedModifier);
        }

    }

    private static void handleGunBolt(AbstractRecruitEntity recruit, ItemStack stack) {
        if (stack.getItem() instanceof GunItem) {
            GunData data = GunData.from(stack);
            if ((Integer)data.get(GunProp.BOLT_ACTION_TIME) > 0 && data.hasEnoughAmmoToShoot(recruit) && data.bolt.actionTimer.get() == 0 && !data.reloading() && !data.charging() && data.bolt.needed.get()) {
                data.startBolt();
                GunEventHandler.playGunBoltSounds(recruit, data);
            }

        }
    }
}
