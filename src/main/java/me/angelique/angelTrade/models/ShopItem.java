package me.angelique.angelTrade.models;

public class ShopItem {

    private final String itemKey;
    private double price;
    private int stock;
    private double discount;

    public ShopItem(String itemKey, double price, int stock) {
        this.itemKey = itemKey;
        this.price = price;
        this.stock = stock;
        this.discount = 0.0;
    }

    public double getEffectivePrice() {
        return price * (1.0 - discount);
    }

    public String getItemKey() { return itemKey; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = Math.max(0, Math.min(1, discount)); }
}
