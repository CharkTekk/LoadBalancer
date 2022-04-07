package de.netpacket.loadbalancer.network.data;

import de.netpacket.loadbalancer.units.PacketUtils;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class HandshakeData {

    private int protocolVersion;
    private String hostname;
    private int port;
    private int nextState;

    public void read(ByteBuf buf) throws IOException {
        this.protocolVersion = PacketUtils.readVarInt(buf);
        this.hostname = PacketUtils.readString(buf);
        this.port = buf.readUnsignedShort();
        this.nextState = PacketUtils.readVarInt(buf);
        if (this.hostname.endsWith("FML\u0000")) {
            this.hostname = this.hostname.substring(0, this.hostname.length() - 4);
        }
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getHostname() {
        return this.hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getNextState() {
        return this.nextState;
    }

    public void setNextState(int nextState) {
        this.nextState = nextState;
    }

    protected boolean canEqual(Object other) {
        return other instanceof HandshakeData;
    }

    public int hashCode() {
        int result = 1;
        result = result * 59 + this.getProtocolVersion();
        result = result * 59 + this.getPort();
        result = result * 59 + this.getNextState();
        String $hostname = this.getHostname();
        result = result * 59 + ($hostname == null ? 43 : $hostname.hashCode());
        return result;
    }

    public String toString() {
        return "HandshakeData(protocolVersion=" + this.getProtocolVersion() + ", hostname=" + this.getHostname() + ", port=" + this.getPort() + ", nextState=" + this.getNextState() + ")";
    }
}

