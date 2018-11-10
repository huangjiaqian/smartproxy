package org.huangjiaqqian.smartproxy.common;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class MainTest01 {
	static EventLoopGroup bossGroup = new NioEventLoopGroup(); // accept线城市
	static EventLoopGroup workerGroup = new NioEventLoopGroup(); // 处理事件线程数
	
	public static void main(String[] args) throws InterruptedException {
		ServerBootstrap bootstrap = new ServerBootstrap();

		try {
			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {

						@Override
						protected void initChannel(SocketChannel ch) throws Exception {

							ch.pipeline().addLast(new DiscardServerHandler());

						}

					}).option(ChannelOption.SO_BACKLOG, 128).option(ChannelOption.SO_KEEPALIVE, true);

			final ChannelFuture future = bootstrap.bind(6001).sync();
			System.out.println("111");
			new Thread(()->{
				try {
					future.channel().closeFuture().sync();
					System.out.println("关闭");
					workerGroup.shutdownGracefully();
					bossGroup.shutdownGracefully();
				} catch (InterruptedException e) {
					System.out.println("2222222222222222222222222222");
					e.printStackTrace();
				}				
			}).start();
			System.out.println("222");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("11111111111111111111111111111111");
		} finally {
			//workerGroup.shutdownGracefully();
			//bossGroup.shutdownGracefully();
		}
		//Thread.sleep(1000000);
	}

	static class DiscardServerHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			ByteBuf byteBuf = (ByteBuf) msg;

			byte[] buf = new byte[byteBuf.readableBytes()];
			byteBuf.getBytes(byteBuf.readerIndex(), buf);

			System.out.println(new String(buf));
			
			if("close".equals(new String(buf))) {
				workerGroup.shutdownGracefully();
				bossGroup.shutdownGracefully();
				//ctx.channel().close();
			}
			
			//ReferenceCountUtil.release(msg); // 释放
			super.channelRead(ctx, msg);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			System.out.println("连接关闭...");
			super.channelInactive(ctx);
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			System.out.println("连接打开...");
			ctx.channel().config().setAutoRead(false);
			new Thread(()->{
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("开始读数据...");
				ctx.channel().config().setAutoRead(true);
			}).start();
			super.channelActive(ctx);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			System.out.println("发生异常...");
			super.exceptionCaught(ctx, cause);
		}
	}

}
