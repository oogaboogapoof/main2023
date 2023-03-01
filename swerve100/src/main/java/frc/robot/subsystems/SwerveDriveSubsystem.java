package frc.robot.subsystems;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.DoubleArrayPublisher;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.SerialPort;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.RobotContainer;
import frc.robot.localization.VisionDataProvider;
import team100.config.Identity;

@SuppressWarnings("unused")
public class SwerveDriveSubsystem extends SubsystemBase {
    // TODO: make this an instance var
    public static final SwerveDriveKinematics kDriveKinematics;

    static {
        final double kTrackWidth;
        final double kWheelBase;
        switch (Identity.get()) {
            case SQUAREBOT:
                kTrackWidth = 0.699;
                kWheelBase = 0.512;
                break;
            case SWERVE_TWO:
                kTrackWidth = 0.380;
                kWheelBase = 0.445;
                break;
            case SWERVE_ONE:
                kTrackWidth = 0.449;
                kWheelBase = 0.464;
                break;
            case FROM_8048:
                kTrackWidth = 0.46;
                kWheelBase = 0.55; // approximate
                break;
            case BLANK: // for simulation
                kTrackWidth = 0.5;
                kWheelBase = 0.5;
                break;
            default:
                throw new IllegalStateException("Identity is not swerve: " + Identity.get().name());
        }

        kDriveKinematics = new SwerveDriveKinematics(
                new Translation2d(kWheelBase / 2, kTrackWidth / 2),
                new Translation2d(kWheelBase / 2, -kTrackWidth / 2),
                new Translation2d(-kWheelBase / 2, kTrackWidth / 2),
                new Translation2d(-kWheelBase / 2, -kTrackWidth / 2));
    }

    // TODO: what were these for?
    // public static final double ksVolts = 2;
    // public static final double kvVoltSecondsPerMeter = 2.0;
    // public static final double kaVoltSecondsSquaredPerMeter = 0.5;

    // SLOW SETTINGS
    public static final double kMaxSpeedMetersPerSecond = 3;
    public static final double kMaxAccelerationMetersPerSecondSquared = 10;
    // NOTE joel 2/8 used to be negative; inversions broken somewhere?
    public static final double kMaxAngularSpeedRadiansPerSecond = 5;
    public static final double kMaxAngularSpeedRadiansPerSecondSquared = 5;

    // FAST SETTINGS. can the robot actually go this fast?
    // public static final double kMaxSpeedMetersPerSecond = 8;
    // public static final double kMaxAccelerationMetersPerSecondSquared = 3;
    // public static final double kMaxAngularSpeedRadiansPerSecond = 10;
    // public static final double kMaxAngularSpeedRadiansPerSecondSquared = 100;

    private final SwerveModule m_frontLeft;
    private final SwerveModule m_frontRight;
    private final SwerveModule m_rearLeft;
    private final SwerveModule m_rearRight;

    // The gyro sensor. We have a Nav-X.
    public final AHRS m_gyro;
    // Odometry class for tracking robot pose
    private final SwerveDrivePoseEstimator m_poseEstimator;

    private double xVelocity = 0;
    private double yVelocity = 0;
    private double thetaVelociy = 0;

    private double x;
    private double y;
    private double rotation;
    private boolean isFieldRelative;

    public VisionDataProvider visionDataProvider;

    private boolean moving = false;

    // TODO: this looks unfinished?
    // public static final TrapezoidProfile.Constraints kXControllerConstraints =
    // new TrapezoidProfile.Constraints(kMaxSpeedMetersPerSecond,
    // kMaxAccelerationMetersPerSecondSquared);

    public final PIDController xController;
    public final PIDController yController;
    public final ProfiledPIDController headingController;
    public ProfiledPIDController thetaController;

    private final DoubleArrayPublisher robotPosePub;
    private final StringPublisher fieldTypePub;

