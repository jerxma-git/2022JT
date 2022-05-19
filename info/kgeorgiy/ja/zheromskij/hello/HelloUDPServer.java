
package info.kgeorgiy.ja.zheromskij.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import info.kgeorgiy.java.advanced.hello.HelloServer;

public class HelloUDPServer implements HelloServer {
    private ExecutorService workers;
    private DatagramSocket socket;
    
    /**
     * Runs the server
     * 
     * @param args command line arguments:
     *             <ul>
     *             <li>port number of the server</li>
     *             <li>number of working threads</li>
     *             </ul>
     */
    public static void main(final String[] args) {
        if (args == null || args.length != 1 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Usage: port threads");
            return;
        }
        try (HelloServer server = new HelloUDPServer()) {
            server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            Thread.currentThread().wait();
        } catch (NumberFormatException e) {
            System.err.println("Invalid number: " + e.getMessage());
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Stops the server, deallocating all resources.
     */
    @Override
    public void close() {
        
        socket.close();
        workers.shutdownNow();
        try {
            workers.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }
        workers = null;
        socket = null;

    }

    /**
     * Starts the server.
     * Returns immediately.
     * 
     * @param port    port number of the server
     * @param threads number of working threads
     */
    @Override
    public void start(int port, int threads) {
        if (workers != null || socket != null) {
            return;
        }
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Cannot create socket: " + e.getMessage());
            return;
        }
        workers = Executors.newFixedThreadPool(threads);
        IntStream.range(0, threads).forEach(i -> workers.submit(this::run));
    }

    private void run() {
        DatagramPacket packet;
        try {
            packet = new DatagramPacket(new byte[socket.getReceiveBufferSize()],
                    socket.getReceiveBufferSize());
        } catch (SocketException e) {
            System.err.println("Cannot create packet: " + e.getMessage());
            return;
        }
        while (!socket.isClosed()) {
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                String response = "Hello, " + message;
                socket.send(new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(),
                        packet.getSocketAddress()));
            } catch (IOException e) {
                System.err.println("Cannot receive packet: " + e.getMessage());
                return;
            }
        }
    }

    
}