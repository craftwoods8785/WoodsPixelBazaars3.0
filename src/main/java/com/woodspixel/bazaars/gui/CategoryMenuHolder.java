package com.woodspixel.bazaars.gui;

public class CategoryMenuHolder extends BazaarHolder {

    private final String categoryKey;

    public CategoryMenuHolder(String categoryKey) {
        this.categoryKey = categoryKey;
    }

    public String categoryKey() {
        return categoryKey;
    }
}