    public SwerveDriveSubsystem(double currentLimit) {
        // Sets up Field2d pose tracking for glass.
        NetworkTableInstance inst = NetworkTableInstance.getDefault();
        NetworkTable fieldTable = inst.getTable("field");
        robotPosePub = fieldTable.getDoubleArrayTopic("robotPose").publish();
        fieldTypePub = fieldTable.getStringTopic(".type").publish();
        fieldTypePub.set("Field2d");

        final double Px = .15;
        final double Ix = 0;
        final double Dx = 0;
        final double xTolerance = 0.2;
        xController = new PIDController(Px, Ix, Dx);
        xController.setTolerance(xTolerance);

        final double Py = 0.15;
        final double Iy = 0;
        final double Dy = 0;
        final double yTolerance = 0.2;
        yController = new PIDController(Py, Iy, Dy);
        yController.setTolerance(yTolerance);

        final double Ptheta = 3;
        final double Itheta = 0;
        final double Dtheta = 0;
        final TrapezoidProfile.Constraints thetaControllerConstraints = new TrapezoidProfile.Constraints(
                kMaxAngularSpeedRadiansPerSecond, kMaxAngularSpeedRadiansPerSecondSquared);
        thetaController = new ProfiledPIDController(Ptheta, Itheta, Dtheta, thetaControllerConstraints);

        final TrapezoidProfile.Constraints headingControllConstraints = new TrapezoidProfile.Constraints(
                2*Math.PI, 2*Math.PI);

        headingController = new ProfiledPIDController( //
                0.7, // kP
                0.1, // kI
                0, //kD
                headingControllConstraints); // kD

        headingController.setIntegratorRange(-0.1, 0.1);
        // Note very low heading tolerance.
        headingController.setTolerance(Units.degreesToRadians(0.1));

        switch (Identity.get()) {
            case SQUAREBOT:
                m_frontLeft = WCPModule(
                        "Front Left",
                        11, // drive CAN
                        30, // turn CAN
                        2, // turn encoder
                        0.27, // turn offset
                        currentLimit);
                m_frontRight = WCPModule(
                        "Front Right",
                        12, // drive CAN
                        32, // turn CAN
                        0, // turn encoder
                        0.87, // turn offset
                        currentLimit);
                m_rearLeft = WCPModule(
                        "Rear Left",
                        21, // drive CAN
                        31, // turn CAN
                        3, // turn encoder
                        0.28, // turn offset
                        currentLimit);
                m_rearRight = WCPModule(
                        "Rear Right",
                        22, // drive CAN
                        33, // turn CAN
                        1, // turn encoder
                        0.47, // turn offset
                        currentLimit);
                break;
            case SWERVE_TWO:
                m_frontLeft = AMModule(
                        "Front Left",
                        11, // drive CAN
                        3, // turn PWM
                        1, // turn encoder
                        0.032635, // turn offset
                        currentLimit);
                m_frontRight = AMModule(
                        "Front Right",
                        12, // drive CAN
                        1, // turn PWM
                        3, // turn encoder
                        0.083566, // turn offset
                        currentLimit);
                m_rearLeft = AMModule(
                        "Rear Left",
                        21, // drive CAN
                        2, // turn PWM
                        0, // turn encoder
                        0.747865, // turn offset
                        currentLimit);
                m_rearRight = AMModule(
                        "Rear Right",
                        22, // drive CAN
                        0, // turn PWM
                        2, // turn encoder
                        0.727833, // turn offset
                        currentLimit);
                break;
            case SWERVE_ONE:
                m_frontLeft = AMModule(
                        "Front Left",
                        11, // drive CAN
                        0, // turn PWM0
                        3, // turn encoder
                        0.69, // turn offset
                        currentLimit);
                m_frontRight = AMModule(
                        "Front Right",
                        12, // drive CAN
                        2, // turn PWM
                        0, // turn encoder
                        0.72, // turn offset
                        currentLimit);
                m_rearLeft = AMModule(
                        "Rear Left",
                        21, // drive CAN
                        1, // turn PWM
                        2, // turn encoder
                        0.37, // turn offset
                        currentLimit);
                m_rearRight = AMModule(
                        "Rear Right",
                        22, // drive CAN
                        3, // turn PWM
                        1, // turn encoder
                        0.976726, // turn offset
                        currentLimit);
                break;
            case FROM_8048:
                m_frontLeft = AMCANModule(
                        "Front Left",
                        3, // drive CAN
                        8, // turn CAN
                        1, // turn encoder (confirmed)
                        0.355157, // turn offset
                        currentLimit);
                m_frontRight = AMCANModule(
                        "Front Right",
                        2, // drive CAN
                        6, // turn CAN
                        0, // turn encoder (confirmed)
                        0.404786, // turn offset
                        currentLimit);
                m_rearLeft = AMCANModule(
                        "Rear Left",
                        1, // drive CAN
                        9, // turn CAN
                        3, // turn encoder (confirmed)
                        0.238757, // turn offset
                        currentLimit);
                m_rearRight = AMCANModule(
                        "Rear Right",
                        4, // drive CAN
                        7, // turn CAN
                        2, // turn encoder (confirmed)
                        0.233683, // turn offset
                        currentLimit);
                break;
            case BLANK: // for simulation; just like squarebot for now
                m_frontLeft = WCPModule(
                        "Front Left",
                        11, // drive CAN
                        30, // turn CAN
                        2, // turn encoder
                        0.812, // turn offset
                        currentLimit);
                m_frontRight = WCPModule(
                        "Front Right",
                        12, // drive CAN
                        32, // turn CAN
                        0, // turn encoder
                        0.382, // turn offset
                        currentLimit);
                m_rearLeft = WCPModule(
                        "Rear Left",
                        21, // drive CAN
                        31, // turn CAN
                        3, // turn encoder
                        0.172, // turn offset
                        currentLimit);
                m_rearRight = WCPModule(
                        "Rear Right",
                        22, // drive CAN
                        33, // turn CAN
                        1, // turn encoder
                        0.789, // turn offset
                        currentLimit);
                break;
            default:
                throw new IllegalStateException("Identity is not swerve: " + Identity.get().name());
        }

        m_gyro = new AHRS(SerialPort.Port.kUSB);
        m_poseEstimator = new SwerveDrivePoseEstimator(
                kDriveKinematics,
                getHeading(),
                new SwerveModulePosition[] {
                        m_frontLeft.getPosition(),
                        m_frontRight.getPosition(),
                        m_rearLeft.getPosition(),
                        m_rearRight.getPosition()
                },
                new Pose2d(),
                VecBuilder.fill(0.03, 0.03, 0.03),
                VecBuilder.fill(0.01, 0.01, Integer.MAX_VALUE));
        visionDataProvider = new VisionDataProvider(m_poseEstimator, () -> getMoving(), () -> getPose());

        SmartDashboard.putData("Drive Subsystem", this);
    }

