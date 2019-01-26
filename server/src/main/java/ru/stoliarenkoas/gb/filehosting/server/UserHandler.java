package ru.stoliarenkoas.gb.filehosting.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import ru.stoliarenkoas.gb.filehosting.common.MessageType;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class UserHandler {
    private final ByteBufAllocator allocator = new PooledByteBufAllocator();
    private static final Path SERVER_ROOT_FOLDER = Paths.get("Server_files");
    private String username = null;

    private InputStream is = null;
    private OutputStream os = null;

    private Path currentFile = null;
    private long remainingFileSize = 0;

    public boolean login(ChannelHandlerContext ctx, ByteBuf msg) {
        //get login
        final byte[] loginBytes = new byte[msg.readByte()];
        for (int i = 0; i < loginBytes.length; i++) loginBytes[i] = msg.readByte();
        final String login = new String(loginBytes);
        //get password
        final byte[] passwordBytes = new byte[msg.readByte()];
        for (int i = 0; i < passwordBytes.length; i++) passwordBytes[i] = msg.readByte();
        final String password = new String(passwordBytes);

        msg.release();
        System.out.printf("User logged in:%n-login: %s%n-password: %s%n", login, password);

        username = login;
        return true;
    }

    public boolean logout(ChannelHandlerContext ctx, ByteBuf msg) {
        username = null;
        return true;
    }

    public boolean uploadFile(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //If this is a first chunk - set flags and open file-writing stream.
        if (os == null) {
            //get size
            System.out.println("Creating a new upload session...");
            remainingFileSize = msg.readLong();
            System.out.println("Incoming file size: " + remainingFileSize);
            //get name
            final byte[] filenameBytes = new byte[msg.readByte()];
            for (int i = 0; i < filenameBytes.length; i++) {
                filenameBytes[i] = msg.readByte();
            }
            currentFile = SERVER_ROOT_FOLDER.resolve(Paths.get(username).resolve(Paths.get(new String(filenameBytes))));
            System.out.println("Incoming file name: " + currentFile);
            //Create dirs, clear old files and open stream.
            Files.createDirectories(currentFile.getParent());
            if (Files.exists(currentFile)) Files.delete(currentFile);
            os = Files.newOutputStream(currentFile);
        }
        //Write remaining data
        final int toWrite = remainingFileSize > msg.readableBytes() ? msg.readableBytes() : (int)remainingFileSize;
        for (int i = 0; i < toWrite; i++) {
            os.write(msg.readByte());
        }
        msg.release();

        //Check completion and return.
        remainingFileSize -= toWrite;
        System.out.println("Remaining file size is " + remainingFileSize);
        System.out.println("Output file size is " + Files.size(currentFile));
        if (remainingFileSize == 0) {
            os.close();
            os = null;
            currentFile = null;
            System.out.println("Uploading file is finished!");
            return true;
        }
        return false;
    }

    public boolean downloadFile(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //Read download request
        final byte[] filenameBytes = new byte[msg.readByte()];
        for (int i = 0; i < filenameBytes.length; i++) {
            filenameBytes[i] = msg.readByte();
        }

        //If user is not logged in - ignore request.
        if (username == null) return true;

        //Ensure file existence.
        currentFile = SERVER_ROOT_FOLDER.resolve(Paths.get(username)).resolve(Paths.get(new String(filenameBytes)));
        if (Files.notExists(currentFile)) {
            System.out.println("No such file on server");
            return true;
        }

        //Write file meta-data
        remainingFileSize = Files.size(currentFile);
        ByteBuf byteBuf = allocator.buffer(1 + 8);
        byteBuf.writeByte((byte)MessageType.FILE_DOWNLOAD_RESPONSE.ordinal());
        for (int i = 7; i >= 0; i--) {
            byteBuf.writeByte((byte)(remainingFileSize >>> 8 * i));
        }
        ctx.writeAndFlush(byteBuf);

        //Split file and send chunks
        is = Files.newInputStream(currentFile);
        final long fullChunksCount = remainingFileSize / 256;
        if (fullChunksCount > 0) {
            byte[] buf = new byte[256];
            for (int i = 0; i < fullChunksCount; i++) {
                is.read(buf);
                System.out.println("Sending filechunk: " + Arrays.toString(buf));
                byteBuf = allocator.buffer(256);
                byteBuf.writeBytes(buf);
                ctx.writeAndFlush(byteBuf);
            }
        }
        remainingFileSize -= fullChunksCount * 256;

        //Write remainder
        if (remainingFileSize > 0) {
            System.out.println("Sending reminder of " + remainingFileSize + "bytes.");
            byteBuf = allocator.buffer((int)remainingFileSize);
            while (remainingFileSize-- > 0) {
                byteBuf.writeByte(is.read());
            }
            ctx.writeAndFlush(byteBuf);
            System.out.println("File sent.");
        }

        //Close file ans reset flags.
        currentFile = null;
        is.close();
        is = null;
        return true;
    }
}
