package de.netpacket.loadbalancer.network.connections;

import com.google.common.collect.Queues;
import de.netpacket.loadbalancer.LoadBalancer;
import de.netpacket.loadbalancer.network.data.HandshakeData;
import de.netpacket.loadbalancer.network.connections.enums.ConnectionType;
import de.netpacket.loadbalancer.units.PacketUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Queue;

/**
 * The type Client connection.
 */
public class ClientConnection extends SimpleChannelInboundHandler<ByteBuf> {

    private Queue<byte[]> queuedPackets = Queues.newArrayDeque();
    private ConnectionType state = ConnectionType.DEFAULT;

    private ServerConnection serverConnection;
    private HandshakeData handshake;
    private Channel channel;
    private String username;

    private final LoadBalancer loadBalancer;

    public ClientConnection(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public boolean isChannelOpen() {
        return this.channel != null && this.channel.isOpen() && this.state != ConnectionType.DISCONNECTED;
    }

    public void closeChannel() {
        if (this.channel != null && this.channel.isOpen()) {
            this.channel.config().setAutoRead(false);
            this.channel.close();
        }
        this.state = ConnectionType.DISCONNECTED;
        if (this.queuedPackets != null) {
            this.queuedPackets.clear();
            this.queuedPackets = null;
        }
        if (this.serverConnection != null && this.serverConnection.isChannelOpen()) {
            this.serverConnection.closeChannel();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        this.closeChannel();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ClosedChannelException || !this.isChannelOpen()) {
            return;
        }
        this.closeChannel();
    }

    public String getName() {
        return username != null ? username : this.channel.remoteAddress().toString().replace("/", "");
    }

    private void addToQueue(ByteBuf in) {
        if (this.queuedPackets == null) {
            return;
        }
        int readerIndex = in.readerIndex();
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        this.queuedPackets.add(bytes);
        in.readerIndex(readerIndex);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        switch (this.state) {
            case DEFAULT: {
                this.addToQueue(in);
                int packetId = PacketUtils.readVarInt(in);
                if (packetId != 0) {
                    throw new IOException("Invalid packet id received: " + packetId + ", connecting state");
                }
                this.handshake = new HandshakeData();
                this.handshake.read(in);
                this.state = ConnectionType.HANDSHAKE_RECEIVED;
                break;
            }
            case HANDSHAKE_RECEIVED: {
                this.addToQueue(in);
                int packetId = PacketUtils.readVarInt(in);
                if (packetId != 0) {
                    throw new IOException("Invalid packet id received: " + packetId + ", handshake received state");
                }
                if (this.handshake.getNextState() == 2) {
                    this.username = PacketUtils.readString(in);
                }
                this.connectToServer();
                break;
            }
            case CONNECTING: {
                this.addToQueue(in);
                break;
            }
            case CONNECTED: {
                in.retain();
                this.serverConnection.fastWrite(in);
            }
        }
    }
    protected void onServerConnected() {
        if (!this.isChannelOpen()) {
            this.serverConnection.closeChannel();
            return;
        }
        if (this.state != ConnectionType.CONNECTING) {
            throw new RuntimeException("Invalid state: " + this.state);
        }
        this.channel.pipeline().remove("splitter");
        if (this.queuedPackets != null) {
            for (byte[] packet : this.queuedPackets) {
                int length = packet.length;
                int size = PacketUtils.getVarIntLength(length) + length;
                ByteBuf buf = Unpooled.buffer(size, size);
                PacketUtils.writeVarInt(buf, length);
                buf.writeBytes(packet);
                this.serverConnection.fastWrite(buf);
            }
            this.queuedPackets.clear();
            this.queuedPackets = null;
        }
        this.state = ConnectionType.CONNECTED;
        this.channel.config().setAutoRead(true);
    }

    protected void fastWrite(ByteBuf buf) {
        this.channel.writeAndFlush(buf);
    }

    public void connectToServer() {
        InetSocketAddress serverData = loadBalancer.getBalancingStrategy().selectTarget(this.getHandshake().getHostname(), this.getHandshake().getPort());
        if (serverData == null) {
            this.closeChannel();
            return;
        }
        this.connectToServer(serverData);
    }

    public void connectToServer(InetSocketAddress server) {
        this.state = ConnectionType.CONNECTING;
        this.channel.config().setAutoRead(false);
        this.serverConnection = new ServerConnection(this);
        Class<? extends SocketChannel> channelClass = Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;
        new Bootstrap().channel(channelClass).option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.IP_TOS, 24)
                .handler(new ServerConnection.ServerConnectionInitializer(this.serverConnection))
                .group(this.channel.eventLoop())
                .remoteAddress(server).connect().addListener((GenericFutureListener<? extends Future<? super Void>>) future -> {
            if (!future.isSuccess()) {
                this.closeChannel();
            }
        });
    }

    public HandshakeData getHandshake() {
        return this.handshake;
    }
}

