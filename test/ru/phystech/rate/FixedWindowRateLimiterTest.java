package ru.phystech.rate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;


public class FixedWindowRateLimiterTest {
    @Test
    public void allow_returnTrue_whenLimitIsNotExceeded_singleThread() {
        RateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        List<Boolean> results =  new ArrayList<>();
        results.add(limiter.allow());
        results.add(limiter.allow());
        results.add(limiter.allow());
        doSleepMillis(100);
        results.add(limiter.allow());
        results.add(limiter.allow());
        results.add(limiter.allow());
        doSleepMillis(100);
        results.add(limiter.allow());
        results.add(limiter.allow());
        results.add(limiter.allow());
        for (Boolean result: results) {
            Assert.assertTrue(result);
        }
    }

    @Test
    public void allow_returnFalse_whenLimitIsExceeded_singleThread() {
        RateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        List<Boolean> results = new ArrayList<>();
        limiter.allow();
        limiter.allow();
        limiter.allow();
        results.add(limiter.allow());
        results.add(limiter.allow());
        doSleepMillis(100);
        limiter.allow();
        limiter.allow();
        limiter.allow();
        results.add(limiter.allow());
        results.add(limiter.allow());
        doSleepMillis(100);
        limiter.allow();
        limiter.allow();
        limiter.allow();
        results.add(limiter.allow());
        results.add(limiter.allow());
        for (Boolean result: results) {
            Assert.assertFalse(result);
        }
    }

    @Test
    public void allow_returnTrue_whenLimitIsNotExceeded_concurrentThreads() {
        RateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        Collection<Boolean> results = new ConcurrentLinkedDeque<>();
        Runnable task = () -> {
            results.add(limiter.allow());
            doSleepMillis(100);
            results.add(limiter.allow());
            doSleepMillis(100);
            results.add(limiter.allow());
        };
        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);
        Thread thread3 = new Thread(task);
        thread1.start();
        thread2.start();
        thread3.start();
        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Boolean result: results) {
            Assert.assertTrue(result);
        }
    }

    @Test
    public void allow_returnFalse_whenLimitIsExceeded_concurrentThreads() {
        RateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        Collection<Boolean> results = new ConcurrentLinkedDeque<>();
        Runnable task = () -> {
            results.add(limiter.allow());
            results.add(limiter.allow());
            doSleepMillis(100);
            results.add(limiter.allow());
            results.add(limiter.allow());
            doSleepMillis(100);
            results.add(limiter.allow());
            results.add(limiter.allow());
        };
        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);
        Thread thread3 = new Thread(task);
        thread1.start();
        thread2.start();
        thread3.start();
        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int success = 0;
        int fail = 0;
        for (Boolean result: results) {
            if (result) success++;
            else fail++;
        }
        Assert.assertEquals(success, 9);
        Assert.assertEquals(fail, 9);
    }

    @Test
    public void cleanWindows_deleteAllOldWindows() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        limiter.allow();
        doSleepMillis(100);
        limiter.allow();
        doSleepMillis(100);
        limiter.allow();
        doSleepMillis(100);
        limiter.cleanWindows();
        Assert.assertEquals(limiter.windows.size(), 0);
    }

    @Test
    public void cleanWindows_deleteAllOldWindows_dontDeleteCurWindow() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        limiter.allow();
        doSleepMillis(100);
        limiter.allow();
        doSleepMillis(100);
        limiter.allow();
        doSleepMillis(100);
        limiter.allow();
        limiter.cleanWindows();
        Assert.assertEquals(limiter.windows.size(), 1);
    }

    static void doSleepMillis(int millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}