/*
 * Copyright (c) 2022 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.mod.util.netclean.mixin.mx;

import io.netty.channel.Channel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alexiil.mc.mod.util.netclean.MinecraftBaseConnection;

import alexiil.mc.lib.net.EnumNetSide;

@Mixin(targets = "net.minecraft.server.ServerNetworkIo$1")
public class ServerNetworkIo_1_Mixin {

    @Inject(at = @At("RETURN"), remap = false, method = "initChannel(Lio/netty/channel/Channel;)V")
    private void postInitChannel(Channel channel, CallbackInfo ci) {
        MinecraftBaseConnection con = new MinecraftBaseConnection(EnumNetSide.SERVER);
        channel.pipeline().replace("decoder", "decoder", con.decoder);
        channel.pipeline().replace("encoder", "encoder", con.encoder);
    }
}
