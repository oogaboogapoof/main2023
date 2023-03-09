package frc.robot;

import java.io.IOException;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.ConditionalCommand;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.autonomous.Circle;
import frc.robot.autonomous.DriveToAprilTag;
import frc.robot.autonomous.DriveToWaypoint2;
import frc.robot.autonomous.MoveToAprilTag;
import frc.robot.autonomous.SanjanAutonomous;
import frc.robot.autonomous.VasiliAutonomous;
import frc.robot.commands.DriveRotation;
import frc.robot.commands.DriveSlow;
import frc.robot.commands.DriveWithHeading;
import frc.robot.commands.ResetPose;
import frc.robot.commands.ResetRotation;
import frc.robot.commands.AutoLevel;
import frc.robot.commands.Arm.ArmTrajectory;
import frc.robot.commands.Arm.DriveToSetpoint;
import frc.robot.commands.Arm.ManualArm;
import frc.robot.commands.Arm.SetConeMode;
import frc.robot.commands.Arm.SetCubeMode;
import frc.robot.commands.Manipulator.Close;
import frc.robot.commands.Manipulator.Home;
import frc.robot.commands.Manipulator.Open;
import frc.robot.subsystems.Manipulator;
import frc.robot.subsystems.SwerveDriveSubsystem;
import frc.robot.subsystems.Arm.ArmController;
import frc.robot.subsystems.Arm.ArmPosition;
import team100.commands.DriveManually;
import team100.commands.GripManually;
import team100.control.DualXboxControl;

@SuppressWarnings("unused")
public class RobotContainer implements Sendable {
    

    // CONFIG
    private final DriverStation.Alliance m_alliance;

    // SUBSYSTEMS
    private final SwerveDriveSubsystem m_robotDrive;
    private final Manipulator manipulator;
    private final ArmController armController;

    // CONTROL
    private final DualXboxControl control;

    // COMMANDS
    private final AutoLevel autoLevel;
    private final DriveManually driveManually;
    private final GripManually gripManually;

    private final ManualArm manualArm;


    private final DriveWithHeading driveWithHeading;
    private final DriveRotation driveRotation;

    private final DriveToAprilTag driveToSubstation, driveToLeftGrid, driveToCenterGrid, driveToRightGrid;

    private final ArmTrajectory armHigh;
    private final ArmTrajectory armSafe;
    private final ArmTrajectory armSubstation;
    private final SetCubeMode setCubeMode; 
    private final SetConeMode setConeMode; 


    private final Home homeCommand;
    private final Close closeCommand;
    private final Open openCommand;

    private final DriveSlow driveSlow;

    public final static Field2d m_field = new Field2d();

