package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;

@ValueObject
public record Price(BigDecimal amount, String currency) {
    public Price {
        validate();
    }

    public void validate() {
        if (amount == null || !isPositive()) {
            throw new IllegalArgumentException("Amount cannot be null or negative");
        }
        if (currency == null || currency.isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        // other validation here
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public Price multiply(int quantity) {
        return new Price(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }
}
