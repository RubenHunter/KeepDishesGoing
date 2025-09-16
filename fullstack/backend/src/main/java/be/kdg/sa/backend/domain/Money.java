package be.kdg.sa.backend.domain;

import lombok.*;

import java.math.BigDecimal;
import java.util.Currency;

@Getter
@AllArgsConstructor
@ToString
public class Money {
    private Currency currency;
    private BigDecimal amount;
}
