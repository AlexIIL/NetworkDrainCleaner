/*
 * Copyright (c) 2022 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.mod.util.netclean;

import java.util.List;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.network.PacketContext;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;

import alexiil.mc.lib.net.ActiveConnection;
import alexiil.mc.lib.net.EnumNetSide;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.NetIdBase;
import alexiil.mc.lib.net.NetIdData;
import alexiil.mc.lib.net.ParentNetId;

public class MinecraftBaseConnection extends ActiveConnection {

    public static final ParentNetId BASE_ROOT;
    public static final NetIdData MC_PACKET;

    static {
        BASE_ROOT = new ParentNetId(null, "MinecraftBaseConnection");
        MC_PACKET = BASE_ROOT.idData("standard_minecraft_packet").withLargeSize()
            .setReceiver((buffer, ctx) -> ((MinecraftBaseConnection) ctx.getConnection()).readMcPacket(buffer, ctx));
    }

    public final EnumNetSide side;
    public final NetCleanEncoder encoder;
    public final NetCleanDecoder decoder;

    // Network State
    public List<Object> packetsOut = null;
    public ByteBuf byteBufOut;

    public MinecraftBaseConnection(EnumNetSide side) {
        super(BASE_ROOT);
        this.side = side;
        encoder = new NetCleanEncoder(this);
        decoder = new NetCleanDecoder(this);
    }

    @Override
    protected boolean isDebuggingConnection() {
        // Sadly stacktraces are useless, since packets are only ever sent to us on a netty thread.
        return false;
    }

    @Override
    public PacketContext getMinecraftContext() {
        throw new UnsupportedOperationException("This is not a normal minecraft connection!");
    }

    @Override
    public EnumNetSide getNetSide() {
        return side;
    }

    @Override
    public NetByteBuf allocBuffer() {
        return NetByteBuf.buffer(true);
    }

    @Override
    public NetByteBuf allocBuffer(int initialCapacity) {
        return NetByteBuf.buffer(initialCapacity, true);
    }

    @Override
    protected void sendPacket(NetByteBuf data, int packetId, NetIdBase netId, int priority) {
        byteBufOut.writeBytes(data);
    }

    protected void writeMcPacket(Packet<?> pkt) {
        MC_PACKET.send(this, (buffer, ctx) -> {

            NetworkState networkState = decoder.nettyCtx.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get();

            NetworkSide netSide = side == EnumNetSide.SERVER ? NetworkSide.CLIENTBOUND : NetworkSide.SERVERBOUND;
            Integer packetId = networkState.getPacketId(netSide, pkt);

            if (packetId == null) {
                throw new IllegalArgumentException(
                    "Unregistered packet type " + pkt.getClass() + " for state " + networkState + " when " + side
                );
            }

            buffer.writeMarker("MC Packet ID");
            buffer.writeVarInt(packetId);
            buffer.writeMarker("MC Payload Length");
            int wi = buffer.writerIndex();
            buffer.writeInt(0); // We come back to modify this after writing
            buffer.writeMarker("MC Packet Payload");
            int start = buffer.writerIndex();
            pkt.write(buffer);
            int end = buffer.writerIndex();
            int len = end - start;
            buffer.setInt(wi, len);
        });
    }

    protected void readMcPacket(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        buffer.readMarker("MC Packet ID");
        int type = buffer.readVarInt();
        buffer.readMarker("MC Payload Length");
        int len = buffer.readInt();
        buffer.readMarker("MC Packet Payload");

        NetworkState networkState = decoder.nettyCtx.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get();

        NetworkSide netSide = side == EnumNetSide.CLIENT ? NetworkSide.CLIENTBOUND : NetworkSide.SERVERBOUND;
        packetsOut.add(networkState.getPacketHandler(netSide, type, buffer));
    }
}
