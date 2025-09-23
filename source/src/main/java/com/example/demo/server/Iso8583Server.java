package com.example.demo.server;

import com.example.demo.model.Iso8583Message;
import com.example.demo.parser.Iso8583Parser;
import com.example.demo.service.Iso8583Processor;
import com.example.demo.simulator.Iso8583TransactionSimulator;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Order(1)
public class Iso8583Server {
    private static final int PORT = 8583;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @EventListener(ApplicationReadyEvent.class)
    public void startServer() {
        String mode = System.getProperty("app.mode", "both");
        if (!"client".equals(mode)) {
            new Thread(this::runServer, "netty-iso8583-server-thread").start();
            System.out.println("🚀 Netty ISO 8583 Server starting on port " + PORT);
        }
    }

    private void runServer() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ChannelPipeline p = ch.pipeline();

                     // Inbound: read 2-byte length prefix and produce a frame (strip the length field)
                     p.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
                     // Convert ByteBuf frames to String (UTF-8)
                     p.addLast(new StringDecoder(StandardCharsets.UTF_8));

                     // Outbound: add 2-byte length prefix then encode String -> ByteBuf
                     p.addLast(new LengthFieldPrepender(2));
                     p.addLast(new StringEncoder(StandardCharsets.UTF_8));

                     // Our handler that processes ISO messages
                     p.addLast(new Iso8583ServerHandler());
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(PORT).sync();
            System.out.println("✅ Server ready and listening...");
            System.out.println("📋 Supported message types: 0200 (Auth), 0800 (Echo)");
            f.channel().closeFuture().sync();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("❌ Server interrupted: " + ie.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Server error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        if (!running.compareAndSet(true, false)) return;
        try {
            if (bossGroup != null) bossGroup.shutdownGracefully().sync();
            if (workerGroup != null) workerGroup.shutdownGracefully().sync();
            System.out.println("🛑 Netty server shut down gracefully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("❌ Shutdown interrupted: " + e.getMessage());
        }
    }

    private static class Iso8583ServerHandler extends SimpleChannelInboundHandler<String> {
        // Scheduled future for periodic sending per connection
        private ScheduledFuture<?> periodicTask;
        private ChannelHandlerContext ctx;
        private String clientAddress;

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            this.ctx = ctx;
            clientAddress = ctx.channel().remoteAddress().toString();
            System.out.println("🔌 Client connected: " + clientAddress);

            // Schedule periodic task every 10 seconds (first run after 10s)
            periodicTask = ctx.executor().scheduleAtFixedRate(() -> {
                if (ctx.channel().isActive()) {
                    try {
                        Iso8583Message transaction = Iso8583TransactionSimulator.getTransaction();
                        String transactionMessage = transaction.toString();
                        System.out.println("📤 [" + clientAddress + "] Periodically sending: " + transactionMessage);
                        // Write with pipeline: StringEncoder -> LengthFieldPrepender will add length prefix
                        ctx.writeAndFlush(transactionMessage).addListener(f -> {
                            if (!f.isSuccess()) {
                                System.err.println("❌ [" + clientAddress + "] Periodic send error: " + f.cause().getMessage());
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("❌ [" + clientAddress + "] Periodic send error: " + e.getMessage());
                    }
                }
            }, 10, 10, java.util.concurrent.TimeUnit.SECONDS);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            // msg is one framed String message (length already stripped)
            System.out.println("📨 [" + clientAddress + "] Received: " + msg);
            try {
                Iso8583Message request = Iso8583Parser.parseMessage(msg);
                Iso8583Message response = Iso8583Processor.processMessage(request);
                String responseMessage = response.toString();
                // writeAndFlush will go through StringEncoder and LengthFieldPrepender
                ctx.writeAndFlush(responseMessage).addListener(f -> {
                    if (f.isSuccess()) {
                        System.out.println("📤 [" + clientAddress + "] Sent: " + responseMessage);
                    } else {
                        System.err.println("❌ [" + clientAddress + "] Send failed: " + f.cause().getMessage());
                    }
                });
            } catch (Exception e) {
                System.err.println("❌ [" + clientAddress + "] Error processing message: " + e.getMessage());
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            System.out.println("👋 [" + clientAddress + "] Client disconnected");
            if (periodicTask != null && !periodicTask.isCancelled()) {
                periodicTask.cancel(true);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("🔌 [" + clientAddress + "] Connection error: " + cause.getMessage());
            ctx.close();
        }

    }
}
