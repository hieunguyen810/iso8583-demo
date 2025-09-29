package com.example.client.client;

import com.example.common.model.Iso8583Message;
import com.example.client.processor.Iso8583Processor;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(2)
public class Iso8583Client implements CommandLineRunner {

    private static final int PORT = 8583;
    private Channel channel;
    private EventLoopGroup workerGroup;
    private final AtomicInteger stanCounter = new AtomicInteger(1);
    private final ScheduledExecutorService echoScheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean connected = false;
    
    @Value("${iso8583.client.server-host}")
    private String serverHost;

    @Override
    public void run(String... args) throws Exception {
        String mode = System.getProperty("app.mode", "both");
        if ("server".equals(mode)) {
            return;
        }

        if ("client".equals(mode)) {
            runPersistentClient();
        } else {
            // Auto test mode
            Thread.sleep(3000);
            runAutoTests();
        }
    }

    private void runPersistentClient() {
        System.out.println("\n🔄 Persistent ISO 8583 Client (Netty)");
        System.out.println("Commands:");
        System.out.println("  'connect'    - Connect to server");
        System.out.println("  'disconnect' - Disconnect from server");
        System.out.println("  'send'       - Send authorization request (0200)");
        System.out.println("  'echo'       - Send echo message (0800)");
        System.out.println("  'status'     - Show connection status");
        System.out.println("  'auto-echo'  - Toggle automatic echo messages");
        System.out.println("  'exit'       - Quit");

        Scanner scanner = new Scanner(System.in);
        boolean autoEcho = false;

        while (true) {
            System.out.print("\nClient" + (connected ? " [CONNECTED]" : " [DISCONNECTED]") + "> ");
            String command = scanner.nextLine().trim().toLowerCase();

            try {
                switch (command) {
                    case "connect":
                        connect();
                        break;

                    case "disconnect":
                        disconnect();
                        break;

                    case "send":
                        if (ensureConnected()) {
                            sendAuthorizationRequest();
                        }
                        break;

                    case "echo":
                        if (ensureConnected()) {
                            sendEchoMessage();
                        }
                        break;

                    case "status":
                        showConnectionStatus();
                        break;

                    case "auto-echo":
                        autoEcho = toggleAutoEcho(autoEcho);
                        break;

                    case "exit":
                    case "quit":
                        disconnect();
                        echoScheduler.shutdown();
                        System.out.println("👋 Goodbye!");
                        System.exit(0);
                        break;

                    default:
                        System.out.println("❓ Unknown command: " + command);
                        break;
                }
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
            }
        }
    }

