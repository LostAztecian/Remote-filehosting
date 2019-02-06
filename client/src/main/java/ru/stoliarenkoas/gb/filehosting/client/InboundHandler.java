package ru.stoliarenkoas.gb.filehosting.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.stoliarenkoas.gb.filehosting.common.MessageType;

public class InboundHandler extends ChannelInboundHandlerAdapter {
    private static final MessageType[] MESSAGE_TYPES = MessageType.values();
    private final MessageReceiver messageReceiver;

    private MessageType stage = null;
    private boolean stageFinished = false;

    public InboundHandler() {
        super();
        messageReceiver = new MessageReceiver(Connection.getInstance());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) return;
        ByteBuf byteBuf = (ByteBuf)msg;

        if (stage == null) {
            byte typeByte = byteBuf.readByte();
            stage = MESSAGE_TYPES[typeByte];
            System.out.printf("Type byte received: %d --> Type is %s%n", typeByte, stage);
        }

        switch (stage) {
            case HANDSHAKE_RESPONSE: {
                stageFinished = messageReceiver.handshake(ctx, byteBuf);
                break;
            }
            case LOGIN_RESPONSE: {
                stageFinished = messageReceiver.login(ctx, byteBuf);
                break;
            }
            case LOGOUT_RESPONSE: {
                stageFinished = messageReceiver.logout(ctx, byteBuf);
                break;
            }
            case REGISTER_RESPONSE: {
                stageFinished = messageReceiver.register(ctx, byteBuf);
                break;
            }
            case GET_FILE_LIST_RESPONSE: {
                stageFinished = messageReceiver.fileList(ctx, byteBuf);
                break;
            }
            case LOCATION_CHANGE_RESPONSE: {
                stageFinished = messageReceiver.locationChange(ctx, byteBuf);
                break;
            }
            case FILE_UPLOAD_RESPONSE: {
                stageFinished = messageReceiver.fileUpload(ctx, byteBuf);
                break;
            }
            case FILE_DOWNLOAD_RESPONSE: {
                stageFinished = messageReceiver.fileDownload(ctx, byteBuf);
                break;
            }
            case FILE_DELETE_RESPONSE: {
                stageFinished = messageReceiver.fileRemove(ctx, byteBuf);
                break;
            }
            case FILE_RENAME_RESPONSE: {
                stageFinished = messageReceiver.fileRename(ctx, byteBuf);
                break;
            }

        }

        if (stageFinished) {
            stage = null;
            stageFinished = false;
        }

    }

}
