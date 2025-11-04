package com.logic.superbrecruits.mixin;

import com.atsuishio.superbwarfare.data.vehicle.VehiclePropertyModifier;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.logic.superbrecruits.bridge.ILookerEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VehicleEntity.class)
public abstract class MixinVehicleEntity extends Entity implements Container, VehiclePropertyModifier {

    @Shadow(remap = false)
    public static EntityDataAccessor<Boolean> TURRET_DAMAGED;

    @Shadow(remap = false)
    public static EntityDataAccessor<Float> DELTA_ROT;

    @Shadow(remap = false)
    public float turretYRotLock;

    public MixinVehicleEntity(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @Inject(at = @At("TAIL"), method = "baseTick")
    public void baseTick(CallbackInfo ci) {
        if (this instanceof WeaponVehicleEntity weaponVehicle) {
            if (this.getFirstPassenger() instanceof AbstractRecruitEntity) {
                this.turretAngle();
            }
        }
    }


    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public void gunnerAngle() {
        float ySpeed = this.passengerWeaponYSpeed();
        float xSpeed = this.passengerWeaponXSpeed();
        Entity gunner = this.getNthEntity(1);
        float diffY = 0.0F;
        float diffX = 0.0F;
        float speed = 1.0F;

        if (gunner instanceof LivingEntity) {
            if(gunner instanceof AbstractRecruitEntity recruit) {
                Vec3 lookTarget = ((ILookerEntity) recruit).getLookTarget();

                if(lookTarget != null) {
                    recruit.lookAt(EntityAnchorArgument.Anchor.EYES, lookTarget);
                }
            }

            float gunAngle = -Mth.wrapDegrees(gunner.getYHeadRot() - this.getYRot());
            diffY = Mth.wrapDegrees(gunAngle - this.getGunYRot());
            diffX = Mth.wrapDegrees(gunner.getXRot() - this.getGunXRot());
            this.turretTurnSound(diffX, diffY, 0.95F);
            speed = 0.0F;
        }

        this.setGunXRot(this.getGunXRot() + Mth.clamp(0.95F * diffX, -xSpeed, xSpeed));
        this.setGunYRot(this.getGunYRot() + Mth.clamp(0.9F * diffY, -ySpeed, ySpeed) + speed * this.turretYRotLock);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public void turretAngle() {
        float ySpeed = this.turretYSpeed();
        float xSpeed = this.turretXSpeed();
        Entity driver = this.getFirstPassenger();
        if (driver != null) {

            if(driver instanceof AbstractRecruitEntity recruit) {
                Vec3 lookTarget = ((ILookerEntity) recruit).getLookTarget();

                if(lookTarget != null) {
                    recruit.lookAt(EntityAnchorArgument.Anchor.EYES, lookTarget);
                }
            }

            float turretAngle = -Mth.wrapDegrees(driver.getYHeadRot() - this.getYRot());

            float diffY = Mth.wrapDegrees(turretAngle - this.getTurretYRot());
            float diffX = Mth.wrapDegrees(driver.getXRot() - this.getTurretXRot());
            this.turretTurnSound(diffX, diffY, 0.95F);
            if ((Boolean)this.entityData.get(TURRET_DAMAGED)) {
                ySpeed *= 0.2F;
                xSpeed *= 0.2F;
            }

            float min = -ySpeed + (float)(this.isInWater() && !this.onGround() ? (double)2.5F : (double)6.0F) * (Float)this.entityData.get(DELTA_ROT);
            float max = ySpeed + (float)(this.isInWater() && !this.onGround() ? (double)2.5F : (double)6.0F) * (Float)this.entityData.get(DELTA_ROT);
            this.setTurretXRot(this.getTurretXRot() + Mth.clamp(0.95F * diffX, -xSpeed, xSpeed));
            this.setTurretYRot(this.getTurretYRot() + Mth.clamp(0.9F * diffY, min, max));
            this.turretYRotLock = Mth.clamp(0.9F * diffY, min, max);
        } else {
            this.turretYRotLock = 0.0F;
        }

    }

    @Shadow(remap = false)
    private void setTurretYRot(float v) {
    }

    @Shadow(remap = false)
    private void setTurretXRot(float v) {
    }

    @Shadow(remap = false)
    private float turretXSpeed() {
        return 0;
    }

    @Shadow(remap = false)
    private float getTurretXRot() {
        return 0;
    }

    @Shadow(remap = false)
    private float getTurretYRot() {
        return 0;
    }

    @Shadow(remap = false)
    private float turretYSpeed() {
        return 0;
    }

    @Shadow(remap = false)
    private void setGunYRot(float v) {
    }

    @Shadow(remap = false)
    private void setGunXRot(float v) {
    }

    @Shadow(remap = false)
    private void turretTurnSound(float diffX, float diffY, float v) {
    }

    @Shadow(remap = false)
    private float getGunXRot() {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private float getGunYRot() {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private Entity getNthEntity(int i) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private float passengerWeaponXSpeed() {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private float passengerWeaponYSpeed() {
        throw new AssertionError();
    }
}
