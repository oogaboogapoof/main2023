package org.team100.frc2023.autonomous;

import org.team100.frc2023.LQRManager;
import org.team100.lib.sensors.RedundantGyro;
import org.team100.lib.subsystems.SwerveDriveSubsystem;
import org.team100.lib.subsystems.VeeringCorrection;

import com.acmerobotics.roadrunner.profile.MotionProfile;
import com.acmerobotics.roadrunner.profile.MotionProfileGenerator;
import com.acmerobotics.roadrunner.profile.MotionState;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;

/**
 * This holonomic drive controller can be used to follow trajectories using a
 * holonomic drivetrain
 * (i.e. swerve or mecanum). Holonomic trajectory following is a much simpler
 * problem to solve
 * compared to skid-steer style drivetrains because it is possible to
 * individually control forward,
 * sideways, and angular velocity.
 *
 * <p>
 * The holonomic drive controller takes in one PID controller for each
 * direction, forward and
 * sideways, and one profiled PID controller for the angular direction. Because
 * the heading dynamics
 * are decoupled from translations, users can specify a custom heading that the
 * drivetrain should
 * point toward. This heading reference is profiled for smoothness.
 */
public class HolonomicLQR {
    private Pose2d m_poseError = new Pose2d();
    private Rotation2d m_rotationError = new Rotation2d();
    private Pose2d m_poseTolerance = new Pose2d();
    private boolean m_enabled = true;
    private final RedundantGyro m_gyro;
    private final VeeringCorrection m_veering;

    private final LQRManager m_xManager;
    private final LQRManager m_yManager;

    MotionState goalX;
    MotionState goalY;
    private MotionProfile profileX = new MotionProfile(null);
    private MotionProfile profileY = new MotionProfile(null);

    Timer m_timer = new Timer();

    private final ProfiledPIDController m_thetaController;

    // private boolean m_firstRun = true;

    SwerveDriveSubsystem m_robotDrive;

    private MotionState m_lastXRef = new MotionState(0, 0);
    private MotionState m_lastYRef = new MotionState(0, 0);

    NetworkTableInstance inst = NetworkTableInstance.getDefault();

    DoublePublisher xOutPublisher = inst.getTable("Holonomic LQR").getDoubleTopic("X Output").publish();
    DoublePublisher yOutPublisher = inst.getTable("Holonomic LQR").getDoubleTopic("Y Output").publish();

    DoublePublisher xDesired = inst.getTable("Holonomic LQR").getDoubleTopic("X Desired").publish();
    DoublePublisher yDesired = inst.getTable("Holonomic LQR").getDoubleTopic("Y Desired").publish();

    DoublePublisher xVolt = inst.getTable("Holonomic LQR").getDoubleTopic("X Volt").publish();
    DoublePublisher yVolt = inst.getTable("Holonomic LQR").getDoubleTopic("Y Volt").publish();

    public HolonomicLQR(
            SwerveDriveSubsystem robotDrive, LQRManager xManager, LQRManager yManager,
            ProfiledPIDController thetaController, RedundantGyro gyro) {
        m_gyro = gyro;
        m_veering = new VeeringCorrection(m_gyro);
        m_xManager = xManager;
        m_yManager = yManager;
        m_thetaController = thetaController;
        m_robotDrive = robotDrive;
        m_thetaController.enableContinuousInput(0, Units.degreesToRadians(360.0));
    }

    /**
     * Returns true if the pose error is within tolerance of the reference.
     *
     * @return True if the pose error is within tolerance of the reference.
     */
    public boolean atReference() {
        final var eTranslate = m_poseError.getTranslation();
        final var eRotate = m_rotationError;
        final var tolTranslate = m_poseTolerance.getTranslation();
        final var tolRotate = m_poseTolerance.getRotation();
        return Math.abs(eTranslate.getX()) < tolTranslate.getX()
                && Math.abs(eTranslate.getY()) < tolTranslate.getY()
                && Math.abs(eRotate.getRadians()) < tolRotate.getRadians();
    }

    /**
     * Sets the pose error which is considered tolerance for use with atReference().
     *
     * @param tolerance The pose error which is tolerable.
     */
    public void setTolerance(Pose2d tolerance) {
        m_poseTolerance = tolerance;
    }

