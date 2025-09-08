package com.omnia.raspberry.model;

public class GpioPin {
    private int pinNumber;
    private String name;
    private String type;
    private boolean state;
    private int pwmValue; // 0-255 per PWM
    private String description;
    private boolean enabled;
    private int brightness; // 0-255 per LED brightness
    private String effect; // per LED Strip effects (static, breathe, rainbow, strobe)

    public GpioPin(int pinNumber, String name, String type, String description) {
        this.pinNumber = pinNumber;
        this.name = name;
        this.type = type;
        this.description = description;
        this.state = false;
        this.pwmValue = 0;
        this.enabled = true;
        this.brightness = 255;
        this.effect = "static";
    }

    // Getters and Setters
    public int getPinNumber() { return pinNumber; }
    public void setPinNumber(int pinNumber) { this.pinNumber = pinNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isState() { return state; }
    public void setState(boolean state) { this.state = state; }

    public int getPwmValue() { return pwmValue; }
    public void setPwmValue(int pwmValue) { this.pwmValue = pwmValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getBrightness() { return brightness; }
    public void setBrightness(int brightness) { this.brightness = brightness; }

    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }
}