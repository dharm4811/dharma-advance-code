package org.example.future_callable;


import java.util.concurrent.*;

public class ConcurrencyWithExecutor {
    public static void main(String []args) throws ExecutionException, InterruptedException {
        Callable<String> task1 = ()-> {
            int sleepTime = 15000;
            System.out.println("Starting, task1");
            Thread.sleep(sleepTime);

            return "Processed task1 took "+ sleepTime + "ms";
        };
        Callable<String> task2 = ()-> {
            int sleepTime = 2000;
            System.out.println("Starting, task2");
            Thread.sleep(sleepTime);

            return "Processed task2 took " + sleepTime + "ms";
        };


        // Use a pool of 2 threads so they run at the same time
        ExecutorService service = Executors.newFixedThreadPool(2); // its a number of thread pool, if there are more than 2 thread then those have to wait in queue (e.g. task3 will wait in queue to finish any of task so that it will get its thread)
        // Wrap your threadPool in a CompletionService
        ExecutorCompletionService<String> completionService = new ExecutorCompletionService<>(service);

        completionService.submit(task1); // As soon as we submit a task will gets executing in background in separate thread
        completionService.submit(task2); // separate Thread

        // Suppose some processing at main thread for 6s, which future won't interrupt because it runs task in separate threads for concurrency
        Thread.sleep(3000); // This in main thread
        // finish main processing

        for (int i = 0; i < 2; i++) {
            // .take() blocks until ANY task is finished
            // Since Task 2 is faster, it will be "taken" first!
            String result = completionService.take().get(); // which ever task finish first it will retrieve the result and remove from the service so that it won't look same task again
            System.out.println("Received: " + result); // Based on our code, task2 will print first and then task1
        }
        System.out.println("END");
        service.shutdown();
    }
}
