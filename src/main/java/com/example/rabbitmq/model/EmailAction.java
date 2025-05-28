package com.example.rabbitmq.model;

public enum EmailAction {
    MOVE("Move"),

    TRASH("Trash");

    private final String value;

    EmailAction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EmailAction fromString(String text) {
        for (EmailAction action : EmailAction.values()) {
            if (action.value.equalsIgnoreCase(text)) {
                return action;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
