package com.logic.superbrecruits.mixin;

import com.logic.superbrecruits.bridge.ILookerEntity;
import com.logic.superbrecruits.entity.ai.DriveVehicleGoal;
import com.logic.superbrecruits.entity.ai.MortarAttackGoal;
import com.logic.superbrecruits.entity.ai.VehicleShootGoal;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecruitEntity.class)
public abstract class MixinAbstractRecruitEntity extends AbstractInventoryEntity implements ILookerEntity {

    @Unique
    private Vec3 lookTarget;

    public MixinAbstractRecruitEntity(EntityType<? extends AbstractInventoryEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    protected void registerGoals(CallbackInfo ci) {
        this.targetSelector.addGoal(2, new VehicleShootGoal<>(((AbstractRecruitEntity) (Object) this)));
        this.targetSelector.addGoal(3, new DriveVehicleGoal<>(((AbstractRecruitEntity) (Object) this)));
        this.targetSelector.addGoal(3, new MortarAttackGoal<>(((AbstractRecruitEntity) (Object) this)));
    }

    @Override
    public void lookAt(EntityAnchorArgument.Anchor p_21078_, Vec3 p_21079_) {
        this.lookTarget = p_21079_;

        super.lookAt(p_21078_, p_21079_);
    }

    @Override
    public Vec3 getLookTarget() {
        return lookTarget;
    }

}
