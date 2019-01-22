package ru.stoliarenkoas.gb.filehosting.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.stoliarenkoas.gb.filehosting.common.Message;

public class ByteToMessageHandler extends ChannelInboundHandlerAdapter {

    private byte stage = 0;
    private byte type = -1;
    private int part;
    private int parts;
    private byte[] payload;
    private short marker;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) return;
        ByteBuf byteBuf = (ByteBuf)msg;

        if (stage == 0) {
            if (byteBuf.readableBytes() < 1) return;
            type = byteBuf.readByte();
            stage++;
        }

        if (stage == 1) {
            if (byteBuf.readableBytes() < 2) return;
            short length = byteBuf.readShort();
            payload = new byte[length];
            marker = 0;
            stage++;
        }

        if (stage == 2) {
            if (byteBuf.readableBytes() < 4) return;
            part = byteBuf.readInt();
            stage++;
        }

        if (stage == 3) {
            if (byteBuf.readableBytes() < 4) return;
            parts = byteBuf.readInt();
            stage++;
        }

        if (stage == 4) {

            int needed = payload.length - marker;
            int ready = byteBuf.readableBytes();
            int toRead = ready > needed ? needed : ready;
            byteBuf.readBytes(payload, marker, toRead);
            marker += toRead;

            if (marker == payload.length) {
                Message message = new Message(type, payload, part, parts);
                stage = 0;
                byteBuf.release();
                ctx.fireChannelRead(message);
            }
        }
    }

}
