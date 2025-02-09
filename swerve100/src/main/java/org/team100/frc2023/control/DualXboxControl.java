package org.team100.frc2023.control;

import static org.team100.lib.control.ControlUtil.clamp;
import static org.team100.lib.control.ControlUtil.deadband;
import static org.team100.lib.control.ControlUtil.expo;

import org.team100.frc2023.autonomous.DriveToWaypoint2;
import org.team100.frc2023.autonomous.MoveConeWidth;
import org.team100.frc2023.autonomous.Rotate;
import org.team100.frc2023.commands.AutoLevel;
import org.team100.frc2023.commands.Defense;
import org.team100.frc2023.commands.DriveScaled;
import org.team100.frc2023.commands.GoalOffset;
import org.team100.frc2023.commands.RumbleOn;
import org.team100.frc2023.commands.Arm.ArmTrajectory;
import org.team100.frc2023.commands.Arm.Oscillate;
import org.team100.frc2023.commands.Arm.SetConeMode;
import org.team100.frc2023.commands.Arm.SetCubeMode;
import org.team100.frc2023.commands.Manipulator.CloseSlow;
import org.team100.frc2023.commands.Manipulator.Eject;
import org.team100.frc2023.commands.Manipulator.Home;
import org.team100.frc2023.commands.Manipulator.Open;
import org.team100.frc2023.commands.Retro.DriveToRetroReflectiveTape;
import org.team100.lib.commands.ResetPose;
import org.team100.lib.commands.ResetRotation;
import org.team100.lib.commands.Retro.LedOn;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.button.Trigger;

/**
 * see
 * https://docs.google.com/document/d/1M89x_IiguQdY0VhQlOjqADMa6SYVp202TTuXZ1Ps280/edit#
 */
public class DualXboxControl implements Control, Sendable {
    private static final double kDeadband = 0.02;
    private static final double kExpo = 0.5;

    // private static final double kDtSeconds = 0.02;
    // private static final double kMaxRotationRateRadiansPerSecond = Math.PI;
    private static final double kTriggerThreshold = .5;

    private final CommandXboxController controller0;
    private final CommandXboxController controller1;
    Rotation2d previousRotation = new Rotation2d(0);

    public DualXboxControl() {
        controller0 = new CommandXboxController(0);
        System.out.printf("Controller0: %s\n", controller0.getHID().getName());
        controller1 = new CommandXboxController(1);
        System.out.printf("Controller1: %s\n", controller1.getHID().getName());
        SmartDashboard.putData("Robot Container", this);
    }

    ///////////////////////////////
    //
    // DRIVER: manual driving and auto navigation controls

    @Override
    public void driveToLeftGrid(DriveToWaypoint2 command) {
        // controller0.x().whileTrue(command);
    };

    @Override
    public void autoLevel(AutoLevel command) {
        // controller0.x().whileTrue(command);
    }

    @Override
    public void driveToCenterGrid(DriveToWaypoint2 command) {
        // controller0.a().whileTrue(command);
    };

    @Override
    public void driveToRightGrid(DriveToWaypoint2 command) {
        // controller0.b().whileTrue(command);
    };

    @Override
    public void driveToSubstation(DriveToWaypoint2 command) {
        // controller0.y().whileTrue(command);
    };

    @Override
    public void resetRotation0(ResetRotation command) {
        JoystickButton startButton = new JoystickButton(controller0.getHID(), 7);
        startButton.onTrue(command);
    }

    @Override
    public void resetRotation180(ResetRotation command) {
        JoystickButton startButton = new JoystickButton(controller0.getHID(), 8);
        startButton.onTrue(command);
    }

    @Override
    public Twist2d twist() {
        double dx = expo(deadband(-1.0 * clamp(controller0.getRightY(), 1), kDeadband, 1), kExpo);
        double dy = expo(deadband(-1.0 * clamp(controller0.getRightX(), 1), kDeadband, 1), kExpo);
        double dtheta = expo(deadband(-1.0 * clamp(controller0.getLeftX(), 1), kDeadband, 1), kExpo);
        return new Twist2d(dx, dy, dtheta);
    }

    @Override
    public Trigger trigger() {
        return controller0.rightBumper();
    }

    @Override
    public Trigger thumb() {
        return controller0.leftBumper();
    }

    @Override
    public void driveSlow(DriveScaled command) {
        controller0.leftBumper().whileTrue(command);
    }

    @Override
    public void resetPose(ResetPose command) {
        // controller0.leftBumper().onTrue(command);
    }

    @Override
    public Rotation2d desiredRotation() {
        double desiredAngleDegrees = controller0.getHID().getPOV();

        if (desiredAngleDegrees < 0) {
            return null;
        }
        previousRotation = Rotation2d.fromDegrees(-1.0 * desiredAngleDegrees);
        return previousRotation;
    }

    @Override
    public GoalOffset goalOffset() {
        double left = controller0.getLeftTriggerAxis();
        double right = controller0.getRightTriggerAxis();
        if (left > kTriggerThreshold) {
            if (right > kTriggerThreshold) {
                return GoalOffset.center;
            }
            return GoalOffset.left;
        }
        if (right > kTriggerThreshold) {
            return GoalOffset.right;
        }
        return GoalOffset.center;
    }

