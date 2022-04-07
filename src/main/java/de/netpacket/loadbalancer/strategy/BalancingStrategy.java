package de.netpacket.loadbalancer.strategy;

import java.net.InetSocketAddress;

public abstract class BalancingStrategy {

    public abstract InetSocketAddress selectTarget(String host, int port);

}

