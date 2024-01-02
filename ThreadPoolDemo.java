package temp.cha_2;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class ThreadPool {

    private BlockingQueue<Runnable> taskQueue = null;
    private List<PoolThreadRunnable> runnables = new ArrayList<>();
    private boolean isStopped = false;

    public ThreadPool(int noOfThreads, int maxNoOfTasks) {
        taskQueue = new ArrayBlockingQueue<>(maxNoOfTasks);

        for (int i = 0; i < noOfThreads; i++) {
            PoolThreadRunnable poolThreadRunnable =
                    new PoolThreadRunnable(taskQueue);

            runnables.add(poolThreadRunnable);
            new Thread(poolThreadRunnable).start();
        }
    }

    public synchronized void execute(Runnable task) throws InterruptedException {
        if (this.isStopped) {
            throw new IllegalStateException("ThreadPool is stopped");
        }

        this.taskQueue.offer(task);
    }

    public synchronized void stop() {
        this.isStopped = true;
        for (PoolThreadRunnable runnable : runnables) {
            runnable.doStop();
        }
    }

    public synchronized void waitUntilAllTasksFinished() {
        while (!taskQueue.isEmpty()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

class PoolThreadRunnable implements Runnable {

    private Thread thread = null;
    private BlockingQueue<Runnable> taskQueue = null;
    private volatile boolean isStopped = false;

    public PoolThreadRunnable(BlockingQueue<Runnable> queue) {
        taskQueue = queue;
    }

    public void run() {
        this.thread = Thread.currentThread();
        while (!isStopped()) {
            try {
                Runnable runnable = taskQueue.take();
                runnable.run();
            } catch (InterruptedException e) {
                if (isStopped()) {
                    break;
                }
                // Handle interruption if needed
            } catch (Exception e) {
                // log or otherwise report exception,
                // but keep the pool thread alive.
            }
        }
    }

    public synchronized void doStop() {
        isStopped = true;
        // break pool thread out of dequeue() call.
        this.thread.interrupt();
    }

    public synchronized boolean isStopped() {
        return isStopped;
    }
}

public class ThreadPoolDemo {

    public static void main(String[] args) throws InterruptedException {

        ThreadPool threadPool = new ThreadPool(3, 10);

        for (int i = 0; i < 10; i++) {
            int taskNo = i;
            threadPool.execute(() -> {
                String message =
                        Thread.currentThread().getName()
                                + ": Task " + taskNo;
                System.out.println(message);
            });
        }

        threadPool.waitUntilAllTasksFinished();
        threadPool.stop();
    }
}
