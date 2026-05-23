package com.lebhas.creativesaas.generation.queue;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class GenerationQueueDispatcher {

    private final ObjectProvider<GenerationJobQueuePublisher> queuePublisher;

    public GenerationQueueDispatcher(ObjectProvider<GenerationJobQueuePublisher> queuePublisher) {
        this.queuePublisher = queuePublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(GenerationJobQueuedEvent event) {
        queuePublisher.ifAvailable(publisher -> publisher.publish(event));
    }
}
