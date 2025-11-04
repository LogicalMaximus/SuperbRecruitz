package com.logic.superbrecruits.mixin;

import com.logic.superbrecruits.entity.ai.*;
import com.talhanation.recruits.entities.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BowmanEntity.class)
public abstract class MixinBowman extends AbstractRecruitEntity implements IRangedRecruit, IStrategicFire {
    public MixinBowman(EntityType<? extends AbstractInventoryEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    protected void registerGoals(CallbackInfo ci) {
        this.targetSelector.addGoal(2, new MortarStrategicFireGoal(((BowmanEntity) (Object) this)));
        this.targetSelector.addGoal(2, new VehicleStrategicFire(((BowmanEntity) (Object) this)));
    }
}