    /**
     * Returns the next output of the holonomic drive controller.
     *
     * @param currentPose                          The current pose, as measured by
     *                                             odometry or pose estimator.
     * @param trajectoryPose                       The desired trajectory pose, as
     *                                             sampled for the current timestep.
     * @param desiredLinearVelocityMetersPerSecond The desired linear velocity.
     * @param desiredHeading                       The desired heading.
     * @return The next output of the holonomic drive controller.
     */
    public ChassisSpeeds calculate(
            Pose2d currentPose,
            Pose2d trajectoryPose,
            double desiredLinearVelocityMetersPerSecond,
            Rotation2d desiredHeading) {

        // If this is the first run, then we need to reset the theta controller to the
        // current pose's

        // Calculate feedforward velocities (field-relative).

        // double xFF = desiredLinearVelocityMetersPerSecond *
        // trajectoryPose.getRotation().getCos();

        // double yFF = desiredLinearVelocityMetersPerSecond *
        // trajectoryPose.getRotation().getSin();

        double thetaFF = m_thetaController.calculate(
                currentPose.getRotation().getRadians(), desiredHeading.getRadians());

        m_poseError = trajectoryPose.relativeTo(currentPose);
        m_rotationError = desiredHeading.minus(currentPose.getRotation());

        if (!m_enabled) {
            // return ChassisSpeeds.fromFieldRelativeSpeeds(xFF, yFF, thetaFF,
            // currentPose.getRotation());
            return null;
        }

        // Calculate feedback velocities (based on position error).

        // goalX = new TrapezoidProfile.State(trajectoryPose.getX(), 0.0);
        // goalY = new TrapezoidProfile.State(trajectoryPose.getY(), 0.0);

        // goalX = new MotionState(trajectoryPose.getX(), 0);
        // goalY = new MotionState(trajectoryPose.getY(), 0);

        m_lastXRef = profileX.get(m_timer.get());
        m_lastYRef = profileY.get(m_timer.get());

        // m_lastXRef = (new TrapezoidProfile(m_xManager.m_constraints, goalX,
        // m_lastXRef)).calculate(0.020);
        // m_lastYRef = (new TrapezoidProfile(m_yManager.m_constraints, goalY,
        // m_lastYRef)).calculate(0.020);

        xDesired.set(m_lastXRef.getX());
        yDesired.set(m_lastYRef.getX());

        m_xManager.m_loop.setNextR(m_lastXRef.getX(), m_lastXRef.getV());
        m_yManager.m_loop.setNextR(m_lastYRef.getX(), m_lastYRef.getV());

        m_xManager.m_loop.correct(VecBuilder.fill(m_robotDrive.getPose().getX()));
        m_yManager.m_loop.correct(VecBuilder.fill(m_robotDrive.getPose().getY()));

        m_xManager.m_loop.predict(0.020);
        m_yManager.m_loop.predict(0.020);

        double nextXVoltage = m_xManager.m_loop.getU(0);
        double nextYVoltage = m_yManager.m_loop.getU(0);

        xVolt.set(nextXVoltage);
        yVolt.set(nextYVoltage);

        Rotation2d rotation2 = m_veering.correct(currentPose.getRotation());

        return ChassisSpeeds.fromFieldRelativeSpeeds(
                5 * nextXVoltage, 5 * nextYVoltage, thetaFF, rotation2);
    }

    /**
     * Returns the next output of the holonomic drive controller.
     *
     * @param currentPose    The current pose, as measured by odometry or pose
     *                       estimator.
     * @param desiredState   The desired trajectory pose, as sampled for the current
     *                       timestep.
     * @param desiredHeading The desired heading.
     * @return The next output of the holonomic drive controller.
     */
    public ChassisSpeeds calculate(
            Pose2d currentPose, Trajectory.State desiredState, Rotation2d desiredHeading) {
        return calculate(
                currentPose, desiredState.poseMeters, desiredState.velocityMetersPerSecond, desiredHeading);
    }

    public void reset(Pose2d currentPose) {
        m_thetaController.reset(currentPose.getRotation().getRadians());
        m_xManager.m_loop.reset(VecBuilder.fill(m_robotDrive.getPose().getX(), 0));
        m_yManager.m_loop.reset(VecBuilder.fill(m_robotDrive.getPose().getY(), 0));

        m_lastXRef = new MotionState(m_robotDrive.getPose().getX(), 0);

        m_lastYRef = new MotionState(m_robotDrive.getPose().getY(), 0);

    }

    public void start() {
        m_timer.restart();
    }

    public void updateProfile(double goalX, double endY, double maxVel, double maxAccel, double maxJerk) {
        profileX = MotionProfileGenerator.generateSimpleMotionProfile(
                new MotionState(m_robotDrive.getPose().getX(), 0),
                new MotionState(goalX, 0),
                maxVel,
                maxAccel,
                maxJerk);

        profileY = MotionProfileGenerator.generateSimpleMotionProfile(
                new MotionState(m_robotDrive.getPose().getY(), 0),
                new MotionState(endY, 0),
                maxVel,
                maxAccel,
                maxJerk);

    }

    /**
     * Enables and disables the controller for troubleshooting problems. When
     * calculate() is called on
     * a disabled controller, only feedforward values are returned.
     *
     * @param enabled If the controller is enabled or not.
     */
    public void setEnabled(boolean enabled) {
        m_enabled = enabled;
    }

}
