package be.kdg.backend.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Immutable money value object — domain only. */
public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (currency.isBlank()) throw new IllegalArgumentException("currency must not be blank");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money ofEuros(double v) { return new Money(BigDecimal.valueOf(v), "EUR"); }
    public static Money ofEuros(BigDecimal v) { return new Money(v, "EUR"); }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }
    public Money multiply(int factor) {
        if (factor < 0) throw new IllegalArgumentException("factor must be >= 0");
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }
    public boolean isPositive() { return amount.compareTo(BigDecimal.ZERO) > 0; }
    public boolean isZero()     { return amount.compareTo(BigDecimal.ZERO) == 0; }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot combine different currencies: " + currency + " / " + other.currency);
        }
    }
}