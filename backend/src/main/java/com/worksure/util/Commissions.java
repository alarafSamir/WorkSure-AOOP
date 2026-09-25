package com.worksure.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class Commissions {
    private final double rate;

    public Commissions(@Value("${app.platform-commission-rate:0.2}") double rate) {
        this.rate = rate;
    }

    public double rate() {
        return rate;
    }

    public Map<String, Object> split(Object amount) {
        BigDecimal total = new BigDecimal(String.valueOf(amount == null ? "0" : amount));
        BigDecimal commission = total.multiply(BigDecimal.valueOf(rate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal payout = total.subtract(commission).setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("amount", total);
        m.put("platform_commission", commission);
        m.put("worker_payout", payout);
        return m;
    }
}
