package dojo.broker;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/** A consumer that applies the effects of each message exactly once. See README.md. */
public final class Processor {
    private final Broker broker;
    private final String group;
    private final String topic;
    private final String member;
    private final Store store;
    private final Handler handler;
    private int seenGeneration = -1;

    public Processor(Broker broker, String group, String topic, String member, Store store, Handler handler) {
        this.broker = broker;
        this.group = group;
        this.topic = topic;
        this.member = member;
        this.store = store;
        this.handler = handler;
    }

    public int runOnce(int max) {
        broker.join(group, topic, member);
        int generation = broker.generation(group);
        if (generation != seenGeneration) {
            for (var owned : broker.assignment(group).entrySet()) {
                if (owned.getValue().equals(member)) {
                    Optional<String> stored = store.get(offsetKey(owned.getKey()));
                    if (stored.isPresent()) {
                        broker.seek(group, member, owned.getKey(), Long.parseLong(stored.get()));
                    }
                }
            }
            seenGeneration = generation;
        }
        Batch batch = broker.poll(group, member, max);
        int applied = 0;
        Map<Integer, Long> next = new HashMap<>();
        for (Message message : batch.messages()) {
            String key = offsetKey(message.partition());
            long done = store.get(key).map(Long::parseLong).orElse(-1L);
            next.merge(message.partition(), message.offset() + 1, Math::max);
            if (message.offset() < done) {
                continue;
            }
            if (handle(message, key)) {
                applied++;
            }
        }
        for (var partition : next.entrySet()) {
            try {
                broker.commit(group, member, batch.generation(), partition.getKey(), partition.getValue());
            } catch (CommitFailedException e) {
                // a rebalance happened: the store has the truth, the next owner starts from it
            }
        }
        return applied;
    }

    /** Tries the handler up to maxAttempts times; a message that keeps failing becomes a dead letter. */
    private boolean handle(Message message, String key) {
        int attempts = broker.config().maxAttempts();
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                Map<String, String> writes = new HashMap<>(handler.handle(message, store));
                writes.put(key, Long.toString(message.offset() + 1));
                store.apply(writes);
                return true;
            } catch (RuntimeException failed) {
                if (attempt == attempts) {
                    deadLetter(message);
                }
            }
        }
        store.apply(Map.of(key, Long.toString(message.offset() + 1)));
        return false;
    }

    private void deadLetter(Message message) {
        String dlq = topic + ".dlq";
        try {
            broker.partitions(dlq);
        } catch (NoSuchElementException missing) {
            broker.createTopic(dlq, broker.partitions(topic), broker.retention(topic));
        }
        broker.send(dlq, message.key(), message.value());
    }

    private String offsetKey(int partition) {
        return "offset:" + topic + ":" + partition;
    }
}
