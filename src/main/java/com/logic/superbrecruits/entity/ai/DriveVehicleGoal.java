package com.logic.superbrecruits.entity.ai;

import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.logic.superbrecruits.entity.ai.navigation.GroundVehiclePathNavigation;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
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

    private MobileVehicleEntity vehicle;

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
        if(this.recruit.getVehicle() instanceof MobileVehicleEntity mobileVehicleEntity) {
            this.vehicle = mobileVehicleEntity;

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
        if(this.recruit.getShouldHoldPos()) {
            targetPos = BlockPos.containing(this.recruit.getHoldPos());
        } else if(this.recruit.getShouldMovePos() && !this.recruit.needsToGetFood() && !this.recruit.getShouldMount()) {
            targetPos = this.recruit.getMovePos();
        } else if (this.recruit.getTarget() != null && canMove()) {
            targetPos = this.recruit.getTarget().blockPosition();
        } else if(this.recruit.getShouldFollow() && !this.recruit.getFleeing() && this.recruit.getFollowState() == 1 && !this.recruit.needsToGetFood() && !this.recruit.getShouldMount() && !this.recruit.getShouldMovePos()) {
            Player owner = this.recruit.getOwner();

            if(owner != null) {
                targetPos = owner.blockPosition();
            }
        } else if (this.canWander()) {
            Vec3 position = this.getPosition();

            if(position != null) {
                targetPos = new BlockPos((int) position.x, (int) position.y, (int) position.z);
            }
        }

        if(this.targetPos != null) {

            double distanceSqr = this.vehicle.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ());

            double stopDistance = this.vehicle.getBbWidth() - 1 + STOP_DISTANCE;

            double stopDistanceSqr = stopDistance * stopDistance;

            if(distanceSqr > stopDistanceSqr) {
                Vec3 toTarget = new Vec3(targetPos.getX() - this.vehicle.getX(), 0, targetPos.getZ() - this.vehicle.getZ()).normalize();

                Vector3f forward = this.vehicle.getForwardDirection().normalize();

                double angle = getAngleBetween(forward, toTarget);
                double angleThreshold = getRotationStopAngle(distanceSqr);
                double EPS = Double.MIN_VALUE;

                if(Math.abs(angle) < Math.max(EPS, angleThreshold))  {
                    this.vehicle.forwardInputDown = true;
                    this.vehicle.backInputDown = false;

                    this.vehicle.leftInputDown = false;
                    this.vehicle.rightInputDown = false;
                }
                else {
                    this.vehicle.leftInputDown = angle > 0;
                    this.vehicle.rightInputDown = angle < 0;

                    if(this.vehicle.hasTracks()) {
                        this.vehicle.forwardInputDown = false;
                    }
                    else {
                        this.vehicle.forwardInputDown = true;
                    }


                    this.vehicle.backInputDown = false;
                }

                lastAngle = angle;
            }
            else {
                this.vehicle.forwardInputDown = false;
                this.vehicle.backInputDown = false;
                this.vehicle.leftInputDown = false;
                this.vehicle.rightInputDown = false;
            }

        }

        super.tick();
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
