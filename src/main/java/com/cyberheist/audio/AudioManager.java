package com.cyberheist.audio;

import com.cyberheist.settings.SettingsManager;
import com.cyberheist.util.Logger;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Procedural Audio Synthesizer and sound manager for Cyber Heist.
 * Generates crisp, low-latency PCM sound effects and ambient HVAC drones
 * entirely via mathematics and Java Sound, requiring zero external audio assets.
 * Completely headless-safe and non-blocking.
 */
public final class AudioManager {

    private static final Logger LOGGER = Logger.getLogger(AudioManager.class);
    private static final AudioManager INSTANCE = new AudioManager();

    private static final float SAMPLE_RATE = 22050.0f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final Map<SoundEffect, byte[]> soundCache = new EnumMap<>(SoundEffect.class);
    private final ExecutorService audioExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "CyberHeist-AudioThread");
        t.setDaemon(true);
        return t;
    });

    private boolean audioAvailable = true;
    private volatile boolean ambientRunning = false;
    private Thread ambientThread = null;
    private final Random random = new Random();

    private AudioManager() {
        try {
            pregenerateSounds();
        } catch (Exception e) {
            LOGGER.info("Audio system running in fallback/silent mode: " + e.getMessage());
            audioAvailable = false;
        }
    }

    public static AudioManager getInstance() {
        return INSTANCE;
    }

    private void pregenerateSounds() {
        soundCache.put(SoundEffect.FOOTSTEP_WALK, synthesizeTone(0.045, 90.0, 45.0, 0.45, WaveType.TRIANGLE_BURST));
        soundCache.put(SoundEffect.FOOTSTEP_RUN, synthesizeTone(0.055, 125.0, 50.0, 0.70, WaveType.TRIANGLE_BURST));
        soundCache.put(SoundEffect.STUN_ZAP, synthesizeTone(0.14, 850.0, 220.0, 0.75, WaveType.ELECTRIC_NOISE));
        soundCache.put(SoundEffect.EMP_BLAST, synthesizeTone(0.32, 120.0, 32.0, 0.95, WaveType.SUB_RUMBLE));
        soundCache.put(SoundEffect.DISTRACTION_PING, synthesizeTone(0.12, 880.0, 880.0, 0.80, WaveType.BELL_SINE));
        soundCache.put(SoundEffect.TAKEDOWN_THUD, synthesizeTone(0.16, 80.0, 40.0, 0.85, WaveType.MUFFLED_PUNCH));
        soundCache.put(SoundEffect.HACK_CLICK, synthesizeTone(0.025, 1400.0, 1400.0, 0.50, WaveType.SINE));
        soundCache.put(SoundEffect.HACK_ERROR, synthesizeTone(0.18, 140.0, 110.0, 0.85, WaveType.BUZZ_SQUARE));
        soundCache.put(SoundEffect.HACK_SUCCESS, synthesizeArpeggio());
        soundCache.put(SoundEffect.DOOR_SLIDE, synthesizeTone(0.26, 320.0, 160.0, 0.60, WaveType.SERVO_WHOOSH));
        soundCache.put(SoundEffect.RADIO_CHIRP, synthesizeTone(0.035, 2100.0, 2100.0, 0.65, WaveType.SINE));
        soundCache.put(SoundEffect.ALARM_ALERT, synthesizeTone(0.20, 800.0, 600.0, 0.80, WaveType.BUZZ_SQUARE));
    }

    public void play(SoundEffect effect) {
        if (!audioAvailable || effect == null) return;
        byte[] pcmData = soundCache.get(effect);
        if (pcmData == null) return;

        double volume = SettingsManager.getInstance().getMasterVolume() * SettingsManager.getInstance().getSfxVolume();
        if (volume <= 0.001) return;

        audioExecutor.submit(() -> playPcmBuffer(pcmData, volume));
    }

    public void playFootstep(boolean running) {
        play(running ? SoundEffect.FOOTSTEP_RUN : SoundEffect.FOOTSTEP_WALK);
    }

    private void playPcmBuffer(byte[] pcmData, double volume) {
        try {
            byte[] adjustedData = applyVolumeScaling(pcmData, volume);
            AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(adjustedData), FORMAT, adjustedData.length / 2);

            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
            // Automatically close clip line when playback ends
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
        } catch (Exception ignored) {
            // Hardware line unavailable or running in headless testing environment
        }
    }

    private byte[] applyVolumeScaling(byte[] pcm, double volume) {
        byte[] scaled = new byte[pcm.length];
        for (int i = 0; i < pcm.length - 1; i += 2) {
            short sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            sample = (short) Math.round(sample * volume);
            scaled[i] = (byte) (sample & 0xFF);
            scaled[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return scaled;
    }

    public synchronized void startAmbientHum() {
        if (!audioAvailable || ambientRunning) return;
        ambientRunning = true;

        ambientThread = new Thread(() -> {
            try {
                SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT);
                line.open(FORMAT, 4096);
                line.start();

                byte[] buffer = new byte[1024];
                double phase60 = 0.0;
                double phase120 = 0.0;

                while (ambientRunning) {
                    double vol = SettingsManager.getInstance().getMasterVolume() * SettingsManager.getInstance().getAmbientVolume() * 0.18;
                    for (int i = 0; i < buffer.length - 1; i += 2) {
                        phase60 += (2.0 * Math.PI * 58.0) / SAMPLE_RATE;
                        phase120 += (2.0 * Math.PI * 116.0) / SAMPLE_RATE;

                        double hum = (Math.sin(phase60) * 0.7) + (Math.sin(phase120) * 0.3);
                        double pinkNoise = (random.nextDouble() * 2.0 - 1.0) * 0.15;
                        double total = (hum + pinkNoise) * vol;

                        short sample = (short) Math.round(total * 32767.0);
                        buffer[i] = (byte) (sample & 0xFF);
                        buffer[i + 1] = (byte) ((sample >> 8) & 0xFF);
                    }
                    line.write(buffer, 0, buffer.length);
                }
                line.drain();
                line.close();
            } catch (Exception ignored) {
                ambientRunning = false;
            }
        }, "CyberHeist-AmbientHum");
        ambientThread.setDaemon(true);
        ambientThread.start();
    }

    public synchronized void stopAmbientHum() {
        ambientRunning = false;
        if (ambientThread != null) {
            ambientThread.interrupt();
            ambientThread = null;
        }
    }

    public synchronized void startAmbient() {
        startAmbientHum();
    }

    public synchronized void stopAmbient() {
        stopAmbientHum();
    }

    private enum WaveType {
        SINE,
        TRIANGLE_BURST,
        ELECTRIC_NOISE,
        SUB_RUMBLE,
        BELL_SINE,
        MUFFLED_PUNCH,
        BUZZ_SQUARE,
        SERVO_WHOOSH
    }

    private byte[] synthesizeTone(double duration, double startFreq, double endFreq, double peakGain, WaveType type) {
        int totalSamples = (int) Math.round(duration * SAMPLE_RATE);
        byte[] buffer = new byte[totalSamples * 2];
        double phase = 0.0;

        for (int i = 0; i < totalSamples; i++) {
            double progress = (double) i / totalSamples;
            double currentFreq = startFreq + (endFreq - startFreq) * progress;
            phase += (2.0 * Math.PI * currentFreq) / SAMPLE_RATE;

            // Attack - Decay envelope
            double env = 1.0 - progress;
            if (progress < 0.15) {
                env = progress / 0.15;
            }

            double sampleVal = switch (type) {
                case SINE -> Math.sin(phase);
                case TRIANGLE_BURST -> Math.asin(Math.sin(phase)) * (2.0 / Math.PI) + (random.nextDouble() * 0.2 - 0.1);
                case ELECTRIC_NOISE -> (Math.sin(phase) > 0 ? 0.7 : -0.7) + (random.nextDouble() * 0.8 - 0.4);
                case SUB_RUMBLE -> Math.sin(phase) * Math.sin(phase * 0.5);
                case BELL_SINE -> Math.sin(phase) + (Math.sin(phase * 2.0) * 0.4);
                case MUFFLED_PUNCH -> Math.sin(phase) * (1.0 - progress * progress);
                case BUZZ_SQUARE -> (Math.sin(phase) > 0 ? 0.8 : -0.8);
                case SERVO_WHOOSH -> (random.nextDouble() * 2.0 - 1.0) * Math.sin(phase * 0.25);
            };

            short pcm = (short) Math.round(sampleVal * env * peakGain * 32767.0);
            buffer[i * 2] = (byte) (pcm & 0xFF);
            buffer[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
        }
        return buffer;
    }

    private byte[] synthesizeArpeggio() {
        double[] notes = { 523.25, 659.25, 783.99 }; // C5, E5, G5
        double noteDuration = 0.08;
        int totalSamples = (int) Math.round(notes.length * noteDuration * SAMPLE_RATE);
        byte[] buffer = new byte[totalSamples * 2];
        int sampleIndex = 0;

        for (double freq : notes) {
            int noteSamples = (int) Math.round(noteDuration * SAMPLE_RATE);
            double phase = 0.0;
            for (int i = 0; i < noteSamples; i++) {
                phase += (2.0 * Math.PI * freq) / SAMPLE_RATE;
                double env = 1.0 - ((double) i / noteSamples);
                double val = Math.sin(phase) * env * 0.75;
                short pcm = (short) Math.round(val * 32767.0);
                buffer[sampleIndex * 2] = (byte) (pcm & 0xFF);
                buffer[sampleIndex * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
                sampleIndex++;
            }
        }
        return buffer;
    }

    public boolean isAudioAvailable() { return audioAvailable; }
    public boolean isAmbientRunning() { return ambientRunning; }
}
