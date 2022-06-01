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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import net.minecraft.network.Packet;

import alexiil.mc.lib.net.InternalMsgUtil;
import alexiil.mc.lib.net.NetByteBuf;

public class NetCleanMessageHandler extends ByteToMessageCodec<Packet<?>> {

    public final MinecraftBaseConnection connection;
    public ChannelHandlerContext nettyCtx;

    public NetCleanMessageHandler(MinecraftBaseConnection connection) {
        this.connection = connection;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.nettyCtx = ctx;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet<?> msg, ByteBuf out) throws Exception {
        try {
            connection.byteBufOut = out;
            connection.writeMcPacket(msg);
        } finally {
            connection.byteBufOut = null;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            connection.packetsOut = out;
            InternalMsgUtil.onReceive(connection, NetByteBuf.asNetByteBuf(in));
        } finally {
            connection.packetsOut = null;
        }
    }
}
