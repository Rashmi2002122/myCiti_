package com.myciti.service;

import com.myciti.repository.ClientProfileRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SlugService {

    private static final Set<String> RESERVED_WORDS = Set.of(
        "admin", "api", "account", "login", "logout", "settings",
        "c", "client", "clients", "me", "profile", "listings",
        "submission", "submissions", "auth", "null", "undefined",
        "root", "app", "help", "terms", "privacy", "about",
        "campaigns", "notifications", "public", "edit", "delete",
        "new", "create", "all", "user", "users"
    );

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-+");

    private final ClientProfileRepository clientProfileRepository;

    public SlugService(ClientProfileRepository clientProfileRepository) {
        this.clientProfileRepository = clientProfileRepository;
    }

    /**
     * Checks if a slug candidate is a blocked reserved word.
     */
    public boolean isReserved(String slug) {
        if (slug == null) return true;
        return RESERVED_WORDS.contains(slug.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Converts a business name or arbitrary text into a URL-safe lowercase slug.
     */
    public String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "client";
        }

        String noWhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = MULTI_HYPHEN.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ROOT);

        // Strip leading and trailing hyphens
        if (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }

        if (slug.isBlank()) {
            return "client";
        }

        return slug;
    }

    /**
     * Generates a unique slug from a business name, appending numeric suffixes if necessary.
     */
    public String generateUniqueSlug(String businessName) {
        String base = slugify(businessName);
        if (isReserved(base)) {
            base = base + "-biz";
        }

        String candidate = base;
        int counter = 1;
        while (clientProfileRepository.existsBySlug(candidate) || isReserved(candidate)) {
            candidate = base + "-" + counter;
            counter++;
        }
        return candidate;
    }
}
