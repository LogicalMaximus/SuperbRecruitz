package com.logic.superbrecruits.entity.ai;

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class VehicleShootGoal<T extends AbstractRecruitEntity> extends Goal {

    private final T recruit;

    private LivingEntity target;

    private VehicleEntity vehicle;

    private int weaponSlot;

    private Random random = new Random();

    private static final double SPREAD = 4;

    public VehicleShootGoal(T recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        if(this.recruit.getVehicle() instanceof VehicleEntity vehicle) {
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

        this.vehicle.turretAutoAimFromUuid(this.target.getStringUUID(), this.recruit);

        if(this.vehicle instanceof ArtilleryEntity entity) {
            ItemStack stack = this.recruit.getMainHandItem();

            stack.getOrCreateTag().putDouble("TargetX", this.target.getX());
            stack.getOrCreateTag().putDouble("TargetY", this.target.getY());
            stack.getOrCreateTag().putDouble("TargetZ", this.target.getZ());

            entity.setTarget(stack, this.recruit, "Main");

            this.recruit.swing(InteractionHand.MAIN_HAND);

            GunData main = this.vehicle.getGunData("Main");

            if(vehicle.canShoot(this.recruit)) {
                vehicle.vehicleShoot(this.recruit, "Main");
            }
        } else {
            if(isSightNearTarget(target)) {
                int seatIndex = vehicle.getSeatIndex(this.recruit);

                int length = vehicle.getGunDataMap().size();

                if(weaponSlot < length) {
                    this.recruit.lookAt(EntityAnchorArgument.Anchor.EYES, eyePosition);

                    if(vehicle.hasWeapon(seatIndex) && vehicle.getWeaponIndex(seatIndex) != weaponSlot) {
                        int currentAmmo = vehicle.getAmmoCount(this.recruit);

                        for(String weaponName : this.vehicle.getGunDataMap().keySet()) {
                            vehicle.vehicleShoot(this.recruit, weaponName);
                        }

                        if(currentAmmo != vehicle.getAmmoCount(this.recruit)) {
                            vehicle.changeWeapon(seatIndex, weaponSlot, false);

                            weaponSlot++;
                        }
                    }
                    else {
                        vehicle.changeWeapon(seatIndex, weaponSlot, false);

                        weaponSlot++;
                    }
                }
            }
        }

        super.tick();
    }

    private boolean isSightNearTarget(LivingEntity target) {
        if(target != null) {
            Vec3 start = this.vehicle.getBarrelVector(1.0F);
            Vec3 end = target.getEyePosition();

            EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(this.vehicle.level(), this.vehicle, start, end, new AABB(start, end).inflate(1.0), (e) -> e == target);

            if(entityHitResult != null) {

                return entityHitResult.getType() == HitResult.Type.ENTITY;
            }
        }

        return false;
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
