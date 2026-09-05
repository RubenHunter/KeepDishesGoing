package be.kdg.backend.domain.shared;

/**
 * Positive integer quantity. Invariant enforced at construction.
 */
public record Quantity(int value) {
    public Quantity {
        if (value <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0, got " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity plus(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    public Quantity minus(Quantity other) {
        int result = this.value - other.value;
        if (result <= 0) {
            throw new IllegalArgumentException("Resulting quantity must be > 0");
        }
        return new Quantity(result);
    }
}