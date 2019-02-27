package wind.yang.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpInitializer extends ChannelInitializer<SocketChannel> {
    HttpServerHandler httpServerHandler = new HttpServerHandler();

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline//.addLast("logger", new LoggingHandler(LogLevel.INFO))
                .addLast("httpServerCodec", new HttpServerCodec()) // Netty 제공코덱(Inbound, Outbound)
//                .addLast(new HttpObjectAggregator(65536))
                .addLast("httpHandler", httpServerHandler);
    }
}
