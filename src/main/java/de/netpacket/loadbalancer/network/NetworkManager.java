package de.netpacket.loadbalancer.network;

import de.netpacket.loadbalancer.LoadBalancer;
import de.netpacket.loadbalancer.network.coder.LengthDeserializer;
import de.netpacket.loadbalancer.network.connections.ClientConnection;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.net.InetSocketAddress;

public final class NetworkManager implements AutoCloseable {

    private final EventLoopGroup workerGroup, bossGroup;
    private final ServerBootstrap serverBootstrap;
    private final InetSocketAddress inetAddress;


    public NetworkManager(final LoadBalancer loadBalancer, InetSocketAddress inetAddress) {
        this.inetAddress = inetAddress;
        int cores = Runtime.getRuntime().availableProcessors();

        if (Epoll.isAvailable()) {
            this.workerGroup = new EpollEventLoopGroup(cores * 4);
            this.bossGroup = new EpollEventLoopGroup(cores);
        } else {
            this.workerGroup = new NioEventLoopGroup(cores * 4);
            this.bossGroup = new NioEventLoopGroup(cores);
        }

        Class<? extends ServerSocketChannel> channelClass = Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
        this.serverBootstrap = new ServerBootstrap()
                .group(this.bossGroup, this.workerGroup)
                .channel(channelClass)
                .childOption(ChannelOption.IP_TOS, 24)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childHandler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
                        channel.pipeline().addLast("splitter", new LengthDeserializer());
                        channel.pipeline().addLast("packet_handler", new ClientConnection(loadBalancer));
                    }

                });
    }

    public void connect() {
        ChannelFuture channelFuture = null;
        try {
            channelFuture = this.serverBootstrap.bind(inetAddress.getPort()).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }
}