    private static SwerveModule WCPModule(
            String name,
            int driveMotorCanId,
            int turningMotorCanId,
            int turningEncoderChannel,
            double turningOffset,
            double currentLimit) {
        final double kWheelDiameterMeters = 0.1015; // WCP 4 inch wheel
        final double kDriveReduction = 5.50; // see wcproducts.com, this is the "fast" ratio.
        final double driveEncoderDistancePerTurn = kWheelDiameterMeters * Math.PI / kDriveReduction;
        final double turningGearRatio = 1.0;

        FalconDriveMotor driveMotor = new FalconDriveMotor(name, driveMotorCanId, currentLimit);
        FalconDriveEncoder driveEncoder = new FalconDriveEncoder(name, driveMotor, driveEncoderDistancePerTurn);
        NeoTurningMotor turningMotor = new NeoTurningMotor(name, turningMotorCanId);
        AnalogTurningEncoder turningEncoder = new AnalogTurningEncoder(name, turningEncoderChannel, turningOffset,
                turningGearRatio);

        // DRIVE PID
        PIDController driveController = new PIDController( //
                0.1, // kP
                0.3, // kI: nonzero I eliminates small errors, e.g. to finish rotations.
                0.0); // kD
        driveController.setIntegratorRange(-0.01, 0.01); // Note very low windup limit.

        // TURNING PID
        ProfiledPIDController turningController = new ProfiledPIDController(
                0.08, // kP: low P because not much reduction gearing.
                0.0, // kI
                0.0, // kD
                new TrapezoidProfile.Constraints( //
                        20 * Math.PI, // max angular speed radians/sec
                        20 * Math.PI)); // max accel radians/sec/sec
        turningController.enableContinuousInput(0, 2 * Math.PI);

        // DRIVE FF
        SimpleMotorFeedforward driveFeedforward = new SimpleMotorFeedforward( //
                0.05, // kS: from experiment; overcome friction for low-effort moves
                .5);// kV

        // TURNING FF
        SimpleMotorFeedforward turningFeedforward = new SimpleMotorFeedforward( //
                0.0, // kS: friction is unimportant
                0.02);// kV: from experiment; higher than AM modules, less reduction gear

        return new SwerveModule(name, driveMotor, turningMotor, driveEncoder, turningEncoder,
                driveController, turningController, driveFeedforward, turningFeedforward);
    }

