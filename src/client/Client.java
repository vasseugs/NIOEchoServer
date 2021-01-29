package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.Set;

public class Client {

    public static void main(String[] args) {
        try {
            //connecting to server
            SocketChannel socketChannel = SocketChannel.open(
                    new InetSocketAddress("localhost", 20000));
            socketChannel.configureBlocking(false);

            if(socketChannel.isConnected()) {
                System.out.println("Connected to server.");
            }

            Selector selector = Selector.open();
            ByteBuffer byteBuffer = ByteBuffer.allocate(512);
            socketChannel.register(selector, SelectionKey.OP_WRITE, byteBuffer);

            // infinite loop for the selector
            while(true) {
                selector.select();
                Set<SelectionKey> keySet = selector.selectedKeys();

                // iterating over selected-keys set
                for(SelectionKey key : keySet) {

                    if(key.isWritable()) {
                        SocketChannel toServer = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();

                        // creating a message
                        System.out.print("Me: ");
                        Scanner console = new Scanner(System.in);
                        byte[] message = console.nextLine().getBytes();

                        /* if message does not contain quit command
                        then we send the message to server */
                        if(!message.equals("quit")) {
                            buffer = buffer.put(0, message);
                            buffer.limit(message.length);

                            toServer.write(buffer);
                            key.interestOps(SelectionKey.OP_READ);
                            buffer.clear();
                        }
                        // otherwise we exit the program
                        else {
                            message = "quit".getBytes();
                            buffer = buffer.put(0, message);

                            while(buffer.hasRemaining()) {
                                toServer.write(buffer);
                            }

                            System.out.println("Disconnected from server.");
                            System.exit(0);
                        }
                    }

                    if(key.isReadable()) {
                        SocketChannel fromServer = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();

                        fromServer.read(buffer);
                        buffer.flip();

                        String message = new String(buffer.array(), buffer.position(), buffer.limit());
                        System.out.println("Server: " + message);

                        buffer.clear();
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }
                // it is important to clear the Set or program will reuse
                // selected keys that we already have - they keep staying there
                keySet.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
