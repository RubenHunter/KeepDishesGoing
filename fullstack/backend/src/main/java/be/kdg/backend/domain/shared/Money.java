package be.kdg.backend.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable money value object. Domain only — no JPA annotations.
 */
public final class Money {
    public static final Money ZERO = new Money(BigDecimal.ZERO, "EUR");

    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        if (amount == null) throw new IllegalArgumentException("amount must not be null");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("currency must not be blank");
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Money ofEuros(double amount) {
        return new Money(BigDecimal.valueOf(amount), "EUR");
    }

    public static Money ofEuros(BigDecimal amount) {
        return new Money(amount, "EUR");
    }

    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), currency);
    }

    public Money multiply(int factor) {
        if (factor < 0) throw new IllegalArgumentException("factor must be >= 0");
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    public boolean isPositive() { return amount.compareTo(BigDecimal.ZERO) > 0; }
    public boolean isZero()    { return amount.compareTo(BigDecimal.ZERO) == 0; }

    public boolean isPositive(BigDecimal referenceAmount) { return referenceAmount != null && referenceAmount.compareTo(BigDecimal.ZERO) > 0; }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot combine money of different currencies: " + currency + " vs " + other.currency);
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override public int hashCode() { return Objects.hash(amount, currency); }

    @Override public String toString() { return amount + " " + currency; }
}