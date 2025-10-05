package com.example.client.service;

import com.example.client.model.ConnectionInfo;
import com.example.common.model.Iso8583Message;
import com.example.common.model.ValidationResult;
import com.example.common.parser.Iso8583Parser;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ConnectionService {

    private final Map<String, ConnectionInfo> connections = new ConcurrentHashMap<>();
    private final Map<String, Channel> activeChannels = new ConcurrentHashMap<>();
    private final Map<String, EventLoopGroup> eventLoopGroups = new ConcurrentHashMap<>();
    private final AtomicInteger stanCounter = new AtomicInteger(1);

    public List<ConnectionInfo> getAllConnections() {
        return new ArrayList<>(connections.values());
    }

    public void addConnection(ConnectionInfo connectionInfo) {
        connections.put(connectionInfo.getConnectionId(), connectionInfo);
    }

    public void connect(String connectionId) throws Exception {
        ConnectionInfo conn = connections.get(connectionId);
        if (conn == null) {
            throw new RuntimeException("Connection not found: " + connectionId);
        }

        if (conn.isConnected()) {
            throw new RuntimeException("Already connected: " + connectionId);
        }

        EventLoopGroup group = new NioEventLoopGroup();
        eventLoopGroups.put(connectionId, group);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
                        pipeline.addLast(new LengthFieldPrepender(2));
                        pipeline.addLast(new ClientHandler(connectionId));
                    }
                });

        ChannelFuture future = bootstrap.connect(conn.getHost(), conn.getPort()).sync();
        Channel channel = future.channel();
        activeChannels.put(connectionId, channel);
        conn.setConnected(true);
    }

    public void disconnect(String connectionId) throws Exception {
        ConnectionInfo conn = connections.get(connectionId);
        if (conn == null) {
            throw new RuntimeException("Connection not found: " + connectionId);
        }

        Channel channel = activeChannels.remove(connectionId);
        if (channel != null && channel.isActive()) {
            channel.close().sync();
        }

        EventLoopGroup group = eventLoopGroups.remove(connectionId);
        if (group != null) {
            group.shutdownGracefully();
        }

        conn.setConnected(false);
    }

    public void removeConnection(String connectionId) throws Exception {
        if (connections.containsKey(connectionId)) {
            disconnect(connectionId);
            connections.remove(connectionId);
        }
    }

    public String[] sendEcho(String connectionId) throws Exception {
        Channel channel = getActiveChannel(connectionId);
        
        Iso8583Message echoMsg = new Iso8583Message();
        echoMsg.setMti("0800");
        echoMsg.addField(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")));
        echoMsg.addField(11, String.format("%06d", stanCounter.getAndIncrement()));
        echoMsg.addField(70, "001");
        
        String request = echoMsg.toString();
        String response = sendAndWaitForResponse(channel, request);
        
        return new String[]{request, response};
    }

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("${iso8583.client.authorization.enabled:false}")
    private boolean authorizationEnabled;
    
    @Value("${kafka.topic.iso8583.request:iso8583-requests}")
    private String requestTopic;

    public String[] sendMessage(String connectionId, String message) throws Exception {
        // Parse and validate message
        Iso8583Message parsedMsg = Iso8583Parser.parseMessage(message);
        ValidationResult validation = Iso8583Parser.validateMessage(parsedMsg);
        
        if (!validation.isValid()) {
            throw new RuntimeException("Invalid message: " + String.join(", ", validation.getErrors()));
        }
        
        if (authorizationEnabled && kafkaTemplate != null) {
            // Send to Kafka for authorization with field 37 as partition key for load balancing
            String partitionKey = parsedMsg.getField(37);
            if (partitionKey == null) partitionKey = connectionId;
            System.out.println("📤 Sending to Kafka for authorization with key: " + partitionKey);
            kafkaTemplate.send(requestTopic, partitionKey, message);
            return new String[]{message, "Sent to authorization service"};
        } else {
            // Direct send to server
            Channel channel = getActiveChannel(connectionId);
            String response = sendAndWaitForResponse(channel, message);
            return new String[]{message, response};
        }
    }

    private Channel getActiveChannel(String connectionId) throws Exception {
        Channel channel = activeChannels.get(connectionId);
        if (channel == null || !channel.isActive()) {
            throw new RuntimeException("Connection not active: " + connectionId);
        }
        return channel;
    }

    private String sendAndWaitForResponse(Channel channel, String message) throws Exception {
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        
        // Store the future in channel attributes for the handler to complete
        channel.attr(AttributeKey.valueOf("responseFuture")).set(responseFuture);
        
        ByteBuf buf = channel.alloc().buffer();
        buf.writeBytes(message.getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(buf);
        
        return responseFuture.get(10, TimeUnit.SECONDS);
    }

    private class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final String connectionId;

        public ClientHandler(String connectionId) {
            this.connectionId = connectionId;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            String message = msg.toString(StandardCharsets.UTF_8);
            System.out.println("📨 Received from server: " + message);
            
            // Check if this is a response to a pending request
            CompletableFuture<String> future = ctx.channel().attr(AttributeKey.<CompletableFuture<String>>valueOf("responseFuture")).get();
            if (future != null) {
                // This is a response to our request
                future.complete(message);
                ctx.channel().attr(AttributeKey.valueOf("responseFuture")).set(null);
            } else if (authorizationEnabled && kafkaTemplate != null) {
                // This is an unsolicited message from server - send to Kafka for authorization
                Iso8583Message parsedMsg = Iso8583Parser.parseMessage(message);
                String partitionKey = parsedMsg.getField(37);
                if (partitionKey == null) partitionKey = connectionId;
                System.out.println("📤 Sending unsolicited message to Kafka with key: " + partitionKey);
                kafkaTemplate.send(requestTopic, partitionKey, message);
            } else {
                // No authorization - just log the message
                System.out.println("📝 Unsolicited message (no authorization): " + message);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            ConnectionInfo conn = connections.get(connectionId);
            if (conn != null) {
                conn.setConnected(false);
            }
            activeChannels.remove(connectionId);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ConnectionInfo conn = connections.get(connectionId);
            if (conn != null) {
                conn.setConnected(false);
            }
            ctx.close();
        }
    }
}