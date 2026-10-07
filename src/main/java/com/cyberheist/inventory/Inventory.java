package com.cyberheist.inventory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages player equipment, consumables, mission data payload, and capacity limits.
 */
public final class Inventory {

    private final Map<ItemType, InventoryItem> items = new LinkedHashMap<>();
    private boolean dataPayloadSecured = false;

    public Inventory() {
        // Empty by default
    }

    /**
     * Creates the canonical mission starting loadout:
     * EMP × 2, Lockpick × 3, Medkit × 1, Data Drive × 1.
     */
    public static Inventory createStartingLoadout() {
        Inventory inv = new Inventory();
        inv.registerItem(ItemType.EMP, 2, 2);
        inv.registerItem(ItemType.LOCKPICK, 3, 3);
        inv.registerItem(ItemType.MEDKIT, 1, 1);
        inv.registerItem(ItemType.DATA_DRIVE, 1, 1);
        return inv;
    }

    public void registerItem(ItemType type, int initialCount, int maxCapacity) {
        items.put(type, new InventoryItem(type, initialCount, maxCapacity));
    }

    public int getCount(ItemType type) {
        InventoryItem item = items.get(type);
        return item != null ? item.getCount() : 0;
    }

    public int getMaxCapacity(ItemType type) {
        InventoryItem item = items.get(type);
        return item != null ? item.getMaxCapacity() : 0;
    }

    public boolean hasItem(ItemType type, int count) {
        return getCount(type) >= count;
    }

    public boolean hasItem(ItemType type) {
        return hasItem(type, 1);
    }

    public boolean consumeItem(ItemType type, int count) {
        InventoryItem item = items.get(type);
        return item != null && item.consume(count);
    }

    public boolean consumeItem(ItemType type) {
        return consumeItem(type, 1);
    }

    public boolean addItem(ItemType type, int count) {
        InventoryItem item = items.get(type);
        if (item != null) {
            return item.add(count);
        } else {
            registerItem(type, count, type.getDefaultMax());
            return true;
        }
    }

    public void setItemCount(ItemType type, int count) {
        InventoryItem item = items.get(type);
        if (item != null) {
            item.setCount(count);
        } else {
            registerItem(type, count, type.getDefaultMax());
        }
    }

    public void applyCapacityBonus(int bonus) {
        for (InventoryItem item : items.values()) {
            if (item.getType() != ItemType.DATA_DRIVE) {
                item.setMaxCapacity(item.getType().getDefaultMax() + bonus);
            }
        }
    }

    public boolean isDataPayloadSecured() {
        return dataPayloadSecured;
    }

    public void setDataPayloadSecured(boolean secured) {
        this.dataPayloadSecured = secured;
    }

    public Map<ItemType, InventoryItem> getItems() {
        return Collections.unmodifiableMap(items);
    }
}
