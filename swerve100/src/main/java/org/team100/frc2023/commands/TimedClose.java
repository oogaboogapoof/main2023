package org.team100.frc2023.commands;

import org.team100.frc2023.subsystems.Manipulator;

import edu.wpi.first.wpilibj2.command.CommandBase;

public class TimedClose extends CommandBase {
    int loopCount;
    int duration;
    double force;
    boolean finishedFlag;
    // duration is given in miliseconds and force is in between 0 and 1

    private final Manipulator manip;

    public TimedClose(Manipulator subsystem, int durationParm, double forceParm) {
        manip = subsystem;
        duration = durationParm;
        force = forceParm;
        addRequirements(subsystem);
    }

    @Override
    public void initialize() {
        loopCount = duration / 20;
        finishedFlag = false;

    }

    @Override
    public void execute() {
        manip.set(force, 30);
        if (loopCount-- <= 0) {
            finishedFlag = true;
        }

    }

    @Override
    public void end(boolean interrupted) {
        manip.set(0, 30);
    }

    @Override
    public boolean isFinished() {
        return finishedFlag;
    }
}
