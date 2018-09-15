package me.coley.jremapper.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Threads {
	public static ExecutorService pool(int maxSize) {
		return Executors.newFixedThreadPool(maxSize);
	}

	public static void waitForCompletion(ExecutorService pool) {
		try {
			pool.shutdown();
			pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			Logging.error(e);
		}
	}

	public static void run(Runnable r) {
		runLater(0, r);
	}

	public static void runLater(int delay, Runnable r) {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
					r.run();
				} catch (Exception e) {
					Logging.error(e);
				}

			}
		}.start();
	}

	public static void runFx(Runnable r) {
		Threads.runFx(r);
	}

	public static void runLaterFx(int delay, Runnable r) {
		runLater(delay, () -> Threads.runFx(r));
	}
}
