package com.cyberheist.hacking;

/**
 * Difficulty configurations for the Network Node hacking minigame.
 * Controls sequence length, node pool size, time limit, security trace speed, and penalties.
 */
public enum HackingDifficulty {

    EASY(
            "Security Tier I // Basic Encryption",
            4,      // Node pool count
            3,      // Required sequence length
            12.0,   // Time limit in seconds
            4.5,    // Security trace build rate (% / sec)
            10.0    // Security penalty per mistaken input (%)
    ),

    MEDIUM(
            "Security Tier II // Standard Corporate ICE",
            5,      // Node pool count
            4,      // Required sequence length
            9.0,    // Time limit in seconds
            6.5,    // Security trace build rate (% / sec)
            15.0    // Security penalty per mistaken input (%)
    ),

    HARD(
            "Security Tier III // Military Countermeasures",
            6,      // Node pool count
            5,      // Required sequence length
            7.5,    // Time limit in seconds
            9.0,    // Security trace build rate (% / sec)
            20.0    // Security penalty per mistaken input (%)
    ),

    VERY_HARD(
            "Security Tier IV // Black-ICE Core Vault",
            8,      // Node pool count
            6,      // Required sequence length
            6.0,    // Time limit in seconds
            13.0,   // Security trace build rate (% / sec)
            25.0    // Security penalty per mistaken input (%)
    );

    private final String description;
    private final int nodePoolCount;
    private final int sequenceLength;
    private final double timeLimitSeconds;
    private final double traceBuildRate;
    private final double mistakePenalty;

    HackingDifficulty(String description, int nodePoolCount, int sequenceLength,
                      double timeLimitSeconds, double traceBuildRate, double mistakePenalty) {
        this.description = description;
        this.nodePoolCount = nodePoolCount;
        this.sequenceLength = sequenceLength;
        this.timeLimitSeconds = timeLimitSeconds;
        this.traceBuildRate = traceBuildRate;
        this.mistakePenalty = mistakePenalty;
    }

    public String getDescription() { return description; }
    public int getNodePoolCount() { return nodePoolCount; }
    public int getSequenceLength() { return sequenceLength; }
    public double getTimeLimitSeconds() { return timeLimitSeconds; }
    public double getTraceBuildRate() { return traceBuildRate; }
    public double getMistakePenalty() { return mistakePenalty; }
}
