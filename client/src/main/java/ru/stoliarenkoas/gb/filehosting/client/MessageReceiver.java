package ru.stoliarenkoas.gb.filehosting.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
public class MessageReceiver {
    private final Connection connection;
    private long remainingSize = 0;

    OutputStream os = null;

    public boolean handshake(ChannelHandlerContext ctx, ByteBuf msg) {

        final byte[] msgBytes = new byte[msg.readByte()];
        msg.readBytes(msgBytes);
        msg.release();
        final String message = new String(msgBytes);
        System.out.println("Server greets you: " + message);
        return true;

    }

    public boolean login(ChannelHandlerContext ctx, ByteBuf msg) {

        final byte[] msgBytes = new byte[msg.readByte()];
        msg.readBytes(msgBytes);
        msg.release();
        final String message = new String(msgBytes);
        Platform.runLater(() -> {
            connection.getController().username.setText(message);
            connection.getController().btnLogin.setText("Logout");
        });
        return true;

    }

    public boolean logout(ChannelHandlerContext ctx, ByteBuf msg) {

        Platform.runLater(() -> {
            connection.getController().username.setText("Unauthorized!");
            connection.getController().btnLogin.setText("Login");
        });
        return true;

    }

    public boolean register(ChannelHandlerContext ctx, ByteBuf msg) {

        //put register confirmation here
        msg.release();
        return true;

    }

    public boolean fileList(final ChannelHandlerContext ctx, final ByteBuf msg) {

        if (remainingSize == 0) {
            remainingSize = msg.readByte();
            Platform.runLater(() -> connection.getController().textarea.clear());
        }
        while (remainingSize > 0 && msg.readableBytes() > 0) {
            final byte[] msgBytes = new byte[msg.readByte()];
            msg.readBytes(msgBytes);
            final String filename = new String(msgBytes) + "\n";
            Platform.runLater(() -> {
                connection.getController().textarea.appendText(filename);
            });
            remainingSize--;
        }

        return remainingSize == 0;

    }

    public boolean locationChange(ChannelHandlerContext ctx, ByteBuf msg) {

        final boolean success = msg.readBoolean();
        if (success) {
            final byte[] msgBytes = new byte[msg.readByte()];
            msg.readBytes(msgBytes);
            final String folder = new String(msgBytes);
            System.out.printf("Location changed to: %s", folder);
            connection.getController().currentRemoteFolder = Paths.get(folder);
        }
        msg.release();
        return true;

    }

    public boolean fileUpload(ChannelHandlerContext ctx, ByteBuf msg) {

        //put server response for upload here
        msg.release();
        return true;

    }

    public boolean fileDownload(ChannelHandlerContext ctx, ByteBuf msg) throws IOException{

        //If it is a first msg - get size and open output stream
        if (os == null) {
            System.out.println("Receiving file...");
            remainingSize = msg.readLong();
            //set file name here: connection.getController().currentFile = (Path)
            final Path targetFile = connection.getController().resolveFilepath();
            if (Files.exists(targetFile)) Files.delete(targetFile);
            os = Files.newOutputStream(targetFile);
        }

        final byte[] buf = new byte[msg.readableBytes()];
        msg.readBytes(buf);
        msg.release();
        os.write(buf);
        remainingSize -= buf.length;

        if (remainingSize <= 0) {
            os.close();
            os = null;
            System.out.println("File downloaded!");
            return true;
        }
        return false;

    }

    public boolean fileRemove(ChannelHandlerContext ctx, ByteBuf msg) {

        boolean success = false;
        final boolean exists = msg.readBoolean();
        if (exists) {
            success = msg.readBoolean();
        }
        msg.release();
        System.out.printf("File deleted? - %s", success);
        return true;

    }

    public boolean fileRename(ChannelHandlerContext ctx, ByteBuf msg) {

        boolean success = false;
        final boolean exists = msg.readBoolean();
        if (exists) {
            success = msg.readBoolean();
        }
        msg.release();
        System.out.printf("File renamed? - %s", success);
        return true;

    }

}
