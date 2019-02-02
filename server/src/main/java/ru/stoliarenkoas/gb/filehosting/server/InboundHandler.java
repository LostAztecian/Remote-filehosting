package ru.stoliarenkoas.gb.filehosting.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.stoliarenkoas.gb.filehosting.common.MessageType;

public class InboundHandler extends ChannelInboundHandlerAdapter {
    private static final MessageType[] MESSAGE_TYPES = MessageType.values();
    private UserHandler handler = new UserHandler();
    private MessageType stage = null;
    private boolean stageFinished = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) return;
        ByteBuf byteBuf = (ByteBuf)msg;

        if (stage == null) {
            byte typeByte = byteBuf.readByte();
            System.out.print("Type byte received: " + typeByte);
            stage = MESSAGE_TYPES[typeByte];
            System.out.println(" --> type is " + stage);
        }

        switch (stage) {
            case HANDSHAKE: {
                break;
            }
            case LOGIN: {
                System.out.println("User login message received.");
                stageFinished = handler.login(ctx, byteBuf);
                break;
            }
            case LOGOUT: {
                System.out.println("User logout message received.");
                stageFinished = handler.logout(ctx, byteBuf);
                break;
            }
            case REGISTER: {
                break;
            }
            case GET_FILE_LIST: {
                break;
            }
            case LOCATION_CHANGE: {
                break;
            }
            case FILE_UPLOAD: {
                System.out.println("File upload message received.");
                stageFinished = handler.uploadFile(ctx, byteBuf);
                break;
            }
            case FILE_DOWNLOAD: {
                System.out.println("File download message received.");
                System.out.println(ctx.name());
                stageFinished = handler.downloadFile(ctx, byteBuf);
                break;
            }
            case FILE_DELETE: {
                break;
            }
            case FILE_RENAME: {
                break;
            }
        }

        if (stageFinished) {
            stage = null;
            stageFinished = false;
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Exception caught: ");
        cause.printStackTrace();
    }
}
