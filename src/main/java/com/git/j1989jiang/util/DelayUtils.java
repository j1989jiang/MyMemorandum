package com.git.j1989jiang.util;

import java.util.concurrent.TimeUnit;

public class DelayUtils {

    /**
     * 包装了一个延时方法，最大支持小时级的延时
     */
    public static void delay(int delay, TimeUnit timeunit) {
        long realDelay = 0;
        switch (timeunit) {
            case SECONDS:
                realDelay = delay * 1000L;
                break;
            case MINUTES:
                realDelay = (long) delay * 1000 * 60;
                break;
            case HOURS:
                realDelay = (long) delay * 1000 * 60 * 60;
                break;
            default:
                realDelay = delay;
                break;
        }
        try {
            Thread.sleep(realDelay);
        } catch (InterruptedException e) {
        }
    }
}
