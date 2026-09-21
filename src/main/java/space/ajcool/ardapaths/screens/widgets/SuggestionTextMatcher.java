package space.ajcool.ardapaths.screens.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Pure text helpers for suggestion-backed input widgets.
 */
public final class SuggestionTextMatcher {
    /**
     * Dimension identifier that should be listed first when available.
     */
    private static final String OVERWORLD = "minecraft:overworld";

    /**
     * Prevents instantiation of this utility class.
     */
    private SuggestionTextMatcher() {
    }

    /**
     * Orders resource suggestions with the overworld first and all other identifiers sorted and deduplicated.
     *
     * @param suggestions raw suggestion values
     * @return ordered suggestion values
     */
    public static List<String> orderDimensions(List<String> suggestions) {
        List<String> ordered = new ArrayList<>();
        addUnique(ordered, OVERWORLD);
        suggestions.stream()
                .filter(Objects::nonNull)
                .filter(suggestion -> !suggestion.isBlank())
                .map(String::trim)
                .sorted()
                .forEach(suggestion -> addUnique(ordered, suggestion));
        return ordered;
    }

    /**
     * Filters suggestions by case-insensitive containment.
     *
     * @param suggestions available suggestion values
     * @param query       current input text
     * @return matching suggestion values
     */
    public static List<String> filterContains(List<String> suggestions, String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return new ArrayList<>(suggestions);
        }
        return suggestions.stream()
                .filter(suggestion -> normalize(suggestion).contains(normalizedQuery))
                .toList();
    }

    /**
     * Checks whether a suggestion exactly matches an input value, ignoring case.
     *
     * @param suggestions available suggestion values
     * @param value       input value
     * @return true when the input value matches a suggestion
     */
    public static boolean containsExactIgnoreCase(List<String> suggestions, String value) {
        return suggestions.stream().anyMatch(suggestion -> suggestion.equalsIgnoreCase(value == null ? "" : value.trim()));
    }

    /**
     * Adds a value when no existing entry matches it case-insensitively.
     *
     * @param values mutable value list
     * @param value  value to add
     */
    private static void addUnique(List<String> values, String value) {
        if (!containsExactIgnoreCase(values, value)) {
            values.add(value);
        }
    }

    /**
     * Normalizes user-facing text for case-insensitive matching.
     *
     * @param value raw text
     * @return normalized text
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
