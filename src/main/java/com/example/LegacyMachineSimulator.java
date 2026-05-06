package com.example;

public class LegacyMachineSimulator {

    public enum MachineState {
        IDLE,
        RUNNING,
        STOPPED,
        ERROR
    }
    private MachineState currentState = MachineState.IDLE;
    private boolean running = false;
    private int errorCode =0;
    private double targetSpeed = 100.0;

    public MachineState getCurrentState(){
        return currentState;
    }
    public boolean isRunning() {
        return running;
    }
    public int getErrorCode() {
        return errorCode;
    }
    public double getTargetSpeed() {
        return targetSpeed;
    }
    public void setTargetSpeed(double targetSpeed) {
        this.targetSpeed = targetSpeed;
    }
    public void start() {
       if(currentState == MachineState.IDLE || currentState == MachineState.STOPPED) {
           currentState = MachineState.RUNNING;
           running = true;
           errorCode =0;
       }
    }
    public void stop() {
        currentState = MachineState.STOPPED;
        running = false;
    }
    public void reset() {
        currentState = MachineState.IDLE;
        running = false;
        errorCode = 0;
    }

    public void triggerError(int code) {
        currentState = MachineState.ERROR;
        running = false;
        errorCode = code;

    }
}
