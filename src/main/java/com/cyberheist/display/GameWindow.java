package com.cyberheist.display;

import com.cyberheist.core.GameConfig;
import com.cyberheist.input.KeyboardInput;
import com.cyberheist.input.MouseInput;
import com.cyberheist.util.Logger;

import javax.swing.JFrame;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;

/**
 * Encapsulates the application JFrame and active Canvas surface.
 * Configures the buffer strategy and attaches input listeners.
 */
public final class GameWindow {

    private static final Logger LOGGER = Logger.getLogger(GameWindow.class);

    private final JFrame frame;
    private final Canvas canvas;
    private BufferStrategy bufferStrategy;

    public GameWindow(String title, int width, int height,
                      KeyboardInput keyboardInput, MouseInput mouseInput,
                      Runnable onCloseCallback) {
        LOGGER.info("Initializing game window [" + width + "x" + height + "]");

        Dimension dimension = new Dimension(width, height);

        this.canvas = new Canvas();
        this.canvas.setPreferredSize(dimension);
        this.canvas.setMaximumSize(dimension);
        this.canvas.setMinimumSize(dimension);
        this.canvas.setFocusable(true);
        this.canvas.setBackground(GameConfig.COLOR_BACKGROUND);

        // Attach input listeners directly to canvas
        this.canvas.addKeyListener(keyboardInput);
        this.canvas.addMouseListener(mouseInput);
        this.canvas.addMouseMotionListener(mouseInput);
        this.canvas.addMouseWheelListener(mouseInput);

        this.frame = new JFrame(title);
        this.frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.frame.setResizable(GameConfig.WINDOW_RESIZABLE);
        this.frame.add(this.canvas);
        this.frame.pack();
        this.frame.setLocationRelativeTo(null); // Center on screen

        this.frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                LOGGER.info("Window close signal received.");
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                } else {
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    /**
     * Makes the window visible and prepares the hardware-accelerated buffer strategy.
     */
    public void show() {
        this.frame.setVisible(true);
        this.frame.toFront();
        this.frame.requestFocus();
        this.canvas.requestFocusInWindow();

        // Create buffer strategy after window is visible on screen
        this.canvas.createBufferStrategy(GameConfig.BUFFER_COUNT);
        this.bufferStrategy = this.canvas.getBufferStrategy();
        LOGGER.info("BufferStrategy created with " + GameConfig.BUFFER_COUNT + " buffers.");
    }

    public BufferStrategy getBufferStrategy() {
        if (this.bufferStrategy == null) {
            this.canvas.createBufferStrategy(GameConfig.BUFFER_COUNT);
            this.bufferStrategy = this.canvas.getBufferStrategy();
        }
        return this.bufferStrategy;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public JFrame getFrame() {
        return frame;
    }

    public void dispose() {
        LOGGER.info("Disposing game window.");
        if (bufferStrategy != null) {
            bufferStrategy.dispose();
        }
        frame.dispose();
    }
}
