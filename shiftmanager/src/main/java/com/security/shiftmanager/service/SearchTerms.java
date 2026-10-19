package com.security.shiftmanager.service;

final class SearchTerms {

    private SearchTerms() {
    }

    /** Converts a free-text search into a case-insensitive LIKE pattern, or null if blank. */
    static String toLikePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase() + "%";
    }
}
