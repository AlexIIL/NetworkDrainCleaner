/*
 * Copyright (c) 2022 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.mod.util.netclean;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import net.minecraft.network.Packet;

public class NetCleanEncoder extends MessageToByteEncoder<Packet<?>> {

    public final MinecraftBaseConnection connection;

    public NetCleanEncoder(MinecraftBaseConnection connection) {
        this.connection = connection;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet<?> msg, ByteBuf out) throws Exception {
        try {
            connection.byteBufOut = out;
            connection.writeMcPacket(msg);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            connection.byteBufOut = null;
        }
    }
}
