package de.netpacket.loadbalancer;

import de.netpacket.loadbalancer.network.InetManager;
import de.netpacket.loadbalancer.network.NetworkManager;
import de.netpacket.loadbalancer.strategy.BalancingStrategy;
import de.netpacket.loadbalancer.strategy.impl.RoundRobinBalancingStrategy;
import io.netty.util.ResourceLeakDetector;

import java.net.InetSocketAddress;

public class LoadBalancer {

    private final InetManager inetManager;

    private final BalancingStrategy balancingStrategy;
    private final NetworkManager networkManager;

    public LoadBalancer(final InetSocketAddress address, int porxys) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

        this.inetManager = new InetManager();
        this.balancingStrategy = new RoundRobinBalancingStrategy(inetManager.atr(porxys).toArray(new InetSocketAddress[0]));
        this.networkManager = new NetworkManager(this, address);
        this.networkManager.connect();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                networkManager.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public BalancingStrategy getBalancingStrategy() {
        return balancingStrategy;
    }
}
