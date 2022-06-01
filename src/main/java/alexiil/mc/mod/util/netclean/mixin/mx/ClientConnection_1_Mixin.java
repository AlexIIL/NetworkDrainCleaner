package alexiil.mc.mod.util.netclean.mixin.mx;

import io.netty.channel.Channel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alexiil.mc.mod.util.netclean.MinecraftBaseConnection;

import alexiil.mc.lib.net.EnumNetSide;

@Mixin(targets = "net.minecraft.network.ClientConnection$1")
public class ClientConnection_1_Mixin {

    @Inject(at = @At("RETURN"), remap = false, method = "initChannel(Lio/netty/channel/Channel;)V")
    private void postInitChannel(Channel channel, CallbackInfo ci) {
        channel.pipeline().remove("decoder");
        channel.pipeline().remove("encoder");

        MinecraftBaseConnection con = new MinecraftBaseConnection(EnumNetSide.CLIENT);
        channel.pipeline().addBefore("packet_handler", "network_drain_cleaner", con.messageHandler);
    }
}
