package com.logic.superbrecruits.entity.ai;

import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData;
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.google.gson.JsonElement;
import com.logic.superbrecruits.entity.ai.navigation.GroundVehiclePathNavigation;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class DriveVehicleGoal<T extends AbstractRecruitEntity> extends Goal {
    private static final double MIN_ANGLE_RAD = Math.toRadians(3.0);   // tight when close
    private static final double MAX_ANGLE_RAD = Math.toRadians(22.5);  // laxer when far
    private static final double MIN_DISTANCE  = 2.0;
    private static final double MAX_DISTANCE  = 20.0;

    private static final double STOP_DISTANCE = 8.0;

    private final T recruit;

    private VehicleEntity vehicle;

    private GroundVehiclePathNavigation navigation;

    private BlockPos targetPos;

    private Path path;

    private Node node;

    private int currentNodeIdex = 0;
    private double lastAngle = 0.0;

    public DriveVehicleGoal(T recruit) {
        this.recruit = recruit;
    }


    @Override
    public boolean canUse() {
        if(this.recruit.getVehicle() instanceof VehicleEntity vehicle) {
            this.vehicle = vehicle;

            if(this.vehicle.getFirstPassenger() == this.recruit) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void start() {
        this.navigation = new GroundVehiclePathNavigation(this.vehicle, recruit, recruit.level());

        super.start();
    }

    @Override
    public void stop() {
        targetPos = null;

        currentNodeIdex = 0;

        super.stop();
    }

    @Override
    public void tick() {
        this.targetPos = getTargetPos();

        if(this.targetPos != null) {
                double distanceSqr = this.vehicle.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ());

                double stopDistance = this.vehicle.getBbWidth() - 1 + STOP_DISTANCE;

                double stopDistanceSqr = stopDistance * stopDistance;

                if(distanceSqr > stopDistanceSqr) {
                    if(this.vehicle.getVehicleType().equals(VehicleType.HELICOPTER)) {
                        flyVehicleHelicopter(distanceSqr);
                    } else {
                        driveGroundVehicle(distanceSqr);
                    }
                }
                else {
                    stopVehicleMovement();
                }
        }

        super.tick();
    }

    private void driveGroundVehicle(double distanceSqr) {
        Vec3 toTarget = new Vec3(targetPos.getX() - this.vehicle.getX(), this.vehicle.getY() - 10, targetPos.getZ() - this.vehicle.getZ()).normalize();

        Vector3f forward = this.vehicle.getForwardDirection().normalize();

        double angle = getAngleBetween(forward, toTarget);
        double angleThreshold = getRotationStopAngle(distanceSqr);
        double EPS = Double.MIN_VALUE;

        if(Math.abs(angle) < Math.max(EPS, angleThreshold))  {
            moveVehicleForward();
        }
        else {
            this.vehicle.setLeftInputDown(angle > 0);
            this.vehicle.setRightInputDown(angle < 0);

            DefaultVehicleData defaultVehicleData = this.vehicle.data().compute();

            if(defaultVehicleData != null) {
                JsonElement trackRotSpeed = defaultVehicleData.getEngineInfo().get("TrackRotSpeed");

                if (trackRotSpeed != null && trackRotSpeed.getAsInt() > 0) {
                    this.vehicle.setForwardInputDown(false);
                }
                else {
                    this.vehicle.setForwardInputDown(true);
                }
            }
            else {
                this.vehicle.setForwardInputDown(true);
            }

            this.vehicle.setBackInputDown(false);
        }

        lastAngle = angle;
    }

    private void flyVehicleHelicopter(double distanceSqr) {
        double targetHeight = this.vehicle.level().getHeight(Heightmap.Types.WORLD_SURFACE_WG, (int) this.vehicle.getX(), (int) this.vehicle.getZ()) + 32;

        Vec3 toTarget = new Vec3(targetPos.getX() - this.vehicle.getX(), targetPos.getY() - this.vehicle.getY(), targetPos.getZ() - this.vehicle.getZ()).normalize();

        Vector3f forward = this.vehicle.getForwardDirection().normalize();

        double angle = getAngleBetween(forward, toTarget);
        double angleThreshold = getRotationStopAngle(distanceSqr);
        double EPS = Double.MIN_VALUE;

        double radians = Math.toRadians(angle);

        double verticalStopDistance = this.vehicle.getBbHeight() + STOP_DISTANCE;

        if(this.vehicle.getY() < targetHeight - verticalStopDistance) {
            moveVehicleForward();

            this.vehicle.setHoverMode(true);
        } else if (this.vehicle.getY() > targetHeight + verticalStopDistance){
            moveVehicleBackward();

            this.vehicle.setHoverMode(false);
        } else {
            stopVehicleMovement();

            this.vehicle.setHoverMode(false);
        }

        if(Math.abs(angle) < Math.max(EPS, angleThreshold))  {
            this.vehicle.mouseInput(0,15);
        }
        else {
            if(angle > 0) {
                this.vehicle.mouseInput(-5,0);
            } else {
                this.vehicle.mouseInput(5,0);
            }
        }

    }

    private void moveVehicleForward() {
        this.vehicle.setForwardInputDown(true);
        this.vehicle.setBackInputDown(false);
        this.vehicle.setLeftInputDown(false);
        this.vehicle.setRightInputDown(false);
    }

    private void moveVehicleBackward() {
        this.vehicle.setForwardInputDown(false);
        this.vehicle.setBackInputDown(true);
        this.vehicle.setLeftInputDown(false);
        this.vehicle.setRightInputDown(false);
    }

    private void stopVehicleMovement() {
        this.vehicle.setForwardInputDown(false);
        this.vehicle.setBackInputDown(false);
        this.vehicle.setLeftInputDown(false);
        this.vehicle.setRightInputDown(false);
    }

    private BlockPos getTargetPos() {
        if(this.recruit.getShouldHoldPos()) {
            return BlockPos.containing(this.recruit.getHoldPos());
        } else if(this.recruit.getShouldMovePos() && !this.recruit.needsToGetFood() && !this.recruit.getShouldMount()) {
            return this.recruit.getMovePos();
        } else if (this.recruit.getTarget() != null && canMove()) {
            return this.recruit.getTarget().blockPosition();
        } else if(this.recruit.getShouldFollow() && !this.recruit.getFleeing() && this.recruit.getFollowState() == 1 && !this.recruit.needsToGetFood() && !this.recruit.getShouldMount() && !this.recruit.getShouldMovePos()) {
            Player owner = this.recruit.getOwner();

            if(owner != null) {
                return owner.blockPosition();
            }
        } else if (this.canWander()) {
            Vec3 position = this.getPosition();

            if(position != null) {
                return new BlockPos((int) position.x, (int) position.y, (int) position.z);
            }
        }

        return null;
    }

    private double getAngleBetween(Vector3f forward, Vec3 target) {

        double fx = forward.x;
        double fz = forward.z;
        double tx = target.x;
        double tz = target.z;

        double cross = fx * tz - fz * tx;
        double dot = fx * tx + fz * tz;

        return -Math.atan2(cross, dot);
    }

    private double getDotProduct(Vector3f forward, Vec3 target) {
        double fx = forward.x;
        double fz = forward.z;
        double tx = target.x;
        double tz = target.z;

        return fx * tx + fz * tz;
    }

    private double getRotationStopAngle(double distanceSq) {
        double distance = Math.sqrt(distanceSq);
        double t = (distance - MIN_DISTANCE) / (MAX_DISTANCE - MIN_DISTANCE);
        t = Math.max(0.0, Math.min(1.0, t));
        return MIN_ANGLE_RAD + (MAX_ANGLE_RAD - MIN_ANGLE_RAD) * t;
    }

    private boolean canWander() {
        LivingEntity living = this.recruit.getTarget();
        boolean state = this.recruit.getFollowState() == 0;
        boolean target = living == null || !living.isAlive() || living.distanceToSqr(this.recruit) < (double)300.0F;
        boolean rest = !this.recruit.getShouldRest();
        boolean move = !this.recruit.getShouldMovePos();
        boolean protect = !this.recruit.getShouldProtect();
        boolean follow = !this.recruit.getShouldFollow();
        boolean fleeing = !this.recruit.getFleeing();
        boolean needFood = !this.recruit.needsToGetFood();
        boolean mount = !this.recruit.getShouldMount();
        return state && target && rest && move && protect && follow && fleeing && needFood && mount;
    }

    @Nullable
    protected Vec3 getPosition() {
        if (this.recruit.isInWaterOrBubble()) {
            this.recruit.restrictTo(this.recruit.blockPosition(), 150);
            Vec3 vec3 = LandRandomPos.getPos(this.recruit, 32, 16);
            return vec3 == null ? DefaultRandomPos.getPos(this.recruit, 32, 16) : vec3;
        } else {
            this.recruit.restrictTo(this.recruit.blockPosition(), 20);
            return LandRandomPos.getPos(this.recruit, 10, 0);
        }
    }

    private boolean canMove() {
        return !this.recruit.getFleeing() && !this.recruit.getShouldMount();
    }
}