    public RobotContainer() throws IOException {
        // THIS IS FROM BOB'S DELETED CODE

        final double kDriveCurrentLimit = 40;
        manipulator = new Manipulator();
        armController = new ArmController();

        // // NEW CONTROL
        control = new DualXboxControl();

        // m_alliance = DriverStation.getAlliance();

        m_alliance = DriverStation.Alliance.Red;

        m_robotDrive = new SwerveDriveSubsystem(m_alliance, kDriveCurrentLimit);
        if (m_alliance == DriverStation.Alliance.Blue) {
            driveToLeftGrid = DriveToAprilTag.newDriveToAprilTag(6, 1.889, .55, control::goalOffset, m_robotDrive);
            driveToCenterGrid = DriveToAprilTag.newDriveToAprilTag(7, 1.889, .55, control::goalOffset, m_robotDrive);
            driveToRightGrid = DriveToAprilTag.newDriveToAprilTag(8, 1.889, .55, control::goalOffset, m_robotDrive);
            driveToSubstation = DriveToAprilTag.newDriveToAprilTag(4, 0.01, -0.762, control::goalOffset, m_robotDrive);
        } else {
            driveToLeftGrid = DriveToAprilTag.newDriveToAprilTag(1, 0.95, .55, control::goalOffset, m_robotDrive);
            driveToCenterGrid = DriveToAprilTag.newDriveToAprilTag(2, 0.95, .55, control::goalOffset, m_robotDrive);
            driveToRightGrid = DriveToAprilTag.newDriveToAprilTag(3, 0.95, .55, control::goalOffset, m_robotDrive);
            driveToSubstation = DriveToAprilTag.newDriveToAprilTag(5, 0.05, -.53, control::goalOffset, m_robotDrive);
        }


        
        
        armHigh = new ArmTrajectory(ArmPosition.HIGH, armController);

        armSafe = new ArmTrajectory(ArmPosition.SAFE, armController);

        armSubstation = new ArmTrajectory(ArmPosition.SUB, armController);

        ResetRotation resetRotation = new ResetRotation(m_robotDrive, new Rotation2d());
        autoLevel = new AutoLevel(m_robotDrive.m_gyro, m_robotDrive);

        ResetPose resetPose = new ResetPose(m_robotDrive, 0, 0, 0);

        homeCommand = new Home(manipulator);

        openCommand = new Open(manipulator);

        closeCommand = new Close(manipulator);

        setCubeMode = new SetCubeMode(armController);
        
        setConeMode = new SetConeMode(armController);

        driveSlow = new DriveSlow(m_robotDrive, control);


        driveWithHeading = new DriveWithHeading(
                m_robotDrive,
                control::xSpeed,
                control::ySpeed,
                control::desiredRotation,
                control::rotSpeed,
                "");

        driveRotation = new DriveRotation(m_robotDrive, control::rotSpeed);

        manualArm = new ManualArm(armController, control.getController());

        control.resetRotation(resetRotation);
        control.autoLevel(autoLevel);
        control.resetPose(resetPose);
        control.driveToLeftGrid(driveToLeftGrid);
        control.driveToCenterGrid(driveToCenterGrid);
        control.driveToRightGrid(driveToRightGrid);
        control.driveToSubstation(driveToSubstation);
        control.resetPose(resetPose);

        
        control.armHigh(armHigh);
        control.armSafe(armSafe);
        // control.armSubstation(armSubstation);

        // control.open(openCommand);
        control.close(closeCommand);
        control.home(homeCommand);

        control.coneMode(setConeMode);
        control.cubeMode(setCubeMode);

        control.driveSlow(driveSlow);

        control.armSubstation(armSubstation);





        // DEFAULT COMMANDS
        // Controller 0 right => cartesian, left => rotation
        driveManually = new DriveManually(
                control::xSpeed,
                control::ySpeed,
                control::rotSpeed,
                m_robotDrive);

        m_robotDrive.setDefaultCommand(driveWithHeading);

        control.driveRotation(driveRotation);

        // Controller 1 triggers => manipulator open/close
        gripManually = new GripManually(
                control::openSpeed,
                control::closeSpeed,
                manipulator);

        // manipulator.setDefaultCommand(gripManually);

        manipulator.setDefaultCommand(new RunCommand(() -> { manipulator.pinch(0); }, manipulator));

        armController.setDefaultCommand(manualArm);

        SmartDashboard.putData("Robot Container", this);
    }

    

    public Command getAutonomousCommand2() {
        // return new SequentialCommandGroup(
        // new IshanAutonomous(m_robotDrive),
        // new IshanAutonomous(m_robotDrive)
        // );

        return new VasiliAutonomous(m_robotDrive);

        // return new SanjanAutonomous(m_robotDrive);
    }

    public void runTest() {
        XboxController controller0 = new XboxController(0);
        System.out.printf(
                "name: %s   left X: %5.2f   left Y: %5.2f   right X: %5.2f   right Y: %5.2f   left T: %5.2f   right T: %5.2f\n",
                DriverStation.getJoystickName(0),
                // DriverStation.getStickAxis(0, 0),
                controller0.getLeftX(), // 0 = GP right X, -0.66 to 0.83
                controller0.getLeftY(), // 1 = GP right Y, -0.64 to 0.64
                controller0.getRightX(), // 4 = GP left X, -0.7 to 0.8
                controller0.getRightY(), // 5
                controller0.getLeftTriggerAxis(), // 2 = GP left Y, -0.64 to 0.6
                controller0.getRightTriggerAxis() // 3
        );
    }

    public void runTest2() {
        XboxController controller0 = new XboxController(0);
        boolean rearLeft = controller0.getAButton();
        boolean rearRight = controller0.getBButton();
        boolean frontLeft = controller0.getXButton();
        boolean frontRight = controller0.getYButton();
        double driveControl = controller0.getLeftY();
        double turnControl = controller0.getLeftX();
        double[][] desiredOutputs = {
                { frontLeft ? driveControl : 0, frontLeft ? turnControl : 0 },
                { frontRight ? driveControl : 0, frontRight ? turnControl : 0 },
                { rearLeft ? driveControl : 0, rearLeft ? turnControl : 0 },
                { rearRight ? driveControl : 0, rearRight ? turnControl : 0 }
        };
        m_robotDrive.test(desiredOutputs);
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("container");
        builder.addDoubleProperty("theta controller error", () -> m_robotDrive.thetaController.getPositionError(),
                null);
        builder.addDoubleProperty("x controller error", () -> m_robotDrive.xController.getPositionError(), null);
        builder.addDoubleProperty("y controller error", () -> m_robotDrive.yController.getPositionError(), null);

    }
}
