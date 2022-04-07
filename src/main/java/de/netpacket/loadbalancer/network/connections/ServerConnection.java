package de.netpacket.loadbalancer.network.connections;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.nio.channels.ClosedChannelException;

public class ServerConnection extends SimpleChannelInboundHandler<ByteBuf> {

    private final ClientConnection clientConnection;

    private boolean disconnected = false;
    private Channel channel;

    public ServerConnection(ClientConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    public boolean isChannelOpen() {
        return this.channel != null && this.channel.isOpen() && !this.disconnected;
    }

    public void closeChannel() {
        if (this.channel != null && this.channel.isOpen()) {
            this.channel.config().setAutoRead(false);
            this.channel.close();
        }
        this.disconnected = true;
        if (this.clientConnection.isChannelOpen()) {
            this.clientConnection.closeChannel();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel = ctx.channel();
        this.clientConnection.onServerConnected();
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

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        msg.retain();
        this.clientConnection.fastWrite(msg);
    }

    protected void fastWrite(ByteBuf buf) {
        this.channel.writeAndFlush(buf);
    }

    public static class ServerConnectionInitializer extends ChannelInitializer<Channel> {

        private final ServerConnection serverConnection;

        public ServerConnectionInitializer(ServerConnection serverConnection) {
            this.serverConnection = serverConnection;
        }

        protected void initChannel(Channel channel) {
            channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
            channel.pipeline().addLast("handler", this.serverConnection);
        }
    }
}

