package de.netpacket.loadbalancer.network;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InetManager {

    private final List<InetSocketAddress> addressList = Arrays.asList(
            new InetSocketAddress("127.0.0.1", 20000),
            new InetSocketAddress("127.0.0.1", 20001),
            new InetSocketAddress("127.0.0.1", 20002),
            new InetSocketAddress("127.0.0.1", 20003),
            new InetSocketAddress("127.0.0.1", 20004),
            new InetSocketAddress("127.0.0.1", 20005),
            new InetSocketAddress("127.0.0.1", 20006),
            new InetSocketAddress("127.0.0.1", 20007),
            new InetSocketAddress("127.0.0.1", 20008),
            new InetSocketAddress("127.0.0.1", 20009)
    );

    public List<InetSocketAddress> atr(String address, int proxy){
        List<InetSocketAddress> current = new ArrayList<>();
        for (int i = 0; i < proxy; i++) {
            current.add(new InetSocketAddress(address, 20000+i));
        }
        return current;
    }
}
