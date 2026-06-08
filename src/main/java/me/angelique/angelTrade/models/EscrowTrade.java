package me.angelique.angelTrade.models;

import java.util.UUID;

public class EscrowTrade {

    public enum Status { PENDING, DEPOSITED, COMPLETED, CANCELLED }

    private final String id;
    private final UUID senderUUID;
    private final UUID receiverUUID;
    private final String itemType;
    private final int quantity;
    private final double price;
    private Status status;
    private long createdAt;
    private long expiresAt;

    public EscrowTrade(String id, UUID sender, UUID receiver, String itemType, int quantity, double price) {
        this.id = id;
        this.senderUUID = sender;
        this.receiverUUID = receiver;
        this.itemType = itemType;
        this.quantity = quantity;
        this.price = price;
        this.status = Status.PENDING;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = System.currentTimeMillis() + 86400000L * 3; // 3 days
    }

    public String getId() { return id; }
    public UUID getSenderUUID() { return senderUUID; }
    public UUID getReceiverUUID() { return receiverUUID; }
    public String getItemType() { return itemType; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isExpired() { return System.currentTimeMillis() > expiresAt && status == Status.PENDING; }
}
