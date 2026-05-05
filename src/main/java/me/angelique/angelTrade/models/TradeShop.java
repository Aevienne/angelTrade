package me.angelique.angelTrade.models;

import org.bukkit.Location;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradeShop {

    private final String id;
    private final UUID ownerUUID;
    private String companyId;
    private Location location;
    private final List<ShopItem> items = new ArrayList<>();
    private Instant lastRelocated;
    private String linkedRouteId;

    public TradeShop(String id, UUID ownerUUID, String companyId, Location location) {
        this.id = id;
        this.ownerUUID = ownerUUID;
        this.companyId = companyId;
        this.location = location;
        this.lastRelocated = Instant.EPOCH;
    }

    public boolean canRelocate(long cooldownSeconds) {
        return (Instant.now().getEpochSecond() - lastRelocated.getEpochSecond()) >= cooldownSeconds;
    }

    public void relocate(Location newLocation) {
        location = newLocation;
        lastRelocated = Instant.now();
    }

    public void addItem(ShopItem item) { items.add(item); }

    public boolean removeItem(String itemKey) {
        return items.removeIf(i -> i.getItemKey().equalsIgnoreCase(itemKey));
    }

    public ShopItem getItem(String itemKey) {
        return items.stream().filter(i -> i.getItemKey().equalsIgnoreCase(itemKey)).findFirst().orElse(null);
    }

    // ── getters / setters ────────────────────────────────────────────────────
    public String getId() { return id; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String id) { companyId = id; }
    public Location getLocation() { return location; }
    public List<ShopItem> getItems() { return items; }
    public Instant getLastRelocated() { return lastRelocated; }
    public void setLastRelocated(Instant t) { lastRelocated = t; }
    public String getLinkedRouteId() { return linkedRouteId; }
    public void setLinkedRouteId(String id) { linkedRouteId = id; }
}
