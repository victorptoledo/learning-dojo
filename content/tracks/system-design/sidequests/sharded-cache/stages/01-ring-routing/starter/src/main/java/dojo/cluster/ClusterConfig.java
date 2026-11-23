package dojo.cluster;

import java.time.Duration;

/**
 * The numbers of the design.
 *
 * @param virtualNodes ring points per unit of weight (stage 1)
 * @param hotWindow the length of a counting window for hot keys (stage 3)
 * @param hotThreshold reads in one window that make a key hot (stage 3)
 * @param hotReplicas how many nodes hold a hot key, the owner included (stage 3)
 * @param failureThreshold failures in a row that eject a node (stage 4)
 * @param ejectFor how long an ejected node gets no calls (stage 4)
 * @param originConcurrency database loads allowed at once (stage 4)
 */
public record ClusterConfig(
        int virtualNodes,
        Duration hotWindow,
        int hotThreshold,
        int hotReplicas,
        int failureThreshold,
        Duration ejectFor,
        int originConcurrency) {
}
