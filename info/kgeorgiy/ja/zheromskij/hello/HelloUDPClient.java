
package info.kgeorgiy.ja.zheromskij.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import info.kgeorgiy.java.advanced.hello.HelloClient;

public class HelloUDPClient implements HelloClient {
    private static final int SO_TIMEOUT = 1000;
    private static final int WAIT_TIMEOUT_MINUTES = 1;

    /**
     * Runs the client.
     * 
     * @param args command line arguments:
     *             <ul>
     *             <li>host name or ip adress of the server</li>
     *             <li>port number of the server</li>
     *             <li>message prefix</li>
     *             <li>number of parallel streams of requests</li>
     *             <li>number of requests in each stream</li>
     *             </ul>
     */
    public static void main(final String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Usage: host port prefix parallel streams requests");
            return;
        }
        HelloClient client = new HelloUDPClient();
        try {
            client.run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]),
                    Integer.parseInt(args[4]));
        } catch (NumberFormatException e) {
            System.err.println("Invalid number: " + e.getMessage());
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        ExecutorService workers = Executors.newFixedThreadPool(threads);
        InetSocketAddress targetAdress;
        try {
            InetAddress hostAddress = InetAddress.getByName(host);
            targetAdress = new InetSocketAddress(hostAddress, port);
        } catch (UnknownHostException e) {
            System.err.println("Invalid host: " + e.getMessage());
            return;
        }
        IntStream.range(0, threads)
                .forEach(i -> workers.submit(() -> doRequest(targetAdress, prefix, requests, i)));
        workers.shutdown();
        try {
            workers.awaitTermination(WAIT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }
    }

    private void doRequest(InetSocketAddress targetAdress, String prefix, int requests, int threadIndex) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SO_TIMEOUT);
            for (int i = 0; i < requests; i++) {
                String requestMessage = prefix + threadIndex + "_" + i;
                byte[] requestMessageBytes = requestMessage.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(requestMessageBytes, requestMessageBytes.length,
                        targetAdress);
                DatagramPacket response = new DatagramPacket(new byte[socket.getReceiveBufferSize()],
                        socket.getReceiveBufferSize());
                while (!socket.isClosed()) {
                    try {
                        socket.send(packet);
                        socket.receive(response);
                        String responseMessage = new String(response.getData(), 0, response.getLength(),
                                StandardCharsets.UTF_8);
                        if (responseMessage.contains(requestMessage)) {
                            System.out.println("Request: \"" + requestMessage + "\" " +
                                    "response: \"" + responseMessage + "\"");
                            break;
                        }
                    } catch (IOException e) {
                        System.err.println("Error when requesting: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}