    @Override
    public void defense(Defense defense) {
        JoystickButton button = new JoystickButton(controller0.getHID(), 2);

        button.whileTrue(defense);
    }

    @Override
    public void rumbleOn() {
        controller0.getHID().setRumble(RumbleType.kLeftRumble, 0.0);
        controller0.getHID().setRumble(RumbleType.kRightRumble, 0.0);
    }

    @Override
    public void rumbleTrigger(RumbleOn command) {
        controller0.a().whileTrue(command);
    }

    @Override
    public void rumbleOff() {
        controller0.getHID().setRumble(RumbleType.kLeftRumble, 0);
        controller0.getHID().setRumble(RumbleType.kRightRumble, 0);

    }

    @Override
    public void rotate0(Rotate command) {
        JoystickButton button = new JoystickButton(controller0.getHID(), 9);
        button.whileTrue(command);
    }

    @Override
    public void driveMedium(DriveScaled command) {
        controller0.rightBumper().whileTrue(command);
    }

    @Override
    public void moveConeWidthLeft(MoveConeWidth command) {
        controller0.y().whileTrue(command);
    }

    @Override
    public void moveConeWidthRight(MoveConeWidth command) {
        controller0.a().whileTrue(command);
    }

    ///////////////////////////////
    //
    // OPERATOR: arm and manipulator controls

    /** @return [-1,1] */
    @Override
    public double openSpeed() {
        return controller1.getRightTriggerAxis();
    }

    /** @return [-1,1] */
    @Override
    public double closeSpeed() {
        return controller1.getLeftTriggerAxis();
    }

    /** @return [-1,1] */
    @Override
    public double lowerSpeed() {
        return controller1.getRightX();
    }

    /** @return [-1,1] */
    @Override
    public double upperSpeed() {
        return controller1.getLeftY();
    }

    @Override
    public void armHigh(ArmTrajectory command) {
        controller1.povUp().whileTrue(command);
    }

    @Override
    public void armLow(ArmTrajectory command) {
        controller1.povLeft().whileTrue(command);
    }

    @Override
    public void armSafe(ArmTrajectory command) {
        controller1.povDown().whileTrue(command);
    }

    @Override
    public void safeWaypoint(ArmTrajectory command) {
        // SequentialCommandGroup commandGroup = new SequentialCommandGroup(command,
        // comman)
        // controller1.rightBumper().whileTrue(command);
    }

    @Override
    public void armSafeSequential(ArmTrajectory command, ArmTrajectory command2) {
        SequentialCommandGroup commandGroup = new SequentialCommandGroup(command, command2);
        controller1.povDown().whileTrue(commandGroup);
    }

    @Override
    public void armSafeBack(ArmTrajectory command) {
        // controller1.leftBumper().whileTrue(command);
    }

    @Override
    public void closeSlow(CloseSlow command) {
        // controller1.leftBumper().whileTrue(command);

        // controller1.a().whileTrue(command);

        controller1.leftBumper().whileTrue(command);
    }

    @Override
    public void armSubstation(ArmTrajectory command) {
        controller1.povRight().whileTrue(command);
    }

    @Override
    public void armMid(ArmTrajectory command) {
        JoystickButton button = new JoystickButton(controller0.getHID(), 7);
        button.whileTrue(command);
    }

    @Override
    public void open(Open command) {
        // controller1.a().whileTrue(command);
    }

    @Override
    public void home(Home command) {
        controller1.b().whileTrue(command);
    }

    @Override
    public void close(Eject command) {
        controller1.x().whileTrue(command);
    }

    @Override
    public void cubeMode(SetCubeMode command) {
        controller1.y().onTrue(command);
    }

    @Override
    public void coneMode(SetConeMode command) {
        controller1.a().onTrue(command);
    }

    @Override
    public void armToSub(ArmTrajectory command) {
        // JoystickButton button = new JoystickButton(controller1.getHID(), 7);
        // button.onTrue(command);

        // controller1.rightBumper().whileTrue(command);
    }

    @Override
    public void ledOn(LedOn command) {
        // controller1.rightBumper().whileTrue(command);
    }

    @Override
    public void oscillate(Oscillate command) {
        controller1.rightBumper().whileTrue(command);
    }

    @Override
    public void tapeDetect(DriveToRetroReflectiveTape command) {
        // controller1.leftBumper().whileTrue(command);
    }

    @Override
    public void armSubSafe(ArmTrajectory command) {
        // controller1.rightBumper().whileTrue(command);
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("xbox control");
        builder.addDoubleProperty("right y", () -> controller0.getRightY(), null);
        builder.addDoubleProperty("right x", () -> controller0.getRightX(), null);
        builder.addDoubleProperty("left x", () -> controller0.getLeftX(), null);

        // builder.addDoubleProperty("x limited", () -> xLimited(), null);
        // builder.addDoubleProperty("y limtied", () -> yLimited(), null);
        // builder.addDoubleProperty("rot Limited", () -> rotLimited(), null);
    }
}
