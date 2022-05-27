/**
 * Copyright 2016 Freescale Semiconductors, Inc.
 */
package com.example.ota.utils;

public class BaseEvent {

    public final long id;

    public BaseEvent() {
        id = System.nanoTime();
    }
}
