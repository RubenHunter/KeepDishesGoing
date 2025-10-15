package be.kdg.sa.backend.domain.Shared;

import lombok.Value;
import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;
import java.util.Objects;

@ValueObject
@Value
public class Money {
    public static final Money ZERO = new Money(BigDecimal.ZERO, "EUR");

    BigDecimal amount;
    String currency;

    private Money(BigDecimal amount, String currency) {
        if (amount == null || currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Amount and currency cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative");
        }
        // Gebruik setScale zonder rounding mode, of gebruik een andere approach
        this.amount = amount.setScale(2);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(double amount, String currency) {
        BigDecimal decimalAmount = BigDecimal.valueOf(amount);
        decimalAmount = decimalAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        return new Money(decimalAmount, currency);
    }

    public static Money ofEuros(double amount) {
        BigDecimal decimalAmount = BigDecimal.valueOf(amount);
        decimalAmount = decimalAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        return new Money(decimalAmount, "EUR");
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        BigDecimal newAmount = this.amount.add(other.amount);
        newAmount = newAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        return new Money(newAmount, this.currency);
    }

    public Money multiply(int multiplier) {
        BigDecimal newAmount = this.amount.multiply(BigDecimal.valueOf(multiplier));
        newAmount = newAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        return new Money(newAmount, this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        BigDecimal newAmount = this.amount.multiply(multiplier);
        newAmount = newAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        return new Money(newAmount, this.currency);
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
}