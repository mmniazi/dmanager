/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import Controllers.layoutController;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author muhammad
 */
public class TotalSpeedCalc {

    private static final TotalSpeedCalc instance = new TotalSpeedCalc();
    private final AtomicInteger speed;
    private final AtomicInteger counter;
    private final AtomicInteger totalNo;
    private layoutController controller;

    private TotalSpeedCalc() {
        this.counter = new AtomicInteger(0);
        this.speed = new AtomicInteger(0);
        this.totalNo = new AtomicInteger(0);
    }

    public void setController(layoutController controller) {
        this.controller = controller;
    }

    public static TotalSpeedCalc getInstance() {
        return instance;
    }

    public void updateTotalNo(int size) {
        this.totalNo.set(size);
    }

    public void updateTotalSpeed(float newSpeed) {
        this.speed.addAndGet((int) newSpeed);
        if (this.counter.incrementAndGet() == totalNo.get()) {
            controller.updateTotalSpeed(speed.get());
            this.counter.set(0);
            this.speed.set(0);
        }

    }
}
