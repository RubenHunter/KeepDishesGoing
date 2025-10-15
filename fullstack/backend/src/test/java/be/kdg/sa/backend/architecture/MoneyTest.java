package be.kdg.sa.backend.architecture;

import be.kdg.sa.backend.domain.Shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {
//g
    @Test
    @DisplayName("Should create money with valid amount")
    void createMoney_withValidAmount_shouldSucceed() {
        // When
        Money money = Money.ofEuros(15.50);

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo("15.50");
        assertThat(money.getCurrency()).isEqualTo("EUR");
    }
//g
    @Test
    @DisplayName("Should not create money with negative amount")
    void createMoney_withNegativeAmount_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> Money.ofEuros(-10.00))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }
//g
    @Test
    @DisplayName("Should add money with same currency")
    void addMoney_withSameCurrency_shouldReturnSum() {
        // Given
        Money money1 = Money.ofEuros(10.00);
        Money money2 = Money.ofEuros(5.50);

        // When
        Money result = money1.add(money2);

        // Then
        assertThat(result).isEqualTo(Money.ofEuros(15.50));
    }
//h
    @Test
    @DisplayName("Should not add money with different currency")
    void addMoney_withDifferentCurrency_shouldThrowException() {
        // Given
        Money euros = Money.ofEuros(10.00);
        Money dollars = Money.of(BigDecimal.TEN, "USD");

        // When & Then
        assertThatThrownBy(() -> euros.add(dollars))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }
//g
    @Test
    @DisplayName("Should multiply money correctly")
    void multiplyMoney_shouldReturnCorrectResult() {
        // Given
        Money money = Money.ofEuros(5.00);

        // When
        Money result = money.multiply(3);

        // Then
        assertThat(result).isEqualTo(Money.ofEuros(15.00));
    }
}