package com.shqms.dsa.queue;

import com.shqms.entity.Appointment;
import com.shqms.enums.PriorityType;

import java.util.PriorityQueue;

public class QueueManager {

    private final PriorityQueue<Appointment> queue =
            new PriorityQueue<>((a1, a2) -> {

                if (a1.getPriority() ==
                        PriorityType.EMERGENCY &&
                        a2.getPriority() ==
                                PriorityType.NORMAL) {
                    return -1;
                }

                if (a1.getPriority() ==
                        PriorityType.NORMAL &&
                        a2.getPriority() ==
                                PriorityType.EMERGENCY) {
                    return 1;
                }

                return a1.getTokenNumber()
                        .compareTo(a2.getTokenNumber());
            });

    public void add(Appointment appointment) {
        queue.offer(appointment);
    }

    public Appointment poll() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}