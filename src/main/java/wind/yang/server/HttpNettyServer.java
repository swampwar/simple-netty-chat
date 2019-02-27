package wind.yang.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class HttpNettyServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        EventLoopGroup boosGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try{
            ServerBootstrap bs = new ServerBootstrap();
            bs.group(boosGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpInitializer());

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
