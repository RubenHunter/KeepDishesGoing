package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;

@ValueObject
public record Price(BigDecimal amount, String currency) {
    public Price {
        validate(amount, currency);
    }

    public void validate(BigDecimal amount, String currency) {
        if (amount == null || !isPositive(amount)) {
            throw new IllegalArgumentException("Amount cannot be null or negative");
        }
        if (currency == null || currency.isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        // other validation here
    }

    public boolean isPositive(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public Price multiply(int quantity) {
        return new Price(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }
}
