package org.team100.lib.motors;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.InvertType;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.SensorCollection;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.sensors.SensorVelocityMeasPeriod;

import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;

/**
 * An abstraction for the Talon SRX for debugging information
 */
public class FRCTalonSRX implements Sendable {

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("Text View");
        builder.addDoubleProperty("EncoderPosition", this::getSelectedSensorPosition, null);
        builder.addDoubleProperty("EncoderSpeed", this::getSensorVelocity, null);
        builder.addDoubleProperty("Analog Input", this.m_sensorCollection::getAnalogIn, null);
        builder.addBooleanProperty("Fwd Limit", this.m_sensorCollection::isFwdLimitSwitchClosed, null);
        builder.addBooleanProperty("Rev Limit", this.m_sensorCollection::isRevLimitSwitchClosed, null);

    }

    public void reset() {
        this.motor.configFactoryDefault();
    }

    public void driveVelocity(double velocity) {
        this.motor.set(ControlMode.Velocity, velocity);
    }

    public void drivePercentOutput(double percentOutput) {
        this.motor.set(ControlMode.PercentOutput, percentOutput);

    }

    public void driveMotionMagic(double setpoint) {
        this.motor.set(ControlMode.MotionProfile, setpoint);
    }

    public void drivePosition(double setpoint) {
        this.motor.set(ControlMode.Position, setpoint);
    }

    public void driveCurrent(double current) {
        this.motor.set(ControlMode.Current, current);
    }

    public double getSensorVelocity() {
        return this.motor.getSelectedSensorVelocity();
    }

    public double getSelectedSensorPosition() {
        return this.motor.getSelectedSensorPosition();
    }

    ///////////////////////////////////////////////////////////////////////////
    /**
     * A direct reference to the TalonSRX motor, designed for direct control
     */
    public WPI_TalonSRX motor;

    /**
     * The master that will be followed
     */
    public FRCTalonSRX master;
    ///////////////////////////////////////////////////////////////////////////
    public SensorCollection m_sensorCollection;

    /**
     * The Can ID of the selected motor
     */
    private int canID;

    /**
     * The inversion of the motor
     *
     * true inverts the motor
     */
    private boolean inverted;

    /**
     * The inversion of the motor
     *
     * Uses CTRE InvertType
     */
    private InvertType invertType;

    /**
     * Determines what mode to use for inversion
     * 
     * true uses InvertType invertType
     * false uses boolean inverted
     */
    private boolean useInvertType;

    /**
     * The feedback port of the motor Default is 0
     */
    private int feedbackPort = 0;

    /**
     * The timeout of the motor in ms
     *
     * Default is 10
     */
    private int timeout = 10;

    /**
     * The sensor phase of the motor
     *
     * true inverts the encoder signal
     */
    private boolean sensorPhase;

    /**
     * The kP value of the motor's PID controller
     */
    private double kP;

    /**
     * The kI value of the motor's PID controller
     */
    private double kI;

    /**
     * The kD value of the motor's PID controller
     */
    private double kD;

    /**
     * The kF value of the motor's PID controller
     */
    private double kF;

    /**
     * The acceptable closed loop error in ticks
     */
    private int allowableClosedLoopError;

    /**
     * The type of status frame
     */
    private StatusFrameEnhanced statusFrameType;

    /**
     * The status frame of the motor
     */
    private int statusFrame;

    /**
     * Is a current limit enabled
     *
     * a currentLimit must be set if this is true
     */
    private boolean currentLimitEnabled;

    /**
     * The current limit set (amps)
     *
     * currentLimitEnabled must be set for this to activate
     */
    private int currentLimit;

    /**
     * The neutral mode of the motor controller
     */
    private NeutralMode neutralMode;

    /**
     * Should a smartDashboardPut be enabled
     *
     * true will put to SmartDashboard
     */
    private boolean smartDashboardPutEnabled;

    /**
     * The path that the motor controller should report to
     */
    private String smartDashboardPath;

    /**
     * The ramp rate when controlled in open loop
     */
    private double openLoopRampRate;

    /**
     * The ramp rate when controlled in closed loop
     */
    private double closedLoopRampRate;

    /**
     * The forward nominal output
     */
    private double nominalOutputForward;

    /**
     * The reverse nominal output
     */
    private double nominalOutputReverse;

    /**
     * The forward peak output
     */
    private double peakOutputForward;

    /**
     * The reverse peak output
     */
    private double peakOutputReverse;

    /**
     * The neutral deadband
     */
    private double neutralDeadband;

    /**
     * The saturation for voltage compensation
     */
    private double voltageCompensationSaturation;

    /**
     * The measurement period for velocity control
     */
    private SensorVelocityMeasPeriod velocityMeasurementPeriod;

    /**
     * The measurement window for the velocity control
     */
    private int velocityMeasurementWindow;

    /**
     * Is a forward soft limit enabled
     */
    private boolean forwardSoftLimitEnabled;

    /**
     * The forward soft limit set
     */
    private int forwardSoftLimitThreshold;

    /**
     * Is a reverse soft limit enabled
     */
    private boolean reverseSoftLimitEnabled;

    /**
     * The reverse soft limit set
     */
    private int reverseSoftLimitThreshold;

    /**
     * Is auxiliary polarity enabled
     */
    private boolean auxPIDPolarity;

    /**
     * The motion cruise velocity set
     */
    private int motionCruiseVelocity;

    /**
     * The motion acceleration set
     */
    private int motionAcceleration;

    /**
     * The strength of the motion curve
     */
    private int motionCurveStrength;

    /**
     * The period set when using motion profiles
     */
    private int motionProfileTrajectoryPeriod;

    /**
     * Is continuous or discontinuous feedback enabled
     */
    private boolean feedbackNotContinuous;

    public void updatePIDController() {
        motor.config_kP(0, this.getkP());
        motor.config_kI(0, this.getkI());
        motor.config_kD(0, this.getkD());
        motor.config_kF(1, this.getkF());

    }
	
	public int getRawAnalogSensor(){
        return m_sensorCollection.getAnalogInRaw();
    }

    public FRCTalonSRX configure() {
        motor = new WPI_TalonSRX(this.getCanID());
        m_sensorCollection = motor.getSensorCollection();

        motor.configFactoryDefault();
        motor.setSafetyEnabled(false);

        if (this.isInverted() || this.isInvertedWithType()) {
            if (this.useInvertType) motor.setInverted(this.invertType);
            else  motor.setInverted(this.isInverted());
            System.out.println("Configuring Inverted");
        }
        if (this.isCurrentLimitEnabled()) {
            motor.enableCurrentLimit(this.isCurrentLimitEnabled());
            motor.configContinuousCurrentLimit(this.getCurrentLimit());
            System.out.println("Configuring Current Limit");

        }
        if (this.isFeedbackNotContinuous()) {
            motor.configFeedbackNotContinuous(this.isFeedbackNotContinuous(), this.getTimeout());
            System.out.println("Configuring Feedback Continuity");
        }

        if (this.isForwardSoftLimitEnabled()) {
            motor.configForwardSoftLimitEnable(this.isForwardSoftLimitEnabled());
            motor.configForwardSoftLimitThreshold(this.getForwardSoftLimitThreshold());
            System.out.println("Configuring forward soft limit");
        }
        if (this.getMotionAcceleration() != 0) {
            motor.configMotionAcceleration(this.getMotionAcceleration());
            motor.configMotionCruiseVelocity(this.getMotionCruiseVelocity());
            System.out.println("Configuring acceleration");
        }
        if (this.getNeutralMode() != null) {
            motor.setNeutralMode(this.getNeutralMode());
            System.out.println("Setting Neutral Mode");
        }
        if (this.getNominalOutputForward() != 0 || this.getNominalOutputReverse() != 0) {
            motor.configNominalOutputForward(this.getNominalOutputForward());
            motor.configNominalOutputReverse(this.getNominalOutputReverse());
            System.out.println("Setting Nominal Output");
        }

        if (this.getOpenLoopRampRate() != 0) {
            motor.configOpenloopRamp(this.getOpenLoopRampRate());
            System.out.println("Setting Open Loop Ramp Rate");

        }

        if (this.getPeakOutputForward() != 0 || this.getPeakOutputReverse() != 0) {
            motor.configPeakOutputForward(this.getPeakOutputForward());
            motor.configPeakOutputReverse(this.getPeakOutputReverse());
            System.out.println("Setting Peak Output");
        }

        if (this.isReverseSoftLimitEnabled()) {
            motor.configReverseSoftLimitEnable(this.isReverseSoftLimitEnabled());
            motor.configReverseSoftLimitThreshold(this.getReverseSoftLimitThreshold());
            System.out.println("setting reverse soft limit enabled");
        }
        if (this.isSensorPhase()) {
            motor.setSensorPhase(this.isSensorPhase());
            System.out.println("setting sensor phase");

        }
        if (this.getStatusFrame() != 0) {
            motor.setStatusFramePeriod(this.getStatusFrameType(), this.getStatusFrame());
            System.out.println("Setting Frame Period");

        }
        if (this.getVelocityMeasurementPeriod() != null || this.getVelocityMeasurementWindow() != 0) {
            motor.configVelocityMeasurementPeriod(this.getVelocityMeasurementPeriod());
            motor.configVelocityMeasurementWindow(this.getVelocityMeasurementWindow());
            System.out.println("Setting Velocity Measurement Period");
        }
        if (this.getVoltageCompensationSaturation() != 0) {
            motor.configVoltageCompSaturation(this.getVoltageCompensationSaturation());
            System.out.println("Setting Saturation");

        }
        if (this.getkP() != 0 || this.getkI() != 0 || this.getkD() != 0 || this.getkF() != 0) {
            updatePIDController();
            System.out.println("Setting PID Controller");
        }
        if (this.master != null) {
            motor.follow(master.motor);
        }
        return this;
    }

    public WPI_TalonSRX getMotor() {
        return motor;
    }

    public void setMotor(WPI_TalonSRX motor) {
        this.motor = motor;
    }

    public int getCanID() {
        return canID;
    }

    public void setCanID(int canID) {
        this.canID = canID;
    }

    public boolean isInverted() {
        return inverted;
    }

    public boolean isInvertedWithType() {
        return invertType != InvertType.None;
    }

    public InvertType getInvertType() {
        return this.invertType;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
        this.invertType = InvertType.None;
        this.useInvertType = false;
    }

    public void setInverted(InvertType inverted) {
        this.invertType = inverted;
        this.inverted = false;
        this.useInvertType = true;
    }

    public int getFeedbackPort() {
        return feedbackPort;
    }

    public void setFeedbackPort(int feedbackPort) {
        this.feedbackPort = feedbackPort;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isSensorPhase() {
        return sensorPhase;
    }

    public void setSensorPhase(boolean sensorPhase) {
        this.sensorPhase = sensorPhase;
    }

    public double getkP() {
        return kP;
    }

    public void setkP(double kP) {
        this.kP = kP;
        this.updatePIDController();
    }

    public double getkI() {
        return kI;
    }

    public void setkI(double kI) {
        this.kI = kI;
        this.updatePIDController();
    }

    public double getkD() {
        return kD;
    }

    public void setkD(double kD) {
        this.kD = kD;
        this.updatePIDController();
    }

    public double getkF() {
        return kF;
    }

    public void setkF(double kF) {
        this.kF = kF;
        this.updatePIDController();
    }

    public int getAllowableClosedLoopError() {
        return allowableClosedLoopError;
    }

    public void setAllowableClosedLoopError(int allowableClosedLoopError) {
        this.allowableClosedLoopError = allowableClosedLoopError;
    }

    public StatusFrameEnhanced getStatusFrameType() {
        return statusFrameType;
    }

    public void setStatusFrameType(StatusFrameEnhanced statusFrameType) {
        this.statusFrameType = statusFrameType;
    }

    public int getStatusFrame() {
        return statusFrame;
    }

    public void setStatusFrame(int statusFrame) {
        this.statusFrame = statusFrame;
    }

    public boolean isCurrentLimitEnabled() {
        return currentLimitEnabled;
    }

    public void setCurrentLimitEnabled(boolean currentLimitEnabled) {
        this.currentLimitEnabled = currentLimitEnabled;
    }

    public int getCurrentLimit() {
        return currentLimit;
    }

    public void setCurrentLimit(int currentLimit) {
        this.currentLimit = currentLimit;
    }

    public NeutralMode getNeutralMode() {
        return neutralMode;
    }

    public void setNeutralMode(NeutralMode neutralMode) {
        this.neutralMode = neutralMode;
    }

    public boolean isSmartDashboardPutEnabled() {
        return smartDashboardPutEnabled;
    }

    public void setSmartDashboardPutEnabled(boolean smartDashboardPutEnabled) {
        this.smartDashboardPutEnabled = smartDashboardPutEnabled;
    }

    public String getSmartDashboardPath() {
        return smartDashboardPath;
    }

    public void setSmartDashboardPath(String smartDashboardPath) {
        this.smartDashboardPath = smartDashboardPath;
    }

    public double getOpenLoopRampRate() {
        return openLoopRampRate;
    }

    public void setOpenLoopRampRate(double openLoopRampRate) {
        this.openLoopRampRate = openLoopRampRate;
    }

    public double getClosedLoopRampRate() {
        return closedLoopRampRate;
    }

    public void setClosedLoopRampRate(double closedLoopRampRate) {
        this.closedLoopRampRate = closedLoopRampRate;
    }

    public double getNominalOutputForward() {
        return nominalOutputForward;
    }

    public void setNominalOutputForward(double nominalOutputForward) {
        this.nominalOutputForward = nominalOutputForward;
    }

    public double getNominalOutputReverse() {
        return nominalOutputReverse;
    }

    public void setNominalOutputReverse(double nominalOutputReverse) {
        this.nominalOutputReverse = nominalOutputReverse;
    }

    public double getPeakOutputForward() {
        return peakOutputForward;
    }

    public void setPeakOutputForward(double peakOutputForward) {
        this.peakOutputForward = peakOutputForward;
    }

    public double getPeakOutputReverse() {
        return peakOutputReverse;
    }

    public void setPeakOutputReverse(double peakOutputReverse) {
        this.peakOutputReverse = peakOutputReverse;
    }

    public double getNeutralDeadband() {
        return neutralDeadband;
    }

    public void setNeutralDeadband(double neutralDeadband) {
        this.neutralDeadband = neutralDeadband;
    }

    public double getVoltageCompensationSaturation() {
        return voltageCompensationSaturation;
    }

    public void setVoltageCompensationSaturation(double voltageCompensationSaturation) {
        this.voltageCompensationSaturation = voltageCompensationSaturation;
    }

    public SensorVelocityMeasPeriod getVelocityMeasurementPeriod() {
        return velocityMeasurementPeriod;
    }

    public void setVelocityMeasurementPeriod(SensorVelocityMeasPeriod velocityMeasurementPeriod) {
        this.velocityMeasurementPeriod = velocityMeasurementPeriod;
    }

    public int getVelocityMeasurementWindow() {
        return velocityMeasurementWindow;
    }

    public void setVelocityMeasurementWindow(int velocityMeasurementWindow) {
        this.velocityMeasurementWindow = velocityMeasurementWindow;
    }

    public boolean isForwardSoftLimitEnabled() {
        return forwardSoftLimitEnabled;
    }

    public void setForwardSoftLimitEnabled(boolean forwardSoftLimitEnabled) {
        this.forwardSoftLimitEnabled = forwardSoftLimitEnabled;
    }

    public int getForwardSoftLimitThreshold() {
        return forwardSoftLimitThreshold;
    }

    public void setForwardSoftLimitThreshold(int forwardSoftLimitThreshold) {
        this.forwardSoftLimitThreshold = forwardSoftLimitThreshold;
    }

    public boolean isReverseSoftLimitEnabled() {
        return reverseSoftLimitEnabled;
    }

    public void setReverseSoftLimitEnabled(boolean reverseSoftLimitEnabled) {
        this.reverseSoftLimitEnabled = reverseSoftLimitEnabled;
    }

    public int getReverseSoftLimitThreshold() {
        return reverseSoftLimitThreshold;
    }

    public void setReverseSoftLimitThreshold(int reverseSoftLimitThreshold) {
        this.reverseSoftLimitThreshold = reverseSoftLimitThreshold;
    }

    public boolean isAuxPIDPolarity() {
        return auxPIDPolarity;
    }

    public void setAuxPIDPolarity(boolean auxPIDPolarity) {
        this.auxPIDPolarity = auxPIDPolarity;
    }

    public int getMotionCruiseVelocity() {
        return motionCruiseVelocity;
    }

    public void setMotionCruiseVelocity(int motionCruiseVelocity) {
        this.motionCruiseVelocity = motionCruiseVelocity;
    }

    public int getMotionAcceleration() {
        return motionAcceleration;
    }

    public void setMotionAcceleration(int motionAcceleration) {
        this.motionAcceleration = motionAcceleration;
    }

    public int getMotionCurveStrength() {
        return motionCurveStrength;
    }

    public void setMotionCurveStrength(int motionCurveStrength) {
        this.motionCurveStrength = motionCurveStrength;
    }

    public int getMotionProfileTrajectoryPeriod() {
        return motionProfileTrajectoryPeriod;
    }

    public void setMotionProfileTrajectoryPeriod(int motionProfileTrajectoryPeriod) {
        this.motionProfileTrajectoryPeriod = motionProfileTrajectoryPeriod;
    }

    public boolean isFeedbackNotContinuous() {
        return feedbackNotContinuous;
    }

    public void setFeedbackNotContinuous(boolean feedbackNotContinuous) {
        this.feedbackNotContinuous = feedbackNotContinuous;
    }

    public void setMaster(FRCTalonSRX master) {
        this.master = master;
    }

    public static final class FRCTalonSRXBuilder {
        private int canID;
        private boolean inverted = false;
        private InvertType invertType = InvertType.None;
        private boolean useInvertType = false;
        private int feedbackPort = 0;
        private int timeout = 10;
        private boolean sensorPhase = false;
        private double kP = 0.0;
        private double kI = 0.0;
        private double kD = 0.0;
        private double kF = 0.0;
        private int allowableClosedLoopError = 0;
        private StatusFrameEnhanced statusFrameType = StatusFrameEnhanced.Status_3_Quadrature;
        private int statusFrame = 0;
        private boolean currentLimitEnabled = false;
        private int currentLimit = 0;
        private NeutralMode neutralMode = NeutralMode.Coast;
        private boolean smartDashboardPutEnabled = false;
        private String smartDashboardPath;
        private double openLoopRampRate = 0;
        private double closedLoopRampRate = 0;
        private double nominalOutputForward = 0;
        private double nominalOutputReverse = 0;
        private double peakOutputForward = 1.0;
        private double peakOutputReverse = -1.0;
        private double neutralDeadband = 0.04;
        private double voltageCompensationSaturation = 0;
        private SensorVelocityMeasPeriod velocityMeasurementPeriod = SensorVelocityMeasPeriod.Period_100Ms;// ??
        private int velocityMeasurementWindow = 64;
        private boolean forwardSoftLimitEnabled = false;
        private int forwardSoftLimitThreshold = 0;
        private boolean reverseSoftLimitEnabled = false;
        private int reverseSoftLimitThreshold = 0;
        private boolean auxPIDPolarity = false;
        private int motionCruiseVelocity = 0;
        private int motionAcceleration = 0;
        private int motionCurveStrength = 0;
        private int motionProfileTrajectoryPeriod = 0;
        private boolean feedbackNotContinuous = false;
        private FRCTalonSRX master;

        public FRCTalonSRXBuilder(int canID) {
            this.canID = canID;
            this.smartDashboardPath = "TalonSRX_" + canID;
        }

        public FRCTalonSRXBuilder withCanID(int canID) {
            this.canID = canID;
            return this;
        }

        public FRCTalonSRXBuilder withInverted(boolean inverted) {
            this.inverted = inverted;
            this.useInvertType = false;
            return this;
        }

        public FRCTalonSRXBuilder withInverted(InvertType inverted) {
            this.invertType = inverted;
            this.useInvertType = true;
            return this;
        }

        public FRCTalonSRXBuilder withFeedbackPort(int feedbackPort) {
            this.feedbackPort = feedbackPort;
            return this;
        }

        public FRCTalonSRXBuilder withTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public FRCTalonSRXBuilder withSensorPhase(boolean sensorPhase) {
            this.sensorPhase = sensorPhase;
            return this;
        }

        public FRCTalonSRXBuilder withKP(double kP) {
            this.kP = kP;
            return this;
        }

        public FRCTalonSRXBuilder withKI(double kI) {
            this.kI = kI;
            return this;
        }

        public FRCTalonSRXBuilder withKD(double kD) {
            this.kD = kD;
            return this;
        }

        public FRCTalonSRXBuilder withKF(double kF) {
            this.kF = kF;
            return this;
        }

        public FRCTalonSRXBuilder withAllowableClosedLoopError(int allowableClosedLoopError) {
            this.allowableClosedLoopError = allowableClosedLoopError;
            return this;
        }

        public FRCTalonSRXBuilder withStatusFrameType(StatusFrameEnhanced statusFrameType) {
            this.statusFrameType = statusFrameType;
            return this;
        }

        public FRCTalonSRXBuilder withStatusFrame(int statusFrame) {
            this.statusFrame = statusFrame;
            return this;
        }

        public FRCTalonSRXBuilder withCurrentLimitEnabled(boolean currentLimitEnabled) {
            this.currentLimitEnabled = currentLimitEnabled;
            return this;
        }

        public FRCTalonSRXBuilder withCurrentLimit(int currentLimit) {
            this.currentLimit = currentLimit;
            return this;
        }

        public FRCTalonSRXBuilder withNeutralMode(NeutralMode neutralMode) {
            this.neutralMode = neutralMode;
            return this;
        }

        public FRCTalonSRXBuilder withSmartDashboardPutEnabled(boolean smartDashboardPutEnabled) {
            this.smartDashboardPutEnabled = smartDashboardPutEnabled;
            return this;
        }

        public FRCTalonSRXBuilder withSmartDashboardPath(String smartDashboardPath) {
            this.smartDashboardPath = smartDashboardPath;
            return this;
        }

        public FRCTalonSRXBuilder withOpenLoopRampRate(double openLoopRampRate) {
            this.openLoopRampRate = openLoopRampRate;
            return this;
        }

        public FRCTalonSRXBuilder withClosedLoopRampRate(double closedLoopRampRate) {
            this.closedLoopRampRate = closedLoopRampRate;
            return this;
        }

        public FRCTalonSRXBuilder withNominalOutputForward(double nominalOutputForward) {
            this.nominalOutputForward = nominalOutputForward;
            return this;
        }

        public FRCTalonSRXBuilder withNominalOutputReverse(double nominalOutputReverse) {
            this.nominalOutputReverse = nominalOutputReverse;
            return this;
        }

        public FRCTalonSRXBuilder withPeakOutputForward(double peakOutputForward) {
            this.peakOutputForward = peakOutputForward;
            return this;
        }

        public FRCTalonSRXBuilder withPeakOutputReverse(double peakOutputReverse) {
            this.peakOutputReverse = peakOutputReverse;
            return this;
        }

        public FRCTalonSRXBuilder withNeutralDeadband(double neutralDeadband) {
            this.neutralDeadband = neutralDeadband;
            return this;
        }

        public FRCTalonSRXBuilder withVoltageCompensationSaturation(double voltageCompensationSaturation) {
            this.voltageCompensationSaturation = voltageCompensationSaturation;
            return this;
        }

        public FRCTalonSRXBuilder withVelocityMeasurementPeriod(SensorVelocityMeasPeriod velocityMeasurementPeriod) {
            this.velocityMeasurementPeriod = velocityMeasurementPeriod;
            return this;
        }

        public FRCTalonSRXBuilder withVelocityMeasurementWindow(int velocityMeasurementWindow) {
            this.velocityMeasurementWindow = velocityMeasurementWindow;
            return this;
        }

        public FRCTalonSRXBuilder withForwardSoftLimitEnabled(boolean forwardSoftLimitEnabled) {
            this.forwardSoftLimitEnabled = forwardSoftLimitEnabled;
            return this;
        }

        public FRCTalonSRXBuilder withForwardSoftLimitThreshold(int forwardSoftLimitThreshold) {
            this.forwardSoftLimitThreshold = forwardSoftLimitThreshold;
            return this;
        }

        public FRCTalonSRXBuilder withReverseSoftLimitEnabled(boolean reverseSoftLimitEnabled) {
            this.reverseSoftLimitEnabled = reverseSoftLimitEnabled;
            return this;
        }

        public FRCTalonSRXBuilder withReverseSoftLimitThreshold(int reverseSoftLimitThreshold) {
            this.reverseSoftLimitThreshold = reverseSoftLimitThreshold;
            return this;
        }

        public FRCTalonSRXBuilder withAuxPIDPolarity(boolean auxPIDPolarity) {
            this.auxPIDPolarity = auxPIDPolarity;
            return this;
        }

        public FRCTalonSRXBuilder withMotionCruiseVelocity(int motionCruiseVelocity) {
            this.motionCruiseVelocity = motionCruiseVelocity;
            return this;
        }

        public FRCTalonSRXBuilder withMotionAcceleration(int motionAcceleration) {
            this.motionAcceleration = motionAcceleration;
            return this;
        }

        public FRCTalonSRXBuilder withMotionCurveStrength(int motionCurveStrength) {
            this.motionCurveStrength = motionCurveStrength;
            return this;
        }

        public FRCTalonSRXBuilder withMotionProfileTrajectoryPeriod(int motionProfileTrajectoryPeriod) {
            this.motionProfileTrajectoryPeriod = motionProfileTrajectoryPeriod;
            return this;
        }

        public FRCTalonSRXBuilder withFeedbackNotContinuous(boolean feedbackNotContinuous) {
            this.feedbackNotContinuous = feedbackNotContinuous;
            return this;
        }

        public FRCTalonSRXBuilder withMaster(FRCTalonSRX master) {
            this.master = master;
            return this;
        }

        public FRCTalonSRX build() {
            FRCTalonSRX fRCTalonSRX = new FRCTalonSRX();
            fRCTalonSRX.setCanID(canID);
            if (useInvertType) fRCTalonSRX.setInverted(invertType);
            else fRCTalonSRX.setInverted(inverted);
            fRCTalonSRX.setFeedbackPort(feedbackPort);
            fRCTalonSRX.setTimeout(timeout);
            fRCTalonSRX.setSensorPhase(sensorPhase);
            fRCTalonSRX.setAllowableClosedLoopError(allowableClosedLoopError);
            fRCTalonSRX.setStatusFrameType(statusFrameType);
            fRCTalonSRX.setStatusFrame(statusFrame);
            fRCTalonSRX.setCurrentLimitEnabled(currentLimitEnabled);
            fRCTalonSRX.setCurrentLimit(currentLimit);
            fRCTalonSRX.setNeutralMode(neutralMode);
            fRCTalonSRX.setSmartDashboardPutEnabled(smartDashboardPutEnabled);
            fRCTalonSRX.setSmartDashboardPath(smartDashboardPath);
            fRCTalonSRX.setOpenLoopRampRate(openLoopRampRate);
            fRCTalonSRX.setClosedLoopRampRate(closedLoopRampRate);
            fRCTalonSRX.setNominalOutputForward(nominalOutputForward);
            fRCTalonSRX.setNominalOutputReverse(nominalOutputReverse);
            fRCTalonSRX.setPeakOutputForward(peakOutputForward);
            fRCTalonSRX.setPeakOutputReverse(peakOutputReverse);
            fRCTalonSRX.setNeutralDeadband(neutralDeadband);
            fRCTalonSRX.setVoltageCompensationSaturation(voltageCompensationSaturation);
            fRCTalonSRX.setVelocityMeasurementPeriod(velocityMeasurementPeriod);
            fRCTalonSRX.setVelocityMeasurementWindow(velocityMeasurementWindow);
            fRCTalonSRX.setForwardSoftLimitEnabled(forwardSoftLimitEnabled);
            fRCTalonSRX.setForwardSoftLimitThreshold(forwardSoftLimitThreshold);
            fRCTalonSRX.setReverseSoftLimitEnabled(reverseSoftLimitEnabled);
            fRCTalonSRX.setReverseSoftLimitThreshold(reverseSoftLimitThreshold);
            fRCTalonSRX.setAuxPIDPolarity(auxPIDPolarity);
            fRCTalonSRX.setMotionCruiseVelocity(motionCruiseVelocity);
            fRCTalonSRX.setMotionAcceleration(motionAcceleration);
            fRCTalonSRX.setMotionCurveStrength(motionCurveStrength);
            fRCTalonSRX.setMotionProfileTrajectoryPeriod(motionProfileTrajectoryPeriod);
            fRCTalonSRX.setFeedbackNotContinuous(feedbackNotContinuous);
            fRCTalonSRX.setMaster(master);
            fRCTalonSRX.kF = this.kF;
            fRCTalonSRX.kD = this.kD;
            fRCTalonSRX.kI = this.kI;
            fRCTalonSRX.kP = this.kP;
            return fRCTalonSRX.configure();
        }
    }
}