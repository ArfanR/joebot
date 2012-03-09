/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stuy.subsystems;

import edu.stuy.RobotMap;
import edu.stuy.commands.Autonomous;
import edu.stuy.commands.DriveManualJoystickControl;
import edu.stuy.util.VictorRobotDrive;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SendablePIDController;

/**
 *
 * @author Kevin Wang
 */
public class Drivetrain extends Subsystem {
    public RobotDrive drive;
    
    Solenoid gearShiftLow;
    Solenoid gearShiftHigh;

    AnalogChannel vcc;
    public Encoder encoderLeft;
    public Encoder encoderRight;
    Gyro gyro;
    SendablePIDController controller;
    private Relay underbodyLights;
    final int WHEEL_RADIUS = 3;
    final double CIRCUMFERENCE = 2 * Math.PI * WHEEL_RADIUS;
    final int ENCODER_CODES_PER_REV = 360;
    final double DISTANCE_PER_PULSE = CIRCUMFERENCE / ENCODER_CODES_PER_REV;
    double Kp = 0.035;
    double Ki = 0.0005;
    double Kd = 1.0;
    private double previousReading = -1.0;
    public Compressor compressor;

    // Put methods for controlling this subsystem
    // here. Call these from Commands.
    public Drivetrain() {
        drive = new VictorRobotDrive(RobotMap.FRONT_LEFT_MOTOR, RobotMap.REAR_LEFT_MOTOR, RobotMap.FRONT_RIGHT_MOTOR, RobotMap.REAR_RIGHT_MOTOR);
        drive.setSafetyEnabled(false);

        encoderLeft = new Encoder(RobotMap.LEFT_ENCODER_CHANNEL_A, RobotMap.LEFT_ENCODER_CHANNEL_B, true);
        encoderRight = new Encoder(RobotMap.RIGHT_ENCODER_CHANNEL_A, RobotMap.RIGHT_ENCODER_CHANNEL_B, true);

        encoderLeft.setDistancePerPulse(DISTANCE_PER_PULSE);
        encoderRight.setDistancePerPulse(DISTANCE_PER_PULSE);

        encoderLeft.start();
        encoderRight.start();


        gyro = new Gyro(RobotMap.GYRO_CHANNEL);
        gyro.setSensitivity(0.007);

        controller = new SendablePIDController(Kp, Ki, Kd, gyro, new PIDOutput() {

            public void pidWrite(double output) {
                drive.arcadeDrive(SpeedRamp.profileSpeed_Bravo(Autonomous.INCHES_TO_FENDER - getAvgDistance(), Autonomous.INCHES_TO_FENDER, 1), -output);
            }
        }, 0.005);

       gearShiftLow = new Solenoid(2, RobotMap.GEAR_SHIFT_LOW);
       gearShiftHigh = new Solenoid(2, RobotMap.GEAR_SHIFT_HIGH);
       vcc = new AnalogChannel(RobotMap.VCC_CHANNEL);
       
       underbodyLights = new Relay(RobotMap.UNDERBODY_LIGHTS);
       
       
        compressor = new Compressor(RobotMap.PRESSURE_SWITCH_CHANNEL, RobotMap.COMPRESSOR_RELAY_CHANNEL);
        compressor.start();
    }



    /**
     * Get value of maximum analog input in volts.
     * @return value of maximum analog input in volts.
     */
    public double getVcc() {
        return vcc.getVoltage();
    }



    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        //setDefaultCommand(new MySpecialCommand());
        setDefaultCommand(new DriveManualJoystickControl());
    }

    public Command getDefaultCommand() {
        return super.getDefaultCommand();
    }

    public void tankDrive(double leftValue, double rightValue) {
        drive.tankDrive(leftValue, rightValue);
    }

    public void setGear(boolean high) {
        gearShiftHigh.set(high);
        gearShiftLow.set(!high);
    }

    public boolean getGear() {
        return gearShiftHigh.get();
    }

    public void initController() {
        resetEncoders();
        controller.setSetpoint(0);
        controller.enable();
    }

    public void endController() {
        controller.disable();
    }
    
    /**
     * Sets the ramping distance and direction by constructing a new PID controller.
     * @param distance inches to travel
     */
    public void setDriveStraightDistanceAndDirection(final double distance, final int direction) {
        if (controller != null) {
            controller.disable();
            controller.free();
        }
        controller = new SendablePIDController(Kp, Ki, Kd, gyro, new PIDOutput() {

            public void pidWrite(double output) {
                drive.arcadeDrive(SpeedRamp.profileSpeed_Bravo(distance - getAvgDistance(), distance, direction), -output);
            }
        }, 0.005);
    }

    public void driveStraight() {
        controller.setSetpoint(0);  // Go straight
    }

    /**
     * Calculate average distance of the two encoders.  
     * @return Average of the distances (inches) read by each encoder since they were last reset.
     */
    public double getAvgDistance() {
        return (getLeftEncoderDistance() + getRightEncoderDistance()) / 2.0;
    }
    
    public double getLeftEncoderDistance() {
        return encoderLeft.getDistance();
    }
    
    public double getRightEncoderDistance() {
        return encoderRight.getDistance();
    }

    /**
     * Reset both encoders's tick, distance, etc. count to zero
     */
    public void resetEncoders() {
        encoderLeft.reset();
        encoderRight.reset();
    }
    
    public double getGyroAngle() {
        return gyro.getAngle();
    }
    
    public void setUnderbodyLights(boolean on) {
        underbodyLights.set(on ? Relay.Value.kOn : Relay.Value.kOff);
    }
    
    public static class SpeedRamp {
        /**
         * Profiles based on generic distance measurement to wall and the distance to travel.
         * Speed/Distance follows the following profile:
         * |
         * |
         * |     _________
         * |    /         \ 
         * |   /           \
         * |__/             \__
         * |               
         * ------------------------------
         * 
         * NOTE: Possible issues include negative differences in case a sensor measures a greater
         *       distance than actually exists.
         * 
         * @param distToFinish Distance from the robot to the Fender
         * @param totalDistToTravel Total distance for the robot to travel
         * @param direction 1 for forward, -1 for backward
         * @return The speed at which to drive the motors, from -1.0 to 1.0
         */
        public static double profileSpeed_Bravo(double distToFinish, double totalDistToTravel, int direction) {
            double outputSpeed = 0;
            double thirdOfDistToTravel = totalDistToTravel / 3.0;
            double difference = totalDistToTravel - distToFinish;
            double stage = Math.abs(difference / totalDistToTravel);

            // If we are in the first third of travel, ramp up speed proportionally to distance from first third
            if (stage < 1.0/3.0) {
                outputSpeed = 0.5 + (1-0.5)/(1.0/3.0) * stage; // Scales from 0.5->1, approaching 1 as the distance traveled
                                                                          //approaches the first third
            }
            else if (stage < 2.0/3.0) {
                outputSpeed = 1.0;
            }
            else if (stage < 1) {
                outputSpeed = distToFinish / (thirdOfDistToTravel); // Scales from 1->0 during the final third of distance travel.
            }
            if (outputSpeed < 0.3) {
                outputSpeed = 0.3;
            }

            return outputSpeed * direction;
        }
    }
}
