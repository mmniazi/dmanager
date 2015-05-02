/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import Controllers.layoutController;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author muhammad
 */
public class TotalSpeedCalc {

    private static final TotalSpeedCalc instance = new TotalSpeedCalc();
    private final AtomicInteger speed;
    private final AtomicLong counter;
    private final AtomicInteger active;
    private layoutController controller;

    private TotalSpeedCalc() {
        counter = new AtomicLong(0);
        speed = new AtomicInteger(0);
        active = new AtomicInteger(0);
    }

    public static TotalSpeedCalc getInstance() {
        return instance;
    }

    public void setController(layoutController controller) {
        this.controller = controller;
    }

    public void updateActiveDownloads(int activeDownloads) {
        this.active.set(activeDownloads);
        if (this.active.get() == 0) controller.updateTotalSpeed(0);
    }

    public void updateTotalSpeed(float newSpeed) {
        this.speed.addAndGet((int) newSpeed);
        if (active.get() != 0 && (counter.incrementAndGet() % active.get()) == 0) {
            controller.updateTotalSpeed(speed.get());
            speed.set(0);
        }
    }
}
