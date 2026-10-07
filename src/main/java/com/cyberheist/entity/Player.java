package com.cyberheist.entity;

import com.cyberheist.animation.AnimationController;
import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.Collidable;
import com.cyberheist.collision.CollisionWorld;
import com.cyberheist.core.GameConfig;
import com.cyberheist.input.KeyboardInput;
import com.cyberheist.interact.Interactable;
import com.cyberheist.util.Logger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Core Player Entity for Cyber Heist.
 * Coordinates movement physics, state machine, stamina consumption,
 * collision sliding, and interaction triggers.
 */
public final class Player implements Collidable {

    private static final Logger LOGGER = Logger.getLogger(Player.class);

    // Position & Spatial Bounds
    private double x;
    private double y;
    private final double width = 28.0;
    private final double height = 28.0;

    // Physics & Velocity
    private double velocityX = 0.0;
    private double velocityY = 0.0;

    // Configurable Movement Parameters (Human Game Designer Tuned)
    private double walkSpeed = 205.0;          // Pixels per second (deliberate stealth infiltration)
    private double sprintSpeed = 365.0;        // Pixels per second (responsive burst dash)
    private double accelerationRate = 1350.0;  // Smooth acceleration curve
    private double decelerationRate = 1950.0;  // Crisp stopping friction - no slippery sliding
    private double speedMultiplier = 1.0;      // Plug-in hook for stealth / encumbrance

    // State & Systems
    private PlayerState state = PlayerState.IDLE;
    private FacingDirection facingDirection = FacingDirection.SOUTH;
    private final StaminaSystem staminaSystem;
    private final AnimationController animationController;

    // Interaction State
    private Interactable activeInteractable = null;
    private double interactionTimer = 0.0;
    private static final double INTERACTION_DURATION = 0.45; // Seconds

    // Stun / Damage Hook
    private double stunTimer = 0.0;

    // Acoustic Footstep Generation & Particle Dust
    private double footstepTimer = 0.0;
    private java.util.function.Consumer<com.cyberheist.stealth.NoiseType> noiseEmitter = null;

