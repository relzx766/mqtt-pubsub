package org.relzx.pubsub.client.processor;

import org.relzx.pubsub.common.entity.ProcessResult;

public class PubRelProcessor extends Processor<ProcessResult> {

    @Override
    public ProcessResult call() throws Exception {
        if (!isCancelled() && !receivedAck.get()) {
            synchronized (receivedAck) {
                receivedAck.wait(timeout);
            }
        }
        return receivedAck.get() ? ProcessResult.SUCCESS : ProcessResult.FAILED;
    }

}