    // for 8048's config
    private static SwerveModule AMCANModule(
            String name,
            int driveMotorCanId,
            int turningMotorCanId,
            int turningEncoderChannel,
            double turningOffset,
            double currentLimit) {
        final double kWheelDiameterMeters = 0.1016; // AndyMark Swerve & Steer has 4 inch wheel
        final double kDriveReduction = 6.67; // see andymark.com/products/swerve-and-steer
        final double driveEncoderDistancePerTurn = kWheelDiameterMeters * Math.PI / kDriveReduction;
        final double turningGearRatio = 1.0; // andymark ma3 encoder is 1:1

        FalconDriveMotor driveMotor = new FalconDriveMotor(name, driveMotorCanId, currentLimit);
        FalconDriveEncoder driveEncoder = new FalconDriveEncoder(name, driveMotor, driveEncoderDistancePerTurn);
        CANTurningMotor turningMotor = new CANTurningMotor(name, turningMotorCanId);
        AnalogTurningEncoder turningEncoder = new AnalogTurningEncoder(name, turningEncoderChannel, turningOffset,
                turningGearRatio);

        // DRIVE PID
        PIDController driveController = new PIDController( //
                0.1, // kP
                0, // kI: TODO: maybe more than zero?
                0); // kD

        // TURNING PID
        ProfiledPIDController turningController = new ProfiledPIDController( //
                0.5, // kP
                0, // kI
                0, // kD
                new TrapezoidProfile.Constraints(
                        20 * Math.PI, // speed rad/s
                        20 * Math.PI)); // accel rad/s/s
        turningController.enableContinuousInput(0, 2 * Math.PI);

        // DRIVE FF
        // TODO: real kS and kV
        SimpleMotorFeedforward driveFeedforward = new SimpleMotorFeedforward( //
                0.0, // kS
                .5); // kV

        // TURNING FF
        // TODO: high kS and low kV means kinda binary?
        SimpleMotorFeedforward turningFeedforward = new SimpleMotorFeedforward( //
                0.1, // kS: very high? TODO: is this right?
                0.005); // kV: very low? TODO: is this right?

        return new SwerveModule(name, driveMotor, turningMotor, driveEncoder, turningEncoder,
                driveController, turningController, driveFeedforward, turningFeedforward);

    }
    
