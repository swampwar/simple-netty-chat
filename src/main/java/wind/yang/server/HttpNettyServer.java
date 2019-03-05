package wind.yang.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpNettyServer {

    @Value("${tcp.port}")
    public int PORT;

    @Value("${boss.thread.count}")
    public int bossGroupCnt;

    @Value("${worker.thread.count}")
    public int workerGroupCnt;

    public void start() {
        EventLoopGroup boosGroup = new NioEventLoopGroup(bossGroupCnt);
        EventLoopGroup workerGroup = new NioEventLoopGroup(workerGroupCnt);

        try{
            ServerBootstrap bs = new ServerBootstrap();
            bs.group(boosGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpInitializer());
            System.out.println("ServerBootstrap Initialized! PORT : " + PORT);

            ChannelFuture bindFuture = bs.bind(PORT).sync();
            System.out.println("ServerSocket bind completed!");

            bindFuture.channel().closeFuture().sync();

        } catch(InterruptedException e){
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            boosGroup.shutdownGracefully();
            System.out.println("ServerSocket shutdown completed!");
        }

    }
}
