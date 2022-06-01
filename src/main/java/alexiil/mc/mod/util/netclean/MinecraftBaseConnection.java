package alexiil.mc.mod.util.netclean;

import java.util.List;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.network.PacketContext;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.DecoderHandler;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.SizePrepender;
import net.minecraft.network.SplitterHandler;

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
        MC_PACKET = BASE_ROOT.idData("standard_minecraft_packet")
            .setReceiver((buffer, ctx) -> ((MinecraftBaseConnection) ctx.getConnection()).readMcPacket(buffer, ctx));
    }

    public final EnumNetSide side;
    public final NetCleanMessageHandler messageHandler;

    // Network State
    public List<Object> packetsOut = null;
    public ByteBuf byteBufOut;

    public MinecraftBaseConnection(EnumNetSide side) {
        super(BASE_ROOT);
        this.side = side;
        this.messageHandler = new NetCleanMessageHandler(this);
    }

    private void initMcConnection() {
        // ReadTimeoutHandler rth = null; // Ignored
        // LegacyQueryHandler lqh = null; // Removed

        // Decoding
        SplitterHandler sh = null;
        DecoderHandler dh = null;

        // Encoding
        SizePrepender sp = null;
        PacketEncoder pe = null;
    }

    @Override
    public PacketContext getMinecraftContext() {
        throw new UnsupportedOperationException("This is not a normal minecraft connection!");
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

            NetworkState networkState
                = messageHandler.nettyCtx.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get();

            NetworkSide netSide = side == EnumNetSide.SERVER ? NetworkSide.CLIENTBOUND : NetworkSide.SERVERBOUND;
            Integer packetId = networkState.getPacketId(netSide, pkt);

            if (packetId == null) {
                throw new IllegalArgumentException(
                    "Unregistered packet type " + pkt.getClass() + " for state " + networkState + " when " + side
                );
            }

            buffer.writeMarker("Packet ID");
            buffer.writeVarInt(packetId);
            buffer.writeMarker("Packet Payload");
            pkt.write(buffer);
        });
    }

    protected void readMcPacket(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        buffer.readMarker("Packet ID");
        int type = buffer.readVarInt();
        buffer.readMarker("Packet Payload");

        NetworkState networkState
            = messageHandler.nettyCtx.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get();

        NetworkSide netSide = side == EnumNetSide.CLIENT ? NetworkSide.CLIENTBOUND : NetworkSide.SERVERBOUND;
        packetsOut.add(networkState.getPacketHandler(netSide, type, buffer));
    }
}