    private static SwerveModule AMModule(
            String name,
            int driveMotorCanId,
            int turningMotorChannel,
            int turningEncoderChannel,
            double turningOffset,
            double currentLimit) {
        final double kWheelDiameterMeters = 0.1016; // AndyMark Swerve & Steer has 4 inch wheel
        final double kDriveReduction = 6.67; // see andymark.com/products/swerve-and-steer
        final double driveEncoderDistancePerTurn = kWheelDiameterMeters * Math.PI / kDriveReduction;
        final double turningGearRatio = 1.0; // andymark ma3 encoder is 1:1
        FalconDriveMotor driveMotor = new FalconDriveMotor(name, driveMotorCanId, currentLimit);
        FalconDriveEncoder driveEncoder = new FalconDriveEncoder(name, driveMotor, driveEncoderDistancePerTurn);
        PWMTurningMotor turningMotor = new PWMTurningMotor(name, turningMotorChannel);
        AnalogTurningEncoder turningEncoder = new AnalogTurningEncoder(name, turningEncoderChannel, turningOffset,
                turningGearRatio);

        // DRIVE PID
        PIDController driveController = new PIDController(//
                0.1, // kP
                0, // kI
                0.05);// kD

        // TURNING PID
        ProfiledPIDController turningController = new ProfiledPIDController(//
                0.5, // kP
                0, // kI
                0, // kD
                new TrapezoidProfile.Constraints(
                        20 * Math.PI, // speed rad/s
                        20 * Math.PI)); // accel rad/s/s
        turningController.enableContinuousInput(0, 2 * Math.PI);

        // DRIVE FF
        SimpleMotorFeedforward driveFeedforward = new SimpleMotorFeedforward(//
                0.04, // kS TODO: too low?
                0.2,// kV
                0.05); 

        // TURNING FF
        SimpleMotorFeedforward turningFeedforward = new SimpleMotorFeedforward(//
                0.05, // kS TODO too high?
                0.003,
                0); // kV TODO: too low?

        return new SwerveModule(name, driveMotor, turningMotor, driveEncoder, turningEncoder,
                driveController, turningController, driveFeedforward, turningFeedforward);
    }

    public void updateOdometry() {
        m_poseEstimator.update(
                getHeading(),
                new SwerveModulePosition[] {
                        m_frontLeft.getPosition(),
                        m_frontRight.getPosition(),
                        m_rearLeft.getPosition(),
                        m_rearRight.getPosition()
                });
        // {
        // if (m_pose.aprilPresent()) {
        // m_poseEstimator.addVisionMeasurement(
        // m_pose.getRobotPose(0),
        // Timer.getFPGATimestamp() - 0.3);
        // }

        // Update the Field2d widget
        Pose2d newEstimate = m_poseEstimator.getEstimatedPosition();
        robotPosePub.set(new double[] {
                newEstimate.getX(),
                newEstimate.getY(),
                newEstimate.getRotation().getDegrees()
        });
    }

    @Override
    public void periodic() {
        updateOdometry();
        RobotContainer.m_field.setRobotPose(m_poseEstimator.getEstimatedPosition());
    }

    public Pose2d getPose() {
        return m_poseEstimator.getEstimatedPosition();
    }

    public double getRadians() {
        return m_poseEstimator.getEstimatedPosition().getRotation().getRadians();
    }

    public void resetPose(Pose2d robotPose) {
        m_poseEstimator.resetPosition(getHeading(), new SwerveModulePosition[] {
                m_frontLeft.getPosition(),
                m_frontRight.getPosition(),
                m_rearLeft.getPosition(),
                m_rearRight.getPosition()
        },
                robotPose);
    }

    public boolean getMoving() {
        return moving;
    }

