package com.cyberheist.combat;

import com.cyberheist.entity.FacingDirection;
import com.cyberheist.entity.Player;
import com.cyberheist.entity.PlayerState;
import com.cyberheist.inventory.Inventory;
import com.cyberheist.inventory.ItemType;
import com.cyberheist.progression.ProgressionManager;
import com.cyberheist.stealth.Guard;
import com.cyberheist.stealth.NoiseType;
import com.cyberheist.stealth.StealthSystem;
import com.cyberheist.util.Logger;
import com.cyberheist.world.SecurityCamera;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Coordinates non-lethal tactical combat, gadgets, weapon cooldowns,
 * projectile physics, and stealth takedown triggers.
 */
public final class CombatManager {

    private static final Logger LOGGER = Logger.getLogger(CombatManager.class);

    private double stunCooldownTimer = 0.0;
    private double distractionCooldownTimer = 0.0;

    private final List<DistractionProjectile> activeProjectiles = new ArrayList<>();
    private final List<CombatEffect> activeEffects = new ArrayList<>();

    private Guard activeTakedownTarget = null;
    private String combatNotification = null;
    private double notificationTimer = 0.0;

    public CombatManager() {
    }

    public void update(double deltaTime, Player player, List<Guard> guards, StealthSystem stealth) {
        if (stunCooldownTimer > 0.0) {
            stunCooldownTimer = Math.max(0.0, stunCooldownTimer - deltaTime);
        }
        if (distractionCooldownTimer > 0.0) {
            distractionCooldownTimer = Math.max(0.0, distractionCooldownTimer - deltaTime);
        }
        if (notificationTimer > 0.0) {
            notificationTimer -= deltaTime;
            if (notificationTimer <= 0.0) {
                combatNotification = null;
            }
        }

        // 1. Update distraction projectiles
        Iterator<DistractionProjectile> projIter = activeProjectiles.iterator();
        while (projIter.hasNext()) {
            DistractionProjectile p = projIter.next();
            p.update(deltaTime);
            if (p.hasLanded()) {
                // Detonate acoustic distraction at landing site
                if (stealth != null) {
                    stealth.emitNoise(p.getTargetX(), p.getTargetY(), NoiseType.DOOR_OPEN);
                }
                activeEffects.add(CombatEffect.createDecoyPing(p.getTargetX(), p.getTargetY()));
                projIter.remove();
                LOGGER.info(String.format("Distraction decoy landed at (%.0f, %.0f) - acoustic wave emitted.",
                        p.getTargetX(), p.getTargetY()));
            }
        }

        // 2. Update visual effects
        Iterator<CombatEffect> effIter = activeEffects.iterator();
        while (effIter.hasNext()) {
            CombatEffect eff = effIter.next();
            eff.update(deltaTime);
            if (eff.isExpired()) {
                effIter.remove();
            }
        }

        // 3. Evaluate stealth takedown proximity
        this.activeTakedownTarget = findTakedownTarget(player, guards);
    }

