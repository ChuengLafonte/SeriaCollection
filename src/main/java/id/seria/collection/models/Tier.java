package id.seria.collection.models;

import java.util.List;

public class Tier {
    private final int level;
    private final int requirement;
    private final List<String> rewards;
    private final List<String> displayRewards;

    public Tier(int level, int requirement, List<String> rewards, List<String> displayRewards) {
        this.level = level;
        this.requirement = requirement;
        this.rewards = rewards;
        this.displayRewards = displayRewards;
    }

    public int getLevel() { return level; }
    public int getRequirement() { return requirement; }
    public List<String> getRewards() { return rewards; }
    public List<String> getDisplayRewards() { return displayRewards; }
}
