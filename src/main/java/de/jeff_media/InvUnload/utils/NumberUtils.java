package de.jeff_media.InvUnload.utils;

public final class NumberUtils {

    private NumberUtils() {
    }

    public static boolean isPositiveInteger(String value) {
        return value != null && !value.isEmpty() && value.chars().allMatch(Character::isDigit);
    }
}
