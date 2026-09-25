package com.worksure.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StripeAmounts {
    private final String currency;
    private final double bdtPerUsd;

    public StripeAmounts(
            @Value("${stripe.currency:usd}") String currency,
            @Value("${stripe.bdt-per-usd:110}") double bdtPerUsd
    ) {
        this.currency = currency.toLowerCase();
        this.bdtPerUsd = bdtPerUsd;
    }

    public Map<String, Object> toCharge(Object bdtAmount) {
        double amount = 0;
        try {
            amount = Double.parseDouble(String.valueOf(bdtAmount));
        } catch (Exception ignored) {
        }
        Map<String, Object> m = new LinkedHashMap<>();
        if ("bdt".equals(currency)) {
            m.put("currency", "bdt");
            m.put("unitAmount", Math.max(100, Math.round(amount * 100)));
            m.put("displayNote", null);
            return m;
        }
        double usd = amount / bdtPerUsd;
        m.put("currency", "usd");
        m.put("unitAmount", Math.max(50, Math.round(usd * 100)));
        m.put("displayNote", String.format("Test charge ~$%.2f USD (৳%.0f BDT at rate %.0f)", usd, amount, bdtPerUsd));
        return m;
    }
}