    /**
     * Method to drive the robot using joystick info.
     *
     * @param xSpeed        Speed of the robot in the x direction (positive =
     *                      forward). X is between -1 and 1
     * @param ySpeed        Speed of the robot in the y direction (positive =
     *                      leftward). Y is between -1 and 1
     * @param rot           Angular rate of the robot (positive = counterclockwise)
     * @param fieldRelative Whether the provided x and y speeds are relative to the
     *                      field.
     */
    @SuppressWarnings("ParameterName")
    public void drive(double xSpeed, double ySpeed, double rot, boolean fieldRelative) {
        // TODO Fix this number

        x = xSpeed;
        y = ySpeed;
        rotation = rot;
        isFieldRelative = fieldRelative;

        if (Math.abs(xSpeed) < .01)
            xSpeed = 100 * xSpeed * xSpeed * Math.signum(xSpeed);
        if (Math.abs(ySpeed) < .01)
            ySpeed = 100 * ySpeed * ySpeed * Math.signum(ySpeed);

        if (Math.abs(rot) < .01)
            rot = 0;

        var swerveModuleStates = kDriveKinematics.toSwerveModuleStates(
                fieldRelative
                        ? ChassisSpeeds.fromFieldRelativeSpeeds(kMaxSpeedMetersPerSecond * xSpeed,
                                kMaxSpeedMetersPerSecond * ySpeed, kMaxAngularSpeedRadiansPerSecond * rot,
                                getPose().getRotation())
                        : new ChassisSpeeds(kMaxSpeedMetersPerSecond * xSpeed, kMaxSpeedMetersPerSecond * ySpeed,
                                kMaxAngularSpeedRadiansPerSecond * rot));

        SwerveDriveKinematics.desaturateWheelSpeeds(
                swerveModuleStates, kMaxSpeedMetersPerSecond);

        getRobotVelocity(swerveModuleStates);

        m_frontLeft.setDesiredState(swerveModuleStates[0]);
        m_frontRight.setDesiredState(swerveModuleStates[1]);
        m_rearLeft.setDesiredState(swerveModuleStates[2]);
        m_rearRight.setDesiredState(swerveModuleStates[3]);
    }

    public void setModuleStates(SwerveModuleState[] desiredStates) {
        SwerveDriveKinematics.desaturateWheelSpeeds(
                desiredStates, kMaxSpeedMetersPerSecond);
        m_frontLeft.setDesiredState(desiredStates[0]);
        m_frontRight.setDesiredState(desiredStates[1]);
        m_rearLeft.setDesiredState(desiredStates[2]);
        m_rearRight.setDesiredState(desiredStates[3]);

        getRobotVelocity(desiredStates);

    }

    public void getRobotVelocity(SwerveModuleState[] desiredStates) {
        ChassisSpeeds chassisSpeeds = kDriveKinematics.toChassisSpeeds(desiredStates[0], desiredStates[1],
                desiredStates[2], desiredStates[3]);
        xVelocity = chassisSpeeds.vxMetersPerSecond;
        yVelocity = chassisSpeeds.vyMetersPerSecond;
        thetaVelociy = chassisSpeeds.omegaRadiansPerSecond;

        if (xVelocity >= 0.1 || yVelocity >= 0.1 || thetaVelociy >= 0.1) {
            moving = true;
        } else {
            moving = false;
        }

    }

    public void test(double[][] desiredOutputs) {
        m_frontLeft.setOutput(desiredOutputs[0][0], desiredOutputs[0][1]);
        m_frontRight.setOutput(desiredOutputs[1][0], desiredOutputs[1][1]);
        m_rearLeft.setOutput(desiredOutputs[2][0], desiredOutputs[2][1]);
        m_rearRight.setOutput(desiredOutputs[3][0], desiredOutputs[3][1]);
    }

    /** Resets the drive encoders to currently read a position of 0. */
    public void resetEncoders() {
        m_frontLeft.resetDriveEncoders();
        m_frontRight.resetDriveEncoders();
        m_rearLeft.resetDriveEncoders();
        m_rearRight.resetDriveEncoders();
    }

