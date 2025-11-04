package com.logic.superbrecruits.entity.ai;

import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class VehicleShootGoal<T extends AbstractRecruitEntity> extends Goal {

    private final T recruit;

    private LivingEntity target;

    private MobileVehicleEntity vehicle;

    private int weaponSlot;

    public VehicleShootGoal(T recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        if(this.recruit.getVehicle() instanceof MobileVehicleEntity vehicle) {
            this.vehicle = vehicle;

            if(this.vehicle.getFirstPassenger() == this.recruit) {

                LivingEntity livingentity = this.recruit.getTarget();

                if (livingentity != null && livingentity.isAlive()) {
                    this.target = livingentity;
                    boolean canTackMovePos = this.canAttackMovePos();
                    boolean canAttack = this.recruit.canAttack(this.target);
                    boolean notPassive = this.recruit.getState() != 3;

                    return canTackMovePos && canAttack && notPassive;
                }
            }
        }

        return false;
    }

    public void start() {
        super.start();

        this.recruit.setAggressive(true);
    }

    public void stop() {
        super.stop();

        this.recruit.setAggressive(false);
        this.target = null;
        weaponSlot = 0;
    }

    @Override
    public void tick() {
        Vec3 eyePosition;

        Entity targetVehicle = this.target.getVehicle();

        if(targetVehicle != null) {
            eyePosition = targetVehicle.getEyePosition();
        } else {
            eyePosition = this.target.getEyePosition();
        }

        this.recruit.lookAt(EntityAnchorArgument.Anchor.EYES, eyePosition);

        this.vehicle.turretAutoAimFormUuid(this.target.getStringUUID(), this.recruit);

        if(this.vehicle instanceof WeaponVehicleEntity weaponVehicleEntity) {
            int seatIndex = vehicle.getSeatIndex(this.recruit);

            int length = weaponVehicleEntity.getAvailableWeapons(seatIndex).size();

            if(weaponSlot < length) {
                this.recruit.lookAt(EntityAnchorArgument.Anchor.EYES, eyePosition);

                if(weaponVehicleEntity.hasWeapon(seatIndex) && weaponVehicleEntity.getWeaponIndex(seatIndex) != weaponSlot) {
                    int currentAmmo = weaponVehicleEntity.getAmmoCount(this.recruit);

                    weaponVehicleEntity.vehicleShoot(this.recruit, weaponSlot);

                    if(currentAmmo != weaponVehicleEntity.getAmmoCount(this.recruit)) {
                        weaponVehicleEntity.changeWeapon(seatIndex, weaponSlot, false);

                        weaponSlot++;
                    }
                }
                else {
                    weaponVehicleEntity.changeWeapon(seatIndex, weaponSlot, false);

                    weaponSlot++;
                }
            }
        }

        super.tick();
    }

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
}
