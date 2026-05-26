package com.logic.superbrecruits.mixin;

import com.atsuishio.superbwarfare.data.vehicle.VehiclePropertyModifier;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
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

    @Shadow
    public abstract VehicleType getVehicleType();

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
        if (this.getFirstPassenger() instanceof AbstractRecruitEntity abstractRecruitEntity) {
            if (this.hasTurret()) {
                Vec3 barrelVector = this.getBarrelVector(1.0F);
                double xRot = VehicleVecUtils.getXRotFromVector(barrelVector);
                double yRot = VehicleVecUtils.getYRotFromVector(barrelVector);
                abstractRecruitEntity.xRotO = (float)(-xRot);
                abstractRecruitEntity.setXRot((float)(-xRot));
                abstractRecruitEntity.yRotO = (float)(-yRot);
                abstractRecruitEntity.setYRot((float)(-yRot));
                abstractRecruitEntity.setYHeadRot((float)(-yRot));
            } else {
                abstractRecruitEntity.xRotO = this.getXRot();
                abstractRecruitEntity.setXRot(this.getXRot());
                abstractRecruitEntity.yRotO = this.getYRot();
                abstractRecruitEntity.setYRot(this.getYRot());
            }
        }
    }

    @Shadow(remap = false)
    public boolean hasTurret() {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    public Vec3 getBarrelVector(float pPartialTicks) {
        throw new AssertionError();
    }
}
