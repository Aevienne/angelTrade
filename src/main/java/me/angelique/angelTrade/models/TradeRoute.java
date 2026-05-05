package me.angelique.angelTrade.models;

import org.bukkit.Location;

import java.time.Instant;
import java.util.UUID;

public class TradeRoute {

    public enum Tier { DIRT_ROAD, STONE_ROAD, GOLD_ROAD, ROYAL_ROAD }
    public enum Status { HEALTHY, INACTIVE, BROKEN }

    private final String id;
    private final UUID ownerUUID;
    private String ownerCompanyId;
    private Location locationA;
    private Location locationB;
    private int uses;
    private Tier tier;
    private Status status;
    private Instant lastUsed;
    private boolean insured;
    private double insurancePool;
    private Instant brokenAt;

    public TradeRoute(String id, UUID ownerUUID, Location locationA, Location locationB) {
        this.id = id;
        this.ownerUUID = ownerUUID;
        this.locationA = locationA;
        this.locationB = locationB;
        this.uses = 0;
        this.tier = Tier.DIRT_ROAD;
        this.status = Status.HEALTHY;
        this.lastUsed = Instant.now();
        this.insured = false;
        this.insurancePool = 0.0;
    }

    public void recordUse() {
        uses++;
        lastUsed = Instant.now();
        status = Status.HEALTHY;
        recalcTier();
    }

    private void recalcTier() {
        if (uses > 150) tier = Tier.ROYAL_ROAD;
        else if (uses > 60) tier = Tier.GOLD_ROAD;
        else if (uses > 20) tier = Tier.STONE_ROAD;
        else tier = Tier.DIRT_ROAD;
    }

    public long daysSinceLastUse() {
        return (Instant.now().getEpochSecond() - lastUsed.getEpochSecond()) / 86400;
    }

    // ── getters / setters ────────────────────────────────────────────────────
    public String getId() { return id; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerCompanyId() { return ownerCompanyId; }
    public void setOwnerCompanyId(String id) { ownerCompanyId = id; }
    public Location getLocationA() { return locationA; }
    public Location getLocationB() { return locationB; }
    public int getUses() { return uses; }
    public void setUses(int uses) { this.uses = uses; recalcTier(); }
    public Tier getTier() { return tier; }
    public void setTier(Tier tier) { this.tier = tier; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getLastUsed() { return lastUsed; }
    public void setLastUsed(Instant t) { lastUsed = t; }
    public boolean isInsured() { return insured; }
    public void setInsured(boolean insured) { this.insured = insured; }
    public double getInsurancePool() { return insurancePool; }
    public void setInsurancePool(double pool) { insurancePool = pool; }
    public Instant getBrokenAt() { return brokenAt; }
    public void setBrokenAt(Instant t) { brokenAt = t; }
    public void setLocationA(Location l) { locationA = l; }
    public void setLocationB(Location l) { locationB = l; }
}
