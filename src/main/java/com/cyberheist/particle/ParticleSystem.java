package com.cyberheist.particle;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Lightweight, high-performance 2D Particle System for tactical cyberpunk effects:
 * - Footstep dust trails
 * - High-voltage stun sparks
 * - EMP shockwaves
 * - Data matrix bits
 * - Takedown impact dust
 */
public final class ParticleSystem {

    private static final int MAX_PARTICLES = 500;
    private final List<Particle> particles = new ArrayList<>(MAX_PARTICLES);
    private final Random random = new Random();

    public void update(double deltaTime) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(deltaTime);
            if (!p.isAlive()) {
                it.remove();
            }
        }
    }

    public void render(Graphics2D g2d) {
        for (Particle p : particles) {
            p.render(g2d);
        }
    }

    public void addParticle(Particle particle) {
        if (particles.size() < MAX_PARTICLES) {
            particles.add(particle);
        }
    }

    /**
     * Spawns subtle ground dust puffs behind player's movement direction.
     */
    public void spawnFootstepDust(double x, double y, double dirX, double dirY, boolean running) {
        int count = running ? 4 : 2;
        double baseSpeed = running ? 25.0 : 12.0;

        for (int i = 0; i < count; i++) {
            // Slight reverse velocity + lateral scatter
            double vx = -dirX * baseSpeed + (random.nextDouble() - 0.5) * 18.0;
            double vy = -dirY * baseSpeed + (random.nextDouble() - 0.5) * 18.0;
            double life = 0.25 + random.nextDouble() * 0.20;
            double size = running ? (3.5 + random.nextDouble() * 2.0) : (2.5 + random.nextDouble() * 1.5);

            Color color = new Color(140, 160, 175, 120);
            addParticle(new Particle(x, y, vx, vy, life, size, color, true, null));
        }
    }

    /**
     * High-voltage electric arc sparks (cyan and electric yellow).
     */
    public void spawnStunSparks(double x, double y, int count) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 80.0 + random.nextDouble() * 140.0;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            double life = 0.15 + random.nextDouble() * 0.25;
            double size = 2.0 + random.nextDouble() * 3.0;

            Color sparkColor = (random.nextBoolean())
                    ? new Color(0, 240, 255)
                    : new Color(255, 235, 100);

            addParticle(new Particle(x, y, vx, vy, life, size, sparkColor, true, null));
        }
    }

    /**
     * EMP electromagnetic pulse blast wave particles.
     */
    public void spawnEmpPulse(double x, double y, int count) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 120.0 + random.nextDouble() * 180.0;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            double life = 0.35 + random.nextDouble() * 0.30;
            double size = 3.0 + random.nextDouble() * 3.5;

            Color empColor = new Color(50, 180, 255);
            addParticle(new Particle(x, y, vx, vy, life, size, empColor, true, null));
        }
    }

    /**
     * Cyberpunk data stream bits floating upwards (0, 1, hex).
     */
    public void spawnDataBits(double x, double y, int count) {
        String[] glyphs = {"0", "1", "<>", "#", "[ ]"};
        for (int i = 0; i < count; i++) {
            double vx = (random.nextDouble() - 0.5) * 30.0;
            double vy = -30.0 - random.nextDouble() * 45.0; // Float upwards
            double life = 0.5 + random.nextDouble() * 0.4;
            String glyph = glyphs[random.nextInt(glyphs.length)];

            Color bitColor = (random.nextBoolean())
                    ? new Color(0, 255, 170)
                    : new Color(0, 210, 255);

            addParticle(new Particle(x, y, vx, vy, life, 10.0, bitColor, false, glyph));
        }
    }

    /**
     * Heavy physical takedown dust impact.
     */
    public void spawnTakedownPuff(double x, double y) {
        for (int i = 0; i < 12; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 30.0 + random.nextDouble() * 60.0;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            double life = 0.3 + random.nextDouble() * 0.25;
            double size = 4.0 + random.nextDouble() * 3.0;

            Color dustColor = new Color(180, 190, 205, 150);
            addParticle(new Particle(x, y, vx, vy, life, size, dustColor, true, null));
        }
    }

    public void clear() {
        particles.clear();
    }

    public int getParticleCount() {
        return particles.size();
    }
}
