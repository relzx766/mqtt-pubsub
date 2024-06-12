package org.relzx.pubsub.client.processor;

import lombok.extern.slf4j.Slf4j;
import org.relzx.pubsub.client.properties.ClientProperties;
import org.relzx.pubsub.client.properties.PropertiesFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class Processor<T> implements Callable<T>, RunnableFuture<T> {
    private static final Executor defaultExecutor = Executors.newVirtualThreadPerTaskExecutor();
    protected long timeout;

    protected final AtomicBoolean receivedAck = new AtomicBoolean(false);
   protected ClientProperties clientProperties = PropertiesFactory.getProperties();

    private final FutureTask<T> futureTask;

    public Processor() {
        this.futureTask = new FutureTask<>(this);
    }

    public Processor<T> execute() {
        defaultExecutor.execute(this);
        return this;
    }


    @Override
    public void run() {
        futureTask.run();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return futureTask.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return futureTask.isCancelled();
    }

    @Override
    public boolean isDone() {
        return futureTask.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return futureTask.get();
        }catch (Exception e){
            cancel(true);
            throw e;
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return futureTask.get(timeout, unit);
        }catch (Exception e){
            cancel(true);
            throw e;
        }

    }

}
