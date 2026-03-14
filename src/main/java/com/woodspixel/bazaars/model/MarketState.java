package com.woodspixel.bazaars.model;

import java.util.ArrayDeque;
import java.util.Deque;

public class MarketState {

    private final double basePrice;
    private double multiplier;
    private final Deque<Double> priceHistory;

    public MarketState(double basePrice, double multiplier, Deque<Double> priceHistory) {
        this.basePrice = basePrice;
        this.multiplier = multiplier;
        this.priceHistory = priceHistory;
    }

    public double basePrice() {
        return basePrice;
    }

    public double multiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public Deque<Double> priceHistory() {
        return priceHistory;
    }

    public void pushHistory(double value, int maxSize) {
        priceHistory.addLast(value);
        while (priceHistory.size() > maxSize) {
            priceHistory.removeFirst();
        }
    }

    public static MarketState fresh(double basePrice) {
        return new MarketState(basePrice, 1.0D, new ArrayDeque<>());
    }
}
