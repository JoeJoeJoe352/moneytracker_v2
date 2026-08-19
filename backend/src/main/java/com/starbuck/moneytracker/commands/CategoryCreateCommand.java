package com.starbuck.moneytracker.commands;

public class CategoryCreateCommand {
    private final String name;

    public CategoryCreateCommand(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