    public interface FootstepDustListener {
        void onFootstep(double x, double y, double dirX, double dirY, boolean running);
    }
    private FootstepDustListener footstepDustListener = null;

    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.staminaSystem = StaminaSystem.createDefault();
        this.animationController = new AnimationController();
    }

    /**
     * Executes the player's per-tick update cycle.
     */
    public void update(double deltaTime, KeyboardInput keyboard,
                       CollisionWorld collisionWorld, List<Interactable> interactables) {

        // 1. Update active timers (Stun, Interaction, etc.)
        updateTimers(deltaTime);

        // 2. Resolve nearby interactables
        updateInteractionProximity(interactables);

        // 3. Process interaction input trigger
        if (state.canInteract() && activeInteractable != null && keyboard.isKeyPressed(KeyEvent.VK_E)) {
            triggerInteraction();
        }

        // 4. Test Stun trigger shortcut (Key 'T' for testing state transitions)
        if (keyboard.isKeyPressed(KeyEvent.VK_T) && state != PlayerState.DEAD) {
            applyStun(1.5);
        }

        // 5. Calculate movement if permitted
        if (state.canMove()) {
            handleMovement(deltaTime, keyboard, collisionWorld);
        } else {
            // Apply deceleration to slide to a halt when immobilized
            applyDeceleration(deltaTime);
            resolveCollisionMovement(deltaTime, collisionWorld);
        }

        // 6. Update animation controller and acoustic footstep noise
        double currentSpeed = Math.hypot(velocityX, velocityY);
        animationController.update(deltaTime, state, currentSpeed);

        if (state == PlayerState.WALKING) {
            footstepTimer += deltaTime;
            if (footstepTimer >= 0.44) {
                footstepTimer = 0.0;
                com.cyberheist.audio.AudioManager.getInstance().playFootstep(false);
                if (noiseEmitter != null) noiseEmitter.accept(com.cyberheist.stealth.NoiseType.WALKING);
                if (footstepDustListener != null) {
                    footstepDustListener.onFootstep(x + width / 2.0, y + height - 2.0,
                            facingDirection.getDirX(), facingDirection.getDirY(), false);
                }
            }
        } else if (state == PlayerState.RUNNING) {
            footstepTimer += deltaTime;
            if (footstepTimer >= 0.28) {
                footstepTimer = 0.0;
                com.cyberheist.audio.AudioManager.getInstance().playFootstep(true);
                if (noiseEmitter != null) noiseEmitter.accept(com.cyberheist.stealth.NoiseType.RUNNING);
                if (footstepDustListener != null) {
                    footstepDustListener.onFootstep(x + width / 2.0, y + height - 2.0,
                            facingDirection.getDirX(), facingDirection.getDirY(), true);
                }
            }
        } else {
            footstepTimer = 0.0;
        }
    }

    private void updateTimers(double deltaTime) {
        if (state == PlayerState.STUNNED) {
            stunTimer -= deltaTime;
            if (stunTimer <= 0.0) {
                LOGGER.info("Player recovered from STUNNED state.");
                state = PlayerState.IDLE;
            }
        } else if (state == PlayerState.INTERACTING) {
            interactionTimer -= deltaTime;
            if (interactionTimer <= 0.0) {
                state = PlayerState.IDLE;
            }
        }
    }

    public interface InteractionListener {
        boolean onAttemptInteract(Interactable interactable);
    }

    private InteractionListener interactionListener = null;

    public void setInteractionListener(InteractionListener listener) {
        this.interactionListener = listener;
    }

    private void triggerInteraction() {
        if (activeInteractable != null && activeInteractable.canInteract()) {
            if (interactionListener != null && interactionListener.onAttemptInteract(activeInteractable)) {
                this.state = PlayerState.INTERACTING;
                this.velocityX = 0.0;
                this.velocityY = 0.0;
                return;
            }
            LOGGER.info("Player interacting with: " + activeInteractable.getName());
            activeInteractable.onInteract();
            this.state = PlayerState.INTERACTING;
            this.interactionTimer = INTERACTION_DURATION;
            this.velocityX = 0.0;
            this.velocityY = 0.0;
        }
    }

    private void updateInteractionProximity(List<Interactable> interactables) {
        activeInteractable = null;
        if (interactables == null) return;

        BoundingBox playerBox = getBoundingBox();
        for (Interactable interactable : interactables) {
            if (playerBox.intersects(interactable.getInteractionTriggerZone())) {
                activeInteractable = interactable;
                break; // Select first reachable interactable
            }
        }
    }

    private void handleMovement(double deltaTime, KeyboardInput keyboard, CollisionWorld collisionWorld) {
        // Read raw directional input vector
        double inputX = 0;
        double inputY = 0;

        if (keyboard.isKeyDown(KeyEvent.VK_W) || keyboard.isKeyDown(KeyEvent.VK_UP)) inputY -= 1.0;
        if (keyboard.isKeyDown(KeyEvent.VK_S) || keyboard.isKeyDown(KeyEvent.VK_DOWN)) inputY += 1.0;
        if (keyboard.isKeyDown(KeyEvent.VK_A) || keyboard.isKeyDown(KeyEvent.VK_LEFT)) inputX -= 1.0;
        if (keyboard.isKeyDown(KeyEvent.VK_D) || keyboard.isKeyDown(KeyEvent.VK_RIGHT)) inputX += 1.0;

        double inputMagnitude = Math.hypot(inputX, inputY);
        if (inputMagnitude > 0.0) {
            inputX /= inputMagnitude;
            inputY /= inputMagnitude;
        }

        // Sprint & Stamina evaluation
        boolean sprintRequested = (keyboard.isKeyDown(KeyEvent.VK_SHIFT) || keyboard.isKeyDown(KeyEvent.VK_SPACE))
                && inputMagnitude > 0.0;
        boolean canSprint = sprintRequested && staminaSystem.canSprint();

        staminaSystem.update(deltaTime, canSprint);

        // Determine target velocity
        double currentMaxSpeed = (canSprint ? sprintSpeed : walkSpeed) * speedMultiplier;
        double targetVx = inputX * (inputMagnitude > 0.0 ? currentMaxSpeed : 0.0);
        double targetVy = inputY * (inputMagnitude > 0.0 ? currentMaxSpeed : 0.0);

        // Smooth Acceleration / Deceleration
        if (inputX != 0.0) {
            velocityX += (targetVx - velocityX) * Math.min(1.0, accelerationRate * deltaTime);
        } else {
            velocityX -= velocityX * Math.min(1.0, decelerationRate * deltaTime);
            if (Math.abs(velocityX) < 2.0) velocityX = 0.0;
        }

        if (inputY != 0.0) {
            velocityY += (targetVy - velocityY) * Math.min(1.0, accelerationRate * deltaTime);
        } else {
            velocityY -= velocityY * Math.min(1.0, decelerationRate * deltaTime);
            if (Math.abs(velocityY) < 2.0) velocityY = 0.0;
        }

        // Apply collision sliding on separate axes
        resolveCollisionMovement(deltaTime, collisionWorld);

        // Update facing direction and state
        double currentSpeed = Math.hypot(velocityX, velocityY);
        if (currentSpeed > 8.0) {
            this.facingDirection = FacingDirection.fromVector(velocityX, velocityY);
            this.state = canSprint ? PlayerState.RUNNING : PlayerState.WALKING;
        } else {
            this.state = PlayerState.IDLE;
        }
    }

    private void applyDeceleration(double deltaTime) {
        velocityX -= velocityX * Math.min(1.0, decelerationRate * deltaTime);
        if (Math.abs(velocityX) < 2.0) velocityX = 0.0;

        velocityY -= velocityY * Math.min(1.0, decelerationRate * deltaTime);
        if (Math.abs(velocityY) < 2.0) velocityY = 0.0;

        staminaSystem.update(deltaTime, false);
    }

    private void resolveCollisionMovement(double deltaTime, CollisionWorld collisionWorld) {
        if (collisionWorld == null) {
            x += velocityX * deltaTime;
            y += velocityY * deltaTime;
            return;
        }

        // 1. Anti-Stuck Depenetration Check: If already overlapping an obstacle, nudge to nearest free space
        BoundingBox currentBox = new BoundingBox(x, y, width, height);
        if (collisionWorld.checkCollision(currentBox, this)) {
            depenetrateFromObstacles(collisionWorld);
        }

        // 2. Continuous Collision Detection (CCD) via Sub-Stepping
        // Prevents tunneling through thin walls during sprint speed or frame delta spikes
        double stepDist = Math.hypot(velocityX * deltaTime, velocityY * deltaTime);
        int subSteps = Math.max(1, (int) Math.ceil(stepDist / 6.0));
        double dtSub = deltaTime / subSteps;

        for (int i = 0; i < subSteps; i++) {
            // Axis-independent collision detection allows smooth wall sliding
            // Sub-step along X axis
            double proposedX = x + velocityX * dtSub;
            BoundingBox testBoxX = new BoundingBox(proposedX, y, width, height);
            if (!collisionWorld.checkCollision(testBoxX, this)) {
                x = proposedX;
            } else {
                velocityX = 0.0;
            }

            // Sub-step along Y axis
            double proposedY = y + velocityY * dtSub;
            BoundingBox testBoxY = new BoundingBox(x, proposedY, width, height);
            if (!collisionWorld.checkCollision(testBoxY, this)) {
                y = proposedY;
            } else {
                velocityY = 0.0;
            }
        }

        // 3. Absolute Map Boundary Hard Clamping (Player can NEVER leave map)
        double maxX = Math.max(0, collisionWorld.getWorldWidth() - width);
        double maxY = Math.max(0, collisionWorld.getWorldHeight() - height);
        this.x = Math.max(0.0, Math.min(maxX, this.x));
        this.y = Math.max(0.0, Math.min(maxY, this.y));
    }

    /**
     * Resolves situation where player is trapped or clipped inside a solid collidable
     * by scanning cardinal offsets for the closest free non-colliding coordinates.
     */
    private void depenetrateFromObstacles(CollisionWorld collisionWorld) {
        double[] offsets = {2.0, 4.0, 8.0, 16.0, 24.0, 32.0, 40.0, 48.0, 64.0, 80.0};
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}};

        for (double d : offsets) {
            for (int[] dir : directions) {
                double testX = x + dir[0] * d;
                double testY = y + dir[1] * d;
                BoundingBox testBox = new BoundingBox(testX, testY, width, height);
                if (!collisionWorld.checkCollision(testBox, this)) {
                    this.x = testX;
                    this.y = testY;
                    LOGGER.info(String.format("Player depenetrated from obstacle: shifted by (%.0f, %.0f)", dir[0] * d, dir[1] * d));
                    return;
                }
            }
        }
    }

    public void applyStun(double duration) {
        this.state = PlayerState.STUNNED;
        this.stunTimer = duration;
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        LOGGER.info("Player STUNNED for " + duration + "s.");
    }

    public void render(Graphics2D g2d) {
        int px = (int) Math.round(x);
        int py = (int) Math.round(y);
        int pw = (int) Math.round(width);
        int ph = (int) Math.round(height);

        // 1. Subtle vertical bob when moving / breathing when idle
        int bobY = (state == PlayerState.WALKING || state == PlayerState.RUNNING)
                ? (int) Math.round(animationController.getStrideSine() * 2.0)
                : (state == PlayerState.IDLE)
                ? (int) Math.round(animationController.getIdlePulse() * 1.5)
                : 0;

        int renderY = py + bobY;

        // 2. Sprint Trail Ghosting when running
        if (state == PlayerState.RUNNING) {
            g2d.setColor(new Color(0, 240, 255, 45));
            g2d.fillRect((int) Math.round(x - velocityX * 0.02),
                         (int) Math.round(renderY - velocityY * 0.02), pw, ph);
        }

        // 3. Character Shadow
        g2d.setColor(new Color(0, 0, 0, 90));
        g2d.fillOval(px - 2, py + ph - 4, pw + 4, 10);

        // 4. Operative Base Hull
        Color bodyColor;
        switch (state) {
            case RUNNING -> bodyColor = GameConfig.COLOR_CYAN;
            case WALKING -> bodyColor = new Color(0x00, 0xC8, 0xDD);
            case INTERACTING -> bodyColor = GameConfig.COLOR_GREEN;
            case STUNNED -> bodyColor = GameConfig.COLOR_MAGENTA;
            case DEAD -> bodyColor = Color.DARK_GRAY;
            default -> bodyColor = new Color(0x3A, 0x86, 0xC8); // Idle blue
        }

        // Subtle animation pulse
        if (state == PlayerState.IDLE) {
            double pulse = animationController.getIdlePulse();
            g2d.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), (int) (180 + pulse * 75)));
        } else {
            g2d.setColor(bodyColor);
        }
        g2d.fillRoundRect(px, renderY, pw, ph, 8, 8);

        // 5. Operative Outer Armor Ring
        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(px, renderY, pw, ph, 8, 8);
        g2d.setStroke(new BasicStroke(1.0f));

        // 6. Facing Direction Indicator / Visor
        renderFacingVisor(g2d, px + (pw / 2), renderY + (ph / 2));

        // 7. Stun / Glitch Arc
        if (state == PlayerState.STUNNED) {
            renderStunSparks(g2d, px, renderY, pw, ph);
        }

        // 8. Dynamic Stamina Bar (Visible during sprint or recovery below max)
        if (staminaSystem.getCurrentStamina() < staminaSystem.getMaxStamina()) {
            renderStaminaBar(g2d, px, py + ph + 8, pw);
        }

        // 9. Floating Interaction Indicator above player
        if (activeInteractable != null && state.canInteract()) {
            renderFloatingInteractionBadge(g2d, px + pw / 2, renderY - 10);
        }
    }

    private void renderFloatingInteractionBadge(Graphics2D g2d, int cx, int topY) {
        double pulse = animationController.getIdlePulse();
        int bounce = (int) Math.round(pulse * 3.0);
        int badgeY = topY - bounce;

        String keyPrompt = "[E] " + activeInteractable.getPrompt();
        g2d.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 11));
        java.awt.FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(keyPrompt);

        // Badge Pill
        g2d.setColor(new Color(10, 22, 38, 220));
        g2d.fillRoundRect(cx - textW / 2 - 8, badgeY - 12, textW + 16, 16, 8, 8);

        g2d.setColor(new Color(0, 240, 255, (int) (180 + pulse * 75)));
        g2d.drawRoundRect(cx - textW / 2 - 8, badgeY - 12, textW + 16, 16, 8, 8);

        g2d.setColor(new Color(0, 255, 200));
        g2d.drawString(keyPrompt, cx - textW / 2, badgeY);
    }

    public void setFootstepDustListener(FootstepDustListener listener) {
        this.footstepDustListener = listener;
    }

    private void renderFacingVisor(Graphics2D g2d, int cx, int cy) {
        double dirX = facingDirection.getDirX();
        double dirY = facingDirection.getDirY();

        int visorDist = 14;
        int vx = cx + (int) Math.round(dirX * visorDist);
        int vy = cy + (int) Math.round(dirY * visorDist);

        g2d.setColor(GameConfig.COLOR_MAGENTA);
        g2d.fillOval(vx - 3, vy - 3, 6, 6);

        // Visor beam line
        g2d.setColor(new Color(255, 0, 127, 180));
        g2d.drawLine(cx, cy, vx, vy);
    }

    private void renderStunSparks(Graphics2D g2d, int px, int py, int pw, int ph) {
        g2d.setColor(GameConfig.COLOR_MAGENTA);
        int flicker = (int) (animationController.getStunFlicker() * 4);
        g2d.drawLine(px - 4 + flicker, py - 4, px + pw + 4, py + ph + 4 - flicker);
        g2d.drawLine(px - 4, py + ph + 4 + flicker, px + pw + 4 - flicker, py - 4);
    }

    private void renderStaminaBar(Graphics2D g2d, int bx, int by, int barWidth) {
        int barHeight = 4;
        // Background
        g2d.setColor(new Color(15, 20, 30, 200));
        g2d.fillRect(bx, by, barWidth, barHeight);

        // Fill
        Color fillColor = staminaSystem.isExhausted() ? GameConfig.COLOR_MAGENTA : GameConfig.COLOR_GREEN;
        int fillWidth = (int) Math.round(barWidth * staminaSystem.getPercentage());
        g2d.setColor(fillColor);
        g2d.fillRect(bx, by, fillWidth, barHeight);

        // Border
        g2d.setColor(GameConfig.COLOR_GRID_LINE);
        g2d.drawRect(bx, by, barWidth, barHeight);
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(x, y, width, height);
    }

    // Getters and Extensibility Hooks
    public double getX() { return x; }
    public double getY() { return y; }
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public void setVelocity(double velocityX, double velocityY) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }
    public double getCurrentSpeed() { return Math.hypot(velocityX, velocityY); }
    public PlayerState getState() { return state; }
    public FacingDirection getFacingDirection() { return facingDirection; }
    public void setFacingDirection(FacingDirection facingDirection) {
        if (facingDirection != null) {
            this.facingDirection = facingDirection;
        }
    }
    public StaminaSystem getStaminaSystem() { return staminaSystem; }
    public AnimationController getAnimationController() { return animationController; }
    public Interactable getActiveInteractable() { return activeInteractable; }

    public double getWalkSpeed() { return walkSpeed; }
    public void setWalkSpeed(double walkSpeed) { this.walkSpeed = walkSpeed; }
    public double getSprintSpeed() { return sprintSpeed; }
    public void setSprintSpeed(double sprintSpeed) { this.sprintSpeed = sprintSpeed; }
    public double getAccelerationRate() { return accelerationRate; }
    public void setAccelerationRate(double rate) { this.accelerationRate = rate; }
    public double getDecelerationRate() { return decelerationRate; }
    public void setDecelerationRate(double rate) { this.decelerationRate = rate; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(double speedMultiplier) { this.speedMultiplier = speedMultiplier; }
    public void setState(PlayerState state) { this.state = state; }

    public void setNoiseEmitter(java.util.function.Consumer<com.cyberheist.stealth.NoiseType> noiseEmitter) {
        this.noiseEmitter = noiseEmitter;
    }

    public void startAction(PlayerState actionState, double duration) {
        this.state = actionState;
        this.interactionTimer = duration;
        this.velocityX = 0.0;
        this.velocityY = 0.0;
    }
}
