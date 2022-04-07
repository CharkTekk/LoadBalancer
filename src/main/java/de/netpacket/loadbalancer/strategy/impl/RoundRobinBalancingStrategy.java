package de.netpacket.loadbalancer.strategy.impl;

import de.netpacket.loadbalancer.strategy.BalancingStrategy;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinBalancingStrategy extends BalancingStrategy {

    private final AtomicInteger currentTarget = new AtomicInteger(0);
    private final InetSocketAddress[] targets;

    public RoundRobinBalancingStrategy(InetSocketAddress... targets) {
        this.targets = targets;
    }

    @Override
    public synchronized InetSocketAddress selectTarget(String originHost, int originPort) {
        int now = this.currentTarget.getAndIncrement();
        if (now >= this.targets.length) {
            now = 0;
            this.currentTarget.set(0);
        }
        return this.targets[now];
    }
}

