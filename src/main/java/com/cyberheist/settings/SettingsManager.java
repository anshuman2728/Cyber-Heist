package com.cyberheist.settings;

/**
 * Stores and manages customizable audio, visual, and control settings.
 */
public final class SettingsManager {

    private static final SettingsManager INSTANCE = new SettingsManager();

    private double masterVolume = 0.80;
    private double sfxVolume = 0.90;
    private double ambientVolume = 0.50;

    private boolean screenShakeEnabled = true;
    private double screenShakeIntensity = 1.0;
    private boolean scanlinesEnabled = true;
    private boolean cameraLeadEnabled = true;
    private double cameraSmoothing = 7.0; // Responsive lerp

    private SettingsManager() {
    }

    public static SettingsManager getInstance() {
        return INSTANCE;
    }

    public double getMasterVolume() { return masterVolume; }
    public void setMasterVolume(double masterVolume) {
        this.masterVolume = Math.max(0.0, Math.min(1.0, masterVolume));
    }

    public double getSfxVolume() { return sfxVolume; }
    public void setSfxVolume(double sfxVolume) {
        this.sfxVolume = Math.max(0.0, Math.min(1.0, sfxVolume));
    }

    public double getAmbientVolume() { return ambientVolume; }
    public void setAmbientVolume(double ambientVolume) {
        this.ambientVolume = Math.max(0.0, Math.min(1.0, ambientVolume));
    }

    public boolean isScreenShakeEnabled() { return screenShakeEnabled; }
    public void setScreenShakeEnabled(boolean screenShakeEnabled) {
        this.screenShakeEnabled = screenShakeEnabled;
    }

    public double getScreenShakeIntensity() { return screenShakeIntensity; }
    public void setScreenShakeIntensity(double intensity) {
        this.screenShakeIntensity = Math.max(0.0, Math.min(2.0, intensity));
    }

    public boolean isScanlinesEnabled() { return scanlinesEnabled; }
    public void setScanlinesEnabled(boolean scanlinesEnabled) {
        this.scanlinesEnabled = scanlinesEnabled;
    }

    public boolean isCameraLeadEnabled() { return cameraLeadEnabled; }
    public void setCameraLeadEnabled(boolean cameraLeadEnabled) {
        this.cameraLeadEnabled = cameraLeadEnabled;
    }

    public double getCameraSmoothing() { return cameraSmoothing; }
    public void setCameraSmoothing(double cameraSmoothing) {
        this.cameraSmoothing = Math.max(2.0, Math.min(15.0, cameraSmoothing));
    }
}
