package com.logic.superbrecruits.entity.ai;

import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public class VehicleStrategicFire extends Goal {
    private BlockPos pos;
    private BowmanEntity bowman;
    private MobileVehicleEntity vehicle;

    private int weaponSlot;

    public VehicleStrategicFire(BowmanEntity bowman) {
        this.bowman = bowman;
    }

    public boolean canUse() {
        Item item = this.bowman.getMainHandItem().getItem();

        if(this.bowman.getVehicle() instanceof MobileVehicleEntity vehicle) {
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

        if(this.vehicle instanceof WeaponVehicleEntity weaponVehicleEntity) {
            int seatIndex = vehicle.getSeatIndex(this.bowman);

            int length = weaponVehicleEntity.getAvailableWeapons(seatIndex).size();

            if(weaponSlot < length) {
                this.bowman.lookAt(EntityAnchorArgument.Anchor.EYES, vec3);

                if(weaponVehicleEntity.hasWeapon(seatIndex) && weaponVehicleEntity.getWeaponIndex(seatIndex) != weaponSlot) {
                    int currentAmmo = weaponVehicleEntity.getAmmoCount(this.bowman);

                    weaponVehicleEntity.vehicleShoot(this.bowman, weaponSlot);

                    if(currentAmmo != weaponVehicleEntity.getAmmoCount(this.bowman)) {
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
    }
}
