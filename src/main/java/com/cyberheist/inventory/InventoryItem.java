package com.cyberheist.inventory;

/**
 * Represents an item stack with dynamic capacity in the player's inventory.
 */
public final class InventoryItem {

    private final ItemType type;
    private int count;
    private int maxCapacity;

    public InventoryItem(ItemType type, int count, int maxCapacity) {
        this.type = type;
        this.count = Math.max(0, Math.min(count, maxCapacity));
        this.maxCapacity = Math.max(1, maxCapacity);
    }

    public boolean consume(int amount) {
        if (count >= amount && amount > 0) {
            count -= amount;
            return true;
        }
        return false;
    }

    public boolean add(int amount) {
        if (amount <= 0 || isFull()) return false;
        int toAdd = Math.min(amount, maxCapacity - count);
        count += toAdd;
        return true;
    }

    public boolean isFull() {
        return count >= maxCapacity;
    }

    public boolean hasAny() {
        return count > 0;
    }

    public ItemType getType() { return type; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = Math.max(0, count); }
    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = Math.max(1, maxCapacity);
        if (this.count > this.maxCapacity) {
            this.count = this.maxCapacity;
        }
    }
}
