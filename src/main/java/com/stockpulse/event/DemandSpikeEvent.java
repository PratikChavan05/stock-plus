package com.stockpulse.event;

import org.springframework.context.ApplicationEvent;

public class DemandSpikeEvent extends ApplicationEvent {
    private final String productId;

    public DemandSpikeEvent(Object source, String productId) {
        super(source);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
