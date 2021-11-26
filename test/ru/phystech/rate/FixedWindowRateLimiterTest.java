package ru.phystech.rate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;


public class FixedWindowRateLimiterTest {
    private final int amountOfIterations = 10;
    private final int amountOfThreads = 10;

    @Test
    public void allow_returnTrue_whenLimitIsNotExceeded_singleThread() {
        RateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        List<Boolean> results =  new ArrayList<>();
        for (int i = 0; i < amountOfIterations; i++) {
            results.add(limiter.allow());
            results.add(limiter.allow());
            results.add(limiter.allow());
            doSleepMillis(100);
        }
        for (Boolean result: results) {
            Assert.assertTrue(result);
        }
    }

    @Test
    public void allow_returnFalse_whenLimitIsExceeded_singleThread() {
        RateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        List<Boolean> results = new ArrayList<>();
        for (int i = 0; i < amountOfIterations; i++) {
            limiter.allow();
            limiter.allow();
            limiter.allow();
            results.add(limiter.allow());
            results.add(limiter.allow());
            doSleepMillis(100);
        }
        for (Boolean result: results) {
            Assert.assertFalse(result);
        }
    }

    @Test
    public void allow_returnTrue_whenLimitIsNotExceeded_concurrentThreads() {
        RateLimiter limiter = new FixedWindowRateLimiter(amountOfThreads, 100);
        CountDownLatch endSignal = new CountDownLatch(amountOfThreads);
        Collection<Boolean> results = new ConcurrentLinkedDeque<>();
        Runnable task = () -> {
            for (int i = 0; i < amountOfIterations; i++) {
                results.add(limiter.allow());
                doSleepMillis(100);
            }
           endSignal.countDown();
        };
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < amountOfThreads; i++) {
            threads.add(new Thread(task));
        }
        threads.forEach(Thread::start);
        try {
            endSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Boolean result: results) {
            Assert.assertTrue(result);
        }
    }

    @Test
    public void allow_returnFalse_whenLimitIsExceeded_concurrentThreads() {
        RateLimiter limiter = new FixedWindowRateLimiter(amountOfThreads, 100);
        CountDownLatch endSignal = new CountDownLatch(amountOfThreads);
        Collection<Boolean> results = new ConcurrentLinkedDeque<>();
        Runnable task = () -> {
            for (int i = 0; i < amountOfIterations; i++) {
                results.add(limiter.allow());
                results.add(limiter.allow());
                doSleepMillis(100);
            }
            endSignal.countDown();
        };
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < amountOfThreads; i++) {
            threads.add(new Thread(task));
        }
        threads.forEach(Thread::start);
        try {
            endSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int success = 0;
        int fail = 0;
        for (Boolean result: results) {
            if (result) success++;
            else fail++;
        }
        Assert.assertEquals(success, amountOfIterations * amountOfThreads);
        Assert.assertEquals(fail, amountOfIterations * amountOfThreads);
    }

    @Test
    public void cleanWindows_deleteAllOldWindows() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        for (int i = 0; i < amountOfIterations; i++) {
            limiter.allow();
            doSleepMillis(100);
        }
        Assert.assertEquals(limiter.windows.size(), 10);
        limiter.cleanWindows();
        Assert.assertEquals(limiter.windows.size(), 0);
    }

    @Test
    public void cleanWindows_deleteAllOldWindows_dontDeleteCurWindow() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        for (int i = 0; i < amountOfIterations; i++) {
            limiter.allow();
            doSleepMillis(100);
        }
        limiter.allow();
        Assert.assertEquals(limiter.windows.size(), 11);
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