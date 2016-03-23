package com.bugsnag;

import java.util.List;
import java.util.concurrent.*;

/**
 * Handles notifications to Bugsnag using a thread pool to manage asynchronous notification
 */
public class NotificationWorker {
    // The time to wait for the notification worker threads to complete before being terminated during shutdown
    final long WORKER_SHUTDOWN_TIMEOUT = TimeUnit.SECONDS.toMillis(1);

    protected ExecutorService notifyPool;

    private Configuration config;

    public NotificationWorker(Configuration configuration) {
        this.config = configuration;

        // Create a thread pool with 2 daemon worker threads that queues up notifications if all threads are in use
        notifyPool = Executors.newFixedThreadPool(2, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("bugsnag-notification-worker");
                t.setDaemon(true);
                return t;
            }
        });

        // Add a shutdown hook to try to gracefully complete all pending error notifications
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                notifyPool.shutdown();
                try {
                    // Attempt to gracefully terminate the currently processing notification threads
                    if (!notifyPool.awaitTermination(WORKER_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                        // The notification threads did not complete in time so force them to stop
                        List<Runnable> droppedNotifications = notifyPool.shutdownNow();
                        int activeNotificationCount = 0;
                        if (notifyPool instanceof ThreadPoolExecutor) {
                            activeNotificationCount = ((ThreadPoolExecutor) notifyPool).getActiveCount();
                        }
                        int unstartedNotificationCount = droppedNotifications != null ? droppedNotifications.size() : 0;
                        int droppedNotificationCount = unstartedNotificationCount + activeNotificationCount;
                        config.logger.warn("Application terminated. " + droppedNotificationCount + " error(s) were not sent to Bugsnag");
                    }
                } catch (InterruptedException e) {
                    config.logger.warn("Application terminated while waiting for errors to be sent to Bugsnag");
                }
            }
        });
    }

    public void notifyAsync(Notification notification) {
        notifyPool.execute(new AsynchronousNotification(config, notification));
    }

    public class AsynchronousNotification implements Runnable {
        private Configuration config;
        private Notification notification;

        public AsynchronousNotification(Configuration config, Notification notification) {
            this.config = config;
            this.notification = notification;
        }

        public void run() {
            notification.deliver();
        }
    }
}
