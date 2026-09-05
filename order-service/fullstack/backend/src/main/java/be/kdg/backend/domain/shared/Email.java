package be.kdg.backend.domain.shared;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validated email value object. Domain-only.
 */
public record Email(String value) {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email other)) return false;
        return value.equalsIgnoreCase(other.value);
    }

    @Override public int hashCode() { return Objects.hash(value.toLowerCase()); }
}