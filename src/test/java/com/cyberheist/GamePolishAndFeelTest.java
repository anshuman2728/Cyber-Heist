package com.cyberheist;

import com.cyberheist.audio.AudioManager;
import com.cyberheist.audio.SoundEffect;
import com.cyberheist.display.Camera2D;
import com.cyberheist.entity.Player;
import com.cyberheist.entity.PlayerState;
import com.cyberheist.particle.Particle;
import com.cyberheist.particle.ParticleSystem;
import com.cyberheist.settings.SettingsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class GamePolishAndFeelTest {

    private SettingsManager settings;

    @BeforeEach
    void setUp() {
        settings = SettingsManager.getInstance();
        settings.setMasterVolume(0.8);
        settings.setSfxVolume(0.9);
        settings.setAmbientVolume(0.5);
        settings.setScreenShakeEnabled(true);
        settings.setScreenShakeIntensity(1.0);
        settings.setScanlinesEnabled(true);
        settings.setCameraSmoothing(7.0);
    }

    @Test
    void testSettingsManagerClampingAndToggles() {
        // Volume clamps between 0.0 and 1.0
        settings.setMasterVolume(1.5);
        assertEquals(1.0, settings.getMasterVolume(), 0.001);

        settings.setMasterVolume(-0.2);
        assertEquals(0.0, settings.getMasterVolume(), 0.001);

        settings.setSfxVolume(0.75);
        assertEquals(0.75, settings.getSfxVolume(), 0.001);

        // Screen shake
        settings.setScreenShakeEnabled(false);
        assertFalse(settings.isScreenShakeEnabled());
        settings.setScreenShakeEnabled(true);
        assertTrue(settings.isScreenShakeEnabled());

        settings.setScreenShakeIntensity(2.5);
        assertEquals(2.0, settings.getScreenShakeIntensity(), 0.001);

        // Scanlines
        settings.setScanlinesEnabled(false);
        assertFalse(settings.isScanlinesEnabled());

        // Camera smoothing clamp
        settings.setCameraSmoothing(1.0);
        assertEquals(2.0, settings.getCameraSmoothing(), 0.001);
        settings.setCameraSmoothing(20.0);
        assertEquals(15.0, settings.getCameraSmoothing(), 0.001);
    }

    @Test
    void testCameraTraumaAndScreenShakeDecay() {
        Camera2D camera = new Camera2D(800, 600, 2000, 2000);
        assertEquals(0.0, camera.getTrauma(), 0.001);

        // Add trauma
        camera.addTrauma(0.5);
        assertEquals(0.5, camera.getTrauma(), 0.001);

        // Trauma clamps to 1.0
        camera.addTrauma(0.8);
        assertEquals(1.0, camera.getTrauma(), 0.001);

        // Update camera: shake offsets should be applied and trauma should decay
        camera.update(500, 500, 0.1, 7.0);
        assertTrue(camera.getTrauma() < 1.0, "Trauma should decay over elapsed delta time");

        // When screen shake is disabled, shake offsets must be 0
        settings.setScreenShakeEnabled(false);
        camera.update(500, 500, 0.01, 7.0);
        assertEquals(0.0, camera.getShakeOffsetX(), 0.001);
        assertEquals(0.0, camera.getShakeOffsetY(), 0.001);
    }

    @Test
    void testParticlePhysicsAndPoolManagement() {
        ParticleSystem system = new ParticleSystem();
        assertEquals(0, system.getParticleCount());

        // Individual particle lifetime & physics
        Particle p = new Particle(100, 100, 50, 0, 0.5, 4.0, Color.CYAN, true, null);
        assertTrue(p.isAlive());
        p.update(0.2);
        assertTrue(p.getX() > 100.0, "Particle should travel forward along velocity vector");
        assertTrue(p.isAlive());

        p.update(0.4);
        assertFalse(p.isAlive(), "Particle should expire once lifetime has elapsed");

        // ParticleSystem spawn emitters
        system.spawnFootstepDust(100, 100, 1.0, 0.0, false);
        assertTrue(system.getParticleCount() >= 2);

        system.spawnStunSparks(150, 150, 10);
        assertTrue(system.getParticleCount() >= 12);

        system.spawnEmpPulse(200, 200, 15);
        assertTrue(system.getParticleCount() >= 27);

        system.spawnDataBits(250, 250, 5);
        assertTrue(system.getParticleCount() >= 32);

        system.spawnTakedownPuff(300, 300);
        assertTrue(system.getParticleCount() >= 40);

        // Updating with large delta time purges expired particles
        system.update(2.0);
        assertEquals(0, system.getParticleCount(), "All short-lived particles should have expired");

        // Clear resets system
        system.spawnFootstepDust(50, 50, 0, 1, true);
        assertTrue(system.getParticleCount() > 0);
        system.clear();
        assertEquals(0, system.getParticleCount());
    }

    @Test
    void testAudioManagerProceduralSynthesisAndSafety() {
        AudioManager audio = AudioManager.getInstance();
        assertNotNull(audio);

        // Playing procedural sound effects must be non-blocking and safe in headless mode
        assertDoesNotThrow(() -> {
            audio.play(SoundEffect.FOOTSTEP_WALK);
            audio.play(SoundEffect.FOOTSTEP_RUN);
            audio.play(SoundEffect.STUN_ZAP);
            audio.play(SoundEffect.EMP_BLAST);
            audio.play(SoundEffect.DISTRACTION_PING);
            audio.play(SoundEffect.TAKEDOWN_THUD);
            audio.play(SoundEffect.HACK_CLICK);
            audio.play(SoundEffect.HACK_SUCCESS);
            audio.play(SoundEffect.DOOR_SLIDE);
            audio.play(SoundEffect.RADIO_CHIRP);
        });

        // Ambient sound starting and stopping
        assertDoesNotThrow(audio::startAmbient);
        assertDoesNotThrow(audio::stopAmbient);
    }

    @Test
    void testHumanDesignerPlayerMovementTuning() {
        Player player = new Player(200, 200);

        // Calibrated human game designer values
        assertEquals(205.0, player.getWalkSpeed(), 0.01, "Walk speed should be tuned to deliberate stealth pace");
        assertEquals(365.0, player.getSprintSpeed(), 0.01, "Sprint speed should be tuned to responsive burst dash");
        assertEquals(1350.0, player.getAccelerationRate(), 0.01, "Acceleration should offer smooth ramp curve");
        assertEquals(1950.0, player.getDecelerationRate(), 0.01, "Deceleration should provide crisp stopping friction");

        // Footstep dust listener registration
        AtomicBoolean dustTriggered = new AtomicBoolean(false);
        player.setFootstepDustListener((x, y, dx, dy, running) -> dustTriggered.set(true));

        // Stride cadence verification (simulate walking steps)
        com.cyberheist.input.KeyboardInput kb = new com.cyberheist.input.KeyboardInput();
        java.awt.Component dummy = new java.awt.Component() {};
        kb.keyPressed(new java.awt.event.KeyEvent(dummy, java.awt.event.KeyEvent.KEY_PRESSED, 0, 0, java.awt.event.KeyEvent.VK_D, 'D'));
        kb.poll();

        for (int i = 0; i < 15; i++) {
            player.update(0.04, kb, null, java.util.List.of());
        }

        assertTrue(dustTriggered.get(), "Footstep cadence must trigger dust listener for particle effects");
    }
}
