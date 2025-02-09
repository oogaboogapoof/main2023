package org.team100.lib.subsystems.drive;

import com.ctre.phoenix.ErrorCode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.StatorCurrentLimitConfiguration;
import com.ctre.phoenix.motorcontrol.SupplyCurrentLimitConfiguration;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;
import com.ctre.phoenix.sensors.SensorVelocityMeasPeriod;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Uses default position/velocity sensor which is the integrated one.
 * 
 * See details on velocity averaging and sampling.
 * https://v5.docs.ctr-electronics.com/en/stable/ch14_MCSensor.html#velocity-measurement-filter
 * 
 * Summarized:
 * 
 * * every 1ms, the velocity is measured as (x(t) - x(t-p)) / p
 * * N such measurements are averaged
 * 
 * Default p is 100 ms, default N is 64, so, if the velocity measurement is
 * perfect, it represents the actual velocity 82 ms ago, which is a long time.
 * 
 * For awhile we had p set to 0.001 and N set to 1. It's kind of amazing that it
 * worked at all. Full speed is roughly 6k rpm or 100 rev/s or 0.1 rev per
 * sample or 200 ticks per sample, so that's fine. But moving slowly, say 0.1
 * m/s, that's 2 ticks per sample, which means that the velocity controller is
 * going to see a lot of quantization noise in the measurement.
 * 
 * A better setting would use a window that's about the width of the control
 * period, 20ms, so try 10ms and 8 samples. Note, this delay will be noticeable.
 * 
 * TODO: deal with delay in velocity measurement.
 */
public class FalconDriveMotor implements DriveMotor {
    private final WPI_TalonFX m_motor;

    /**
     * Throws if any of the configurations fail.
     */
    public FalconDriveMotor(String name, int canId, double currentLimit) {
        m_motor = new WPI_TalonFX(canId);
        m_motor.configFactoryDefault();
        m_motor.setNeutralMode(NeutralMode.Brake);
        m_motor.configStatorCurrentLimit(new StatorCurrentLimitConfiguration(true, currentLimit, currentLimit, 0));
        m_motor.configSupplyCurrentLimit(new SupplyCurrentLimitConfiguration(true, currentLimit, currentLimit, 0));
        m_motor.configVelocityMeasurementPeriod(SensorVelocityMeasPeriod.Period_10Ms);
        m_motor.configVelocityMeasurementWindow(8);
        SmartDashboard.putData(String.format("Falcon Drive Motor %s", name), this);
    }

    private void require(ErrorCode errorCode) {
        if (errorCode != ErrorCode.OK)
            throw new IllegalArgumentException("motor configuration error: " + errorCode.name());
    }

    @Override
    public double get() {
        return m_motor.get();
    }

    @Override
    public void set(double output) {
        m_motor.setVoltage(10 * MathUtil.clamp(output, -1.3, 1.3));
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("FalconDriveMotor");
        builder.addDoubleProperty("Device ID", () -> m_motor.getDeviceID(), null);
        builder.addDoubleProperty("Output", this::get, null);
    }

    /**
     * @return integrated sensor position in sensor units (1/2048 turn).
     */
    double getPosition() {
        return m_motor.getSelectedSensorPosition();
    }

    /**
     * @return integrated sensor velocity in sensor units (1/2048 turn) per 100ms.
     */
    double getVelocity() {
        return m_motor.getSelectedSensorVelocity();
    }

    /**
     * Sets integrated sensor position to zero.
     * Throws if it fails, watch out, don't use this during a match.
     */
    void resetPosition() {
        require(m_motor.setSelectedSensorPosition(0));
    }
}
