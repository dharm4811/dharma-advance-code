package org.example.future_callable;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class FutureAndCallable {
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

        Callable<String> task3 = ()-> {
            int sleepTime = 2000;
            System.out.println("Got it, Starting, task3");
            Thread.sleep(sleepTime);

            return "Processed task3 took " + sleepTime + "ms";
        };

        // Use a pool of 2 threads so they run at the same time
        ExecutorService service = Executors.newFixedThreadPool(2); // its a number of thread pool, if there are more than 2 thread then those have to wait in queue (e.g. task3 will wait in queue to finish any of task so that it will get its thread)
        List<Future<String>> futureList = new LinkedList<>();
        futureList.add(service.submit(task1)); // As soon as we submit a task will gets executing in background in separate thread
        futureList.add(service.submit(task2)); // separate Thread

        while(!futureList.get(1).isDone()) { // This in main thread
            System.out.println("task2 is processing.., so once I am free then I'll give my thread to task3 to process");
            Thread.sleep(1000); // wait 1 sec before checking again, This in main thread
        }
        futureList.add(service.submit(task3)); // So we have three total task but thread pool is 2 that means two task will run concurrent and remaining task (i.e. task3) will wait in queue

        // Suppose some processing at main thread for 6s, which future won't interrupt because it runs task in separate threads for concurrency
        Thread.sleep(3000); // This in main thread
        // finish main processing

        // Task 1 will return isDone as false because the main thread was in 3s sleep but task1 needs 5s to complete process in its separate thread and till this point, task1 has completed 3s but still 2s more left to complete
        System.out.println("Is task1 done? " + futureList.get(0).isDone()); // This printing in main thread
        // Task 2 in our case will return isDone as True because Main thread was in 3s sleep but task2 took just 1 sec in its thread to completed
        System.out.println("Is task2 done? " + futureList.get(1).isDone()); // This printing in main thread

        System.out.println("Is task3 done? " + futureList.get(2).isDone()); // This printing in main thread

        for(Future<String> future: futureList) { // Main thread
            // future.get() return result quickly if submitted task is done else it will wait for result and then get result from other thread even if other thread is done already
            // So based on this code, Task1 will call future.get() but it will wait 2s more and then return result But as soon as index 1 means Task2 call future.get() then it return quickly because it was already done.
            String result = future.get();
            System.out.println("Result here: "+ result);
        }
        // Future also allow to cancel a task in middle of its processing, future.cancel(true);
        System.out.println("END");
        service.shutdown();
    }

}