    /**
     * Checks if a conscious guard is positioned in front of player with back turned.
     */
    public Guard findTakedownTarget(Player player, List<Guard> guards) {
        if (player == null || guards == null || player.getState() == PlayerState.DEAD || player.getState() == PlayerState.STUNNED) {
            return null;
        }

        double px = player.getX() + 14.0;
        double py = player.getY() + 14.0;

        for (Guard guard : guards) {
            if (guard.isIncapacitated()) continue;

            double gx = guard.getX() + 14.0;
            double gy = guard.getY() + 14.0;
            double dist = Math.hypot(px - gx, py - gy);

            if (dist <= 50.0) {
                // Angle from guard to player
                double angleToPlayer = Math.atan2(py - gy, px - gx);
                double guardFacing = guard.getFacingAngle();
                double angleDiff = Math.abs(normalizeAngle(angleToPlayer - guardFacing));

                // Player must be behind the guard (angle difference > 75 degrees)
                if (angleDiff > Math.toRadians(75.0)) {
                    // Guard cannot be in full DETECTED combat alert
                    if (guard.getDetectionMeter().getState() != com.cyberheist.stealth.DetectionState.DETECTED) {
                        return guard;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Executes non-lethal stealth takedown on nearby unaware guard.
     */
    public boolean executeTakedown(Player player, ProgressionManager progression) {
        if (activeTakedownTarget == null || player == null) return false;

        Guard target = activeTakedownTarget;
        target.knockOut();
        player.startAction(PlayerState.INTERACTING, 0.7);

        if (progression != null) {
            progression.addXp(40);
        }

        setNotification("STEALTH TAKEDOWN EXECUTED (+40 XP)");
        LOGGER.info("Player executed non-lethal stealth takedown on guard: " + target.getName());
        return true;
    }

    /**
     * Discharges directional electric arc taser in player's facing direction.
     */
    public boolean fireStunDevice(Player player, List<Guard> guards, StealthSystem stealth, ProgressionManager progression) {
        if (stunCooldownTimer > 0.0 || player == null) return false;

        double cooldown = progression != null ? progression.getStunCooldown() : 6.0;
        double range = progression != null ? progression.getStunArcRange() : 90.0;
        this.stunCooldownTimer = cooldown;

        double px = player.getX() + 14.0;
        double py = player.getY() + 14.0;

        FacingDirection facing = player.getFacingDirection();
        double aimAngle = Math.toRadians(facing.getAngleDegrees());

        // Emit electrical discharge noise
        if (stealth != null) {
            stealth.emitNoise(px, py, NoiseType.HACKING);
        }

        // Find nearest guard in shock arc
        Guard hitGuard = null;
        double closestDist = Double.MAX_VALUE;

        if (guards != null) {
            for (Guard guard : guards) {
                if (guard.isUnconscious()) continue;

                double gx = guard.getX() + 14.0;
                double gy = guard.getY() + 14.0;
                double dist = Math.hypot(gx - px, gy - py);

                if (dist <= range) {
                    double angleToGuard = Math.atan2(gy - py, gx - px);
                    double angleDiff = Math.abs(normalizeAngle(angleToGuard - aimAngle));

                    if (angleDiff <= Math.toRadians(45.0) && dist < closestDist) {
                        closestDist = dist;
                        hitGuard = guard;
                    }
                }
            }
        }

        if (hitGuard != null) {
            hitGuard.applyStun(5.0);
            activeEffects.add(CombatEffect.createShockArc(px, py, hitGuard.getX() + 14.0, hitGuard.getY() + 14.0));
            if (progression != null) {
                progression.addXp(25);
            }
            setNotification("GUARD STUNNED (5.0s) // +25 XP");
            LOGGER.info("Stun device struck guard: " + hitGuard.getName());
        } else {
            // Discharged forward into empty space
            double endX = px + Math.cos(aimAngle) * range;
            double endY = py + Math.sin(aimAngle) * range;
            activeEffects.add(CombatEffect.createShockArc(px, py, endX, endY));
            setNotification("STUN DEVICE DISCHARGED");
        }
        return true;
    }

    /**
     * Deploys electromagnetic pulse canister, disabling cameras and stunning guards in radius.
     */
    public boolean deployEmp(Player player, Inventory inventory, List<Guard> guards,
                             List<SecurityCamera> cameras, StealthSystem stealth, ProgressionManager progression) {
        if (player == null || inventory == null) return false;
        if (!inventory.consumeItem(ItemType.EMP, 1)) {
            setNotification("NO EMP CANISTERS REMAINING");
            return false;
        }

        double radius = progression != null ? progression.getEmpRadius() : 220.0;
        double px = player.getX() + 14.0;
        double py = player.getY() + 14.0;

        // Overload cameras in blast radius
        int camsDisabled = 0;
        if (cameras != null) {
            for (SecurityCamera cam : cameras) {
                if (Math.hypot(cam.getX() - px, cam.getY() - py) <= radius) {
                    cam.applyEmpDisable(15.0);
                    camsDisabled++;
                }
            }
        }

        // Overload guard cybernetics
        int guardsStunned = 0;
        if (guards != null) {
            for (Guard guard : guards) {
                if (!guard.isUnconscious() && Math.hypot(guard.getX() + 14.0 - px, guard.getY() + 14.0 - py) <= radius) {
                    guard.applyStun(6.0);
                    guardsStunned++;
                }
            }
        }

        // Acoustic shockwave
        if (stealth != null) {
            stealth.emitNoise(px, py, NoiseType.EMP);
        }

        // Visual EMP wave ring
        activeEffects.add(CombatEffect.createEmpWave(px, py, radius));

        if (progression != null) {
            progression.addXp(35);
        }

        setNotification(String.format("EMP DETONATED // %d GUARDS STUNNED, %d CAMERAS DISABLED (+35 XP)",
                guardsStunned, camsDisabled));
        LOGGER.info(String.format("EMP deployed! Stunned %d guards, disabled %d cameras.", guardsStunned, camsDisabled));
        return true;
    }

    /**
     * Throws an acoustic distraction decoy toward destination.
     */
    public boolean throwDistraction(Player player, double targetX, double targetY,
                                     StealthSystem stealth, ProgressionManager progression) {
        if (distractionCooldownTimer > 0.0 || player == null) {
            return false;
        }

        double maxRange = 180.0 * (progression != null ? progression.getThrowRangeMultiplier() : 1.0);
        double px = player.getX() + 14.0;
        double py = player.getY() + 14.0;

        double dx = targetX - px;
        double dy = targetY - py;
        double dist = Math.hypot(dx, dy);

        if (dist > maxRange && dist > 0.001) {
            targetX = px + (dx / dist) * maxRange;
            targetY = py + (dy / dist) * maxRange;
        }

        activeProjectiles.add(new DistractionProjectile(px, py, targetX, targetY));
        this.distractionCooldownTimer = 3.2;

        setNotification("DISTRACTION DECOY THROWN");
        LOGGER.info(String.format("Player threw distraction decoy toward (%.0f, %.0f)", targetX, targetY));
        return true;
    }

    public void renderWorld(Graphics2D g2d) {
        // Render projectiles
        for (DistractionProjectile p : activeProjectiles) {
            int cx = (int) Math.round(p.getCurrentX());
            int cy = (int) Math.round(p.getCurrentY());
            g2d.setColor(new Color(255, 200, 0));
            g2d.fillOval(cx - 3, cy - 3, 6, 6);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(cx - 4, cy - 4, 8, 8);
        }

        // Render effects
        for (CombatEffect eff : activeEffects) {
            eff.render(g2d);
        }

        // Render takedown prompt over target guard
        if (activeTakedownTarget != null) {
            int gx = (int) Math.round(activeTakedownTarget.getX() + 14.0);
            int gy = (int) Math.round(activeTakedownTarget.getY() - 16.0);

            g2d.setColor(new Color(10, 14, 23, 220));
            g2d.fillRoundRect(gx - 45, gy - 12, 90, 20, 4, 4);
            g2d.setColor(new Color(255, 0, 128));
            g2d.drawRoundRect(gx - 45, gy - 12, 90, 20, 4, 4);

            g2d.setFont(new Font("Consolas", Font.BOLD, 10));
            g2d.setColor(Color.WHITE);
            g2d.drawString("[F] TAKEDOWN", gx - 36, gy + 2);
        }
    }

    private void setNotification(String text) {
        this.combatNotification = text;
        this.notificationTimer = 2.4;
    }

    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    public double getStunCooldownTimer() { return stunCooldownTimer; }
    public double getDistractionCooldownTimer() { return distractionCooldownTimer; }
    public Guard getActiveTakedownTarget() { return activeTakedownTarget; }
    public String getCombatNotification() { return combatNotification; }
    public List<CombatEffect> getActiveEffects() { return activeEffects; }
}
