/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.connectors.handlers;

import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import glide.connectors.resources.ThreadPoolResource;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.unix.DomainSocketAddress;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import redis_request.RedisRequestOuterClass.RedisRequest;
import response.ResponseOuterClass.Response;

/**
 * Class responsible for handling calls to/from a netty.io {@link Channel}. Uses a {@link
 * CallbackDispatcher} to record callbacks of every request sent.
 */
public class ChannelHandler {

    protected final Channel channel;
    protected final CallbackDispatcher callbackDispatcher;

    /**
     * Open a new channel for a new client and running it on the provided EventLoopGroup.
     *
     * @param callbackDispatcher Dispatcher to handle callbacks
     * @param socketPath Address to connect
     * @param threadPoolResource Resource to choose ELG and domainSocketChannelClass
     */
    public ChannelHandler(
            CallbackDispatcher callbackDispatcher,
            String socketPath,
            ThreadPoolResource threadPoolResource)
            throws InterruptedException {

        channel =
                new Bootstrap()
                        .group(threadPoolResource.getEventLoopGroup())
                        .channel(threadPoolResource.getDomainSocketChannelClass())
                        .handler(new ProtobufSocketChannelInitializer(callbackDispatcher))
                        .connect(new DomainSocketAddress(socketPath))
                        // TODO    .addListener(new NettyFutureErrorHandler())
                        //   we need to use connection promise here for that ^
                        .sync()
                        .channel();
        this.callbackDispatcher = callbackDispatcher;
    }

    /**
     * Complete a protobuf message and write it to the channel (to UDS).
     *
     * @param request Incomplete request, function completes it by setting callback ID
     * @param flush True to flush immediately
     * @return A response promise
     */
    public CompletableFuture<Response> write(RedisRequest.Builder request, boolean flush) {
        var commandId = callbackDispatcher.registerRequest();
        request.setCallbackIdx(commandId.getKey());

        if (flush) {
            channel
                    .writeAndFlush(request.build())
                    .addListener(new NettyFutureErrorHandler(commandId.getValue()));
        } else {
            channel.write(request.build()).addListener(new NettyFutureErrorHandler(commandId.getValue()));
        }
        return commandId.getValue();
    }

    /**
     * Write a protobuf message to the channel (to UDS).
     *
     * @param request A connection request
     * @return A connection promise
     */
    public CompletableFuture<Response> connect(ConnectionRequest request) {
        var future = callbackDispatcher.registerConnection();
        channel.writeAndFlush(request).addListener(new NettyFutureErrorHandler(future));
        return future;
    }

    /** Closes the UDS connection and frees corresponding resources. */
    public ChannelFuture close() {
        callbackDispatcher.shutdownGracefully();
        return channel.close();
    }

    /** Propagate an error from Netty's future into the future returned to user. */
    @RequiredArgsConstructor
    private static class NettyFutureErrorHandler implements ChannelFutureListener {

        private final CompletableFuture<Response> promise;

        @Override
        public void operationComplete(@NonNull ChannelFuture channelFuture) throws Exception {
            if (!channelFuture.isSuccess()) {
                promise.completeExceptionally(channelFuture.cause());
            }
        }
    }
}
