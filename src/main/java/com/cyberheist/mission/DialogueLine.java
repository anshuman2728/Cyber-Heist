package com.cyberheist.mission;

import com.cyberheist.core.GameConfig;

import java.awt.Color;

/**
 * Represents a single contextual radio comms transmission.
 * Designed for authentic, gritty, concise human storytelling.
 */
public final class DialogueLine {

    private final String speaker;
    private final String role;
    private final String text;
    private final double duration;
    private final Color color;

    public DialogueLine(String speaker, String role, String text, double duration, Color color) {
        this.speaker = speaker;
        this.role = role;
        this.text = text;
        this.duration = duration;
        this.color = color;
    }

    public static DialogueLine mira(String text) {
        return new DialogueLine("MIRA", "HANDLER", text, 3.2, GameConfig.COLOR_CYAN);
    }

    public static DialogueLine mira(String text, double duration) {
        return new DialogueLine("MIRA", "HANDLER", text, duration, GameConfig.COLOR_CYAN);
    }

    public static DialogueLine kavin(String text) {
        return new DialogueLine("KAVIN", "INFILTRATOR", text, 3.0, GameConfig.COLOR_TEXT_BRIGHT);
    }

    public static DialogueLine kavin(String text, double duration) {
        return new DialogueLine("KAVIN", "INFILTRATOR", text, duration, GameConfig.COLOR_TEXT_BRIGHT);
    }

    public static DialogueLine alert(String text) {
        return new DialogueLine("MIRA", "URGENT", text, 3.5, Color.RED);
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public double getDuration() {
        return duration;
    }

    public Color getColor() {
        return color;
    }
}
