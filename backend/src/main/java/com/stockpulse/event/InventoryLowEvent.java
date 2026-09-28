package com.stockpulse.event;

import org.springframework.context.ApplicationEvent;

public class InventoryLowEvent extends ApplicationEvent {
    private final String productId;

    public InventoryLowEvent(Object source, String productId) {
        super(source);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
