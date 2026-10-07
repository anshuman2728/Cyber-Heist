package com.cyberheist.mission;

import com.cyberheist.core.GameConfig;
import com.cyberheist.util.Logger;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

/**
 * Manages the sequencing, timing, and playback of radio dialogue communications.
 * Decoupled from rendering and completely headless testable.
 */
public final class RadioDialogueManager {

    private static final Logger LOGGER = Logger.getLogger(RadioDialogueManager.class);

    private final Queue<DialogueLine> lineQueue = new ArrayDeque<>();
    private DialogueLine currentLine = null;
    private double currentLineTimer = 0.0;
    private double audioWaveformTimer = 0.0;

    public void queueLine(DialogueLine line) {
        if (line == null) return;
        if (currentLine == null) {
            this.currentLine = line;
            this.currentLineTimer = line.getDuration();
            LOGGER.info(String.format("Radio Transmission [%s]: \"%s\"", line.getSpeaker(), line.getText()));
        } else {
            lineQueue.offer(line);
        }
    }

    public void queueLines(DialogueLine... lines) {
        if (lines != null) {
            queueLines(Arrays.asList(lines));
        }
    }

    public void queueLines(List<DialogueLine> lines) {
        if (lines == null) return;
        for (DialogueLine line : lines) {
            queueLine(line);
        }
    }

    public void queueTransmission(String speaker, String text, double duration) {
        queueLine(new DialogueLine(speaker, "RADIO", text, duration, GameConfig.COLOR_CYAN));
    }

    public void update(double deltaTime) {
        audioWaveformTimer += deltaTime * 12.0;

        if (currentLine != null) {
            currentLineTimer -= deltaTime;
            if (currentLineTimer <= 0.0) {
                advanceToNextLine();
            }
        }
    }

    private void advanceToNextLine() {
        if (!lineQueue.isEmpty()) {
            this.currentLine = lineQueue.poll();
            this.currentLineTimer = currentLine.getDuration();
            LOGGER.info(String.format("Radio Transmission [%s]: \"%s\"", currentLine.getSpeaker(), currentLine.getText()));
        } else {
            this.currentLine = null;
            this.currentLineTimer = 0.0;
        }
    }

    public void skipCurrentLine() {
        advanceToNextLine();
    }

    public void clear() {
        lineQueue.clear();
        currentLine = null;
        currentLineTimer = 0.0;
    }

    public boolean hasActiveDialogue() {
        return currentLine != null;
    }

    public DialogueLine getCurrentLine() {
        return currentLine;
    }

    public double getCurrentLineTimer() {
        return currentLineTimer;
    }

    public int getQueuedCount() {
        return lineQueue.size();
    }

    public double getAudioWaveformTimer() {
        return audioWaveformTimer;
    }
}
