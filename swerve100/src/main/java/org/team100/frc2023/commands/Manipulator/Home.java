package org.team100.frc2023.commands.Manipulator;

import org.team100.frc2023.subsystems.Manipulator;

import edu.wpi.first.wpilibj2.command.CommandBase;

public class Home extends CommandBase {
    Manipulator m_manipulator;

    public Home(Manipulator manipulator) {
        m_manipulator = manipulator;
        addRequirements(m_manipulator);
    }

    @Override
    public void initialize() {
        m_manipulator.set(0.8, 30);
    }

    @Override
    public void execute() {
    }
}
