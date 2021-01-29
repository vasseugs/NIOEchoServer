package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Server {

    public static void main(String[] args) {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress("localhost", 20000));
            System.out.println("Server started.");

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // infinite loop for the selector
            while(true) {
                selector.select();
                Set<SelectionKey> keySet = selector.selectedKeys();

                // iterating over selected-keys set
                for(SelectionKey key : keySet) {
                    /* if request to accept the connection
                    comes in severSocketChannel, accept the
                    incoming connection and register new channel
                    in selector */
                    if(key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();

                        if(client != null) {
                            if (client.isConnected()) System.out.println("Client connected");

                            // буфер, связанный с клиентом
                            ByteBuffer buffer = ByteBuffer.allocate(512);
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ, buffer);
                        } else continue;
                    }

                    if(key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();

                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        client.read(buffer);
                        buffer.flip();

                        String message = new String(buffer.array(), buffer.position(), buffer.limit());
                        /* if the client does not want to disconnect,
                        print out his message and switch the channel for
                        writing back */
                        if(!message.equals("quit")) {
                            System.out.println("Client: " + message);
                            key.interestOps(SelectionKey.OP_WRITE);
                        } else {
                            throw new SocketException();
                        }
                    }

                    if(key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();

                        client.write(buffer);

                        buffer.clear();
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }
                // it is important to clear the Set or program will reuse
                // selected keys that we already have - they keep staying there
                keySet.clear();
            }
        } catch(SocketException e) {
            System.out.println("Client disconnected");
            System.exit(0);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