    public Rotation2d getHeading() {
        return Rotation2d.fromDegrees(-m_gyro.getFusedHeading());
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        super.initSendable(builder);
        builder.addDoubleProperty("heading_radians", () -> 2 + this.getHeading().getRadians(), null);
        builder.addDoubleProperty("translationalx", () -> getPose().getX(), null);
        builder.addDoubleProperty("translationaly", () -> getPose().getY(), null);
        builder.addDoubleProperty("theta", () -> getPose().getRotation().getRadians(), null);
        builder.addDoubleProperty("Front Left Position", () -> m_frontLeft.getPosition().distanceMeters, null);
        builder.addDoubleProperty("Front Right Position", () -> m_frontRight.getPosition().distanceMeters, null);
        builder.addDoubleProperty("Rear Left Position", () -> m_rearLeft.getPosition().distanceMeters, null);
        builder.addDoubleProperty("Rear Right Position", () -> m_rearRight.getPosition().distanceMeters, null);
        builder.addDoubleProperty("Theta Controller Error", () -> thetaController.getPositionError(), null);
        builder.addDoubleProperty("Theta Controller Measurment", () -> getPose().getRotation().getRadians(), null);
        builder.addDoubleProperty("Theta Controller Setpoint", () -> thetaController.getSetpoint().position, null);

        builder.addDoubleProperty("Front Left Turning Controller Output", () -> m_frontLeft.getTControllerOutput(),
                null);
        builder.addDoubleProperty("Front Right Turning Controller Output", () -> m_frontRight.getTControllerOutput(),
                null);
        builder.addDoubleProperty("Rear Left Turning Controller Output", () -> m_rearLeft.getTControllerOutput(), null);
        builder.addDoubleProperty("Rear Right Turning Controller Output", () -> m_rearRight.getTControllerOutput(),
                null);
        builder.addDoubleProperty("Front Left Driving Controller Output", () -> m_frontLeft.getDControllerOutput(),
                null);
        builder.addDoubleProperty("Front Right Driving Controller Output", () -> m_frontRight.getDControllerOutput(),
                null);
        builder.addDoubleProperty("Rear Left Driving Controller Output", () -> m_rearLeft.getDControllerOutput(), null);
        builder.addDoubleProperty("Rear Right Driving Controller Output", () -> m_rearRight.getDControllerOutput(),
                null);

        builder.addDoubleProperty("X controller Error", () -> xController.getPositionError(), null);
        builder.addDoubleProperty("X controller Setpoint", () -> xController.getSetpoint(), null);
        builder.addDoubleProperty("X controller Measurment", () -> getPose().getX(), null);

        builder.addDoubleProperty("Y controller Error", () -> yController.getPositionError(), null);
        builder.addDoubleProperty("Y controller Setpoint", () -> yController.getSetpoint(), null);
        builder.addDoubleProperty("Y controller Measurment", () -> getPose().getY(), null);

        builder.addBooleanProperty("Moving", () -> getMoving(), null);

        builder.addDoubleProperty("X controller Velocity", () -> xVelocity, null);
        builder.addDoubleProperty("Y controller Velocity", () -> yVelocity, null);
        builder.addDoubleProperty("Theta controller Velocity", () -> thetaVelociy, null);
        builder.addDoubleProperty("Pitch", () -> m_gyro.getPitch(), null);
        builder.addDoubleProperty("Roll", () -> m_gyro.getRoll(), null);
        builder.addDoubleProperty("Heading Degrees", () -> getHeading().getDegrees(), null);
        builder.addDoubleProperty("xSpeed", () -> x, null);
        builder.addDoubleProperty("ySpeed", () -> y, null);
        builder.addDoubleProperty("Rotation", () -> rotation, null);
        builder.addBooleanProperty("Field Relative", () -> isFieldRelative, null);
        // builder.addDoubleProperty("Identity", () -> Identity.get(), null);

        builder.addDoubleProperty("Front Left Output", () -> m_frontLeft.getDriveOutput(), null);
        builder.addDoubleProperty("Front Right Output", () -> m_frontRight.getDriveOutput(), null);
        builder.addDoubleProperty("Rear Left Output", () -> m_rearLeft.getDriveOutput(), null);
        builder.addDoubleProperty("Rear Right Output", () -> m_rearLeft.getDriveOutput(), null);
    }
}
