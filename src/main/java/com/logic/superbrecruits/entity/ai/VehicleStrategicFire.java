package com.logic.superbrecruits.entity.ai;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class VehicleStrategicFire extends Goal {
    private static final double SPREAD = 6;

    private BlockPos pos;
    private BowmanEntity bowman;
    private VehicleEntity vehicle;

    private int weaponSlot;

    private Random random = new Random();

    public VehicleStrategicFire(BowmanEntity bowman) {
        this.bowman = bowman;
    }

    public boolean canUse() {
        Item item = this.bowman.getMainHandItem().getItem();

        if(this.bowman.getVehicle() instanceof VehicleEntity vehicle) {
            this.vehicle = vehicle;

            if(this.vehicle.getFirstPassenger() == this.bowman) {

                if (this.bowman.getTarget() == null && this.bowman.getShouldStrategicFire()  && this.bowman.getFollowState() != 5 && !this.bowman.getShouldMount()) {
                    return true;
                } else {
                    this.pos = null;
                }
            }
        }

        return false;
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void stop() {
        super.stop();
        this.bowman.stopUsingItem();
        this.bowman.clearArrowsPos();

        weaponSlot = 0;
    }

    public void tick() {
        this.pos = this.bowman.StrategicFirePos();

        if(this.pos == null)return;

        Vec3 vec3 = new Vec3((double) this.pos.getX(), (double) (this.pos.getY()), (double) this.pos.getZ());

        this.bowman.lookAt(EntityAnchorArgument.Anchor.EYES, vec3);

        if(this.vehicle instanceof ArtilleryEntity entity) {
            ItemStack stack = this.bowman.getMainHandItem();

            stack.getOrCreateTag().putDouble("TargetX", this.random.nextDouble(this.bowman.getX() - SPREAD, this.bowman.getX() + SPREAD));
            stack.getOrCreateTag().putDouble("TargetY", this.bowman.getY());
            stack.getOrCreateTag().putDouble("TargetZ", this.random.nextDouble(this.bowman.getZ() - SPREAD, this.bowman.getZ() + SPREAD));

            entity.setTarget(stack, this.bowman, "Main");

            this.bowman.swing(InteractionHand.MAIN_HAND);

            GunData main = this.vehicle.getGunData("Main");

            if(vehicle.canShoot(this.bowman)) {
                vehicle.vehicleShoot(this.bowman, "Main");
            }
        } else {
            int seatIndex = vehicle.getSeatIndex(this.bowman);

            int length = vehicle.getGunDataMap().size();

            if(weaponSlot < length) {
                this.bowman.lookAt(EntityAnchorArgument.Anchor.EYES, vec3);

                if(vehicle.hasWeapon(seatIndex) && vehicle.getWeaponIndex(seatIndex) != weaponSlot) {
                    int currentAmmo = vehicle.getAmmoCount(this.bowman);

                    vehicle.vehicleShoot(this.bowman, vehicle.getGunName(seatIndex, weaponSlot));

                    if(currentAmmo != vehicle.getAmmoCount(this.bowman)) {
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
}