    private void connect() {
        try {
            if (connected) {
                System.out.println("ℹ️ Already connected!");
                return;
            }
            
            System.out.println("🔄 Connecting to server...");
            
            workerGroup = new NioEventLoopGroup();
            
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // Length field handling (2 bytes for message length)
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
                            pipeline.addLast(new LengthFieldPrepender(2));
                            
                            // Message handler
                            pipeline.addLast(new Iso8583ClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(serverHost, PORT).sync();
            channel = future.channel();
            connected = true;
            
            System.out.println("✅ Connected successfully!");
            
            // Send initial echo to verify connection
            sendEchoMessage();

        } catch (Exception e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
            connected = false;
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
        }
    }

    private void disconnect() {
        try {
            if (!connected) {
                System.out.println("ℹ️ Already disconnected!");
                return;
            }

            System.out.println("🔄 Disconnecting...");

            if (channel != null && channel.isActive()) {
                channel.close().sync();
            }

            if (workerGroup != null) {
                workerGroup.shutdownGracefully().sync();
            }

            connected = false;
            System.out.println("✅ Disconnected successfully!");

        } catch (Exception e) {
            System.err.println("❌ Disconnect error: " + e.getMessage());
        } finally {
            connected = false;
        }
    }

    private boolean ensureConnected() {
        if (!connected || channel == null || !channel.isActive()) {
            System.out.println("❌ Not connected! Use 'connect' command first.");
            return false;
        }
        return true;
    }

    private void sendAuthorizationRequest() throws Exception {
        Iso8583Message request = createAuthorizationRequest();
        String requestMessage = request.toString();
        System.out.println("📤 Sending Authorization: " + requestMessage);
        
        ByteBuf buf = channel.alloc().buffer();
        buf.writeBytes(requestMessage.getBytes(StandardCharsets.UTF_8));
        
        channel.writeAndFlush(buf).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("... Waiting for response...");
            } else {
                System.err.println("❌ Failed to send message: " + future.cause().getMessage());
            }
        });
    }

    private void sendEchoMessage() throws Exception {
        Iso8583Message echoRequest = createEchoMessage();
        String requestMessage = echoRequest.toString();
        System.out.println("💓 Sending Echo: " + requestMessage);
        
        ByteBuf buf = channel.alloc().buffer();
        buf.writeBytes(requestMessage.getBytes(StandardCharsets.UTF_8));
        
        channel.writeAndFlush(buf).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("... Waiting for echo response...");
            } else {
                System.err.println("❌ Failed to send echo: " + future.cause().getMessage());
            }
        });
    }

    private Iso8583Message createAuthorizationRequest() {
        Iso8583Message msg = new Iso8583Message();
        Random random = new Random();
        msg.setMti("0200");
        msg.addField(2, generatePan());
        msg.addField(3, "000000");
        msg.addField(4, String.format("%012d", random.nextInt(100000) + 1000));
        msg.addField(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")));
        msg.addField(11, String.format("%06d", stanCounter.getAndIncrement()));
        msg.addField(12, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
        msg.addField(13, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd")));
        msg.addField(18, "5999");
        msg.addField(22, "012");
        msg.addField(25, "00");
        msg.addField(37, String.format("%012d", random.nextInt(999999999)));
        msg.addField(41, "TERM001 ");
        msg.addField(42, "MERCHANT001     ");
        msg.addField(49, "840");
        return msg;
    }

    private Iso8583Message createEchoMessage() {
        Iso8583Message msg = new Iso8583Message();
        msg.setMti("0800");
        msg.addField(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")));
        msg.addField(11, String.format("%06d", stanCounter.getAndIncrement()));
        msg.addField(70, "001");
        return msg;
    }

    private String generatePan() {
        Random random = new Random();
        return "4000" + String.format("%012d", random.nextInt(1000000000));
    }

    private void showConnectionStatus() {
        System.out.println("\n📊 Connection Status:");
        System.out.println("   Connected: " + (connected ? "✅ Yes" : "❌ No"));
        if (connected && channel != null) {
            System.out.println("   Server: " + channel.remoteAddress());
            System.out.println("   Local: " + channel.localAddress());
            System.out.println("   Channel Active: " + channel.isActive());
            System.out.println("   Channel Writable: " + channel.isWritable());
        }
        System.out.println("   STAN Counter: " + stanCounter.get());
    }

    private boolean toggleAutoEcho(boolean currentState) {
        if (currentState) {
            echoScheduler.shutdownNow();
            System.out.println("🔇 Automatic echo messages disabled");
            return false;
        } else {
            echoScheduler.scheduleAtFixedRate(() -> {
                if (connected) {
                    try {
                        System.out.println("\n⏰ Auto echo...");
                        sendEchoMessage();
                    } catch (Exception e) {
                        System.err.println("❌ Auto echo failed: " + e.getMessage());
                        connected = false;
                    }
                }
            }, 30, 30, TimeUnit.SECONDS);

            System.out.println("🔔 Automatic echo messages enabled (every 30 seconds)");
            return true;
        }
    }

    private void runAutoTests() throws Exception {
        System.out.println("\n🔄 Starting auto tests with persistent connection...");
        connect();
        if (!connected) {
            System.err.println("❌ Could not establish connection for auto tests");
            return;
        }
        try {
            sendEchoMessage();
            Thread.sleep(1000);
            for (int i = 1; i <= 3; i++) {
                System.out.println("\n--- Test Transaction " + i + " ---");
                sendAuthorizationRequest();
                Thread.sleep(1000);
            }
            sendEchoMessage();
            System.out.println("\n✅ All tests completed using persistent connection!");
        } finally {
            disconnect();
        }
    }

    // Netty Channel Handler for incoming messages
    private class Iso8583ClientHandler extends ChannelInboundHandlerAdapter {
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf buf = (ByteBuf) msg;
            try {
                String isoMessage = buf.toString(StandardCharsets.UTF_8);
                Iso8583Processor.processIncomingMessage(isoMessage, connected);
            } finally {
                buf.release();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            System.out.println("\n👋 Server disconnected.");
            connected = false;
            System.out.print("\nClient [DISCONNECTED]> ");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("\n🔌 Connection error: " + cause.getMessage());
            connected = false;
            ctx.close();
        }
    }
}