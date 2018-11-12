package org.huangjiaqqian.smartproxy.common.netty;

import java.net.InetSocketAddress;

import org.huangjiaqqian.smartproxy.common.BinaryWriter;
import org.huangjiaqqian.smartproxy.common.ServerTunnel;
import org.huangjiaqqian.smartproxy.common.TunnelUtil;
import org.huangjiaqqian.smartproxy.common.Util;

import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyTcpServer {
	
	protected ServerChannelObjCache channelObjCache;
	
	public static int bufSize = 1024;

	private EventLoopGroup bossGroup = new NioEventLoopGroup(); // accept线城市

	private EventLoopGroup workerGroup = new NioEventLoopGroup(); // 处理事件线程数

	private int bindPort;

	private byte[] natAddrBytes;

	private DragoniteSocket dragoniteSocket;

	public NettyTcpServer(DragoniteSocket dragoniteSocket, int bindPort, InetSocketAddress natAddr, ServerChannelObjCache channelObjCache) {
		super();
		this.bindPort = bindPort;
		this.dragoniteSocket = dragoniteSocket;

		natAddrBytes = Util.genAddrBytes(natAddr.getHostString(), natAddr.getPort());
		
		this.channelObjCache = channelObjCache;
	}

	public void start() throws InterruptedException {
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {

						ch.pipeline().addLast(new SocketDataHandler());

					}

				}).option(ChannelOption.SO_BACKLOG, bufSize);

		final ChannelFuture future = bootstrap.bind(bindPort).sync();
		new Thread(() -> {
			try {
				future.channel().closeFuture().sync();
				workerGroup.shutdownGracefully();
				bossGroup.shutdownGracefully();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();

	}

	public void close() {
		workerGroup.shutdownGracefully();
		bossGroup.shutdownGracefully();
	}

	class SocketDataHandler extends ChannelInboundHandlerAdapter {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			ByteBuf byteBuf = (ByteBuf) msg;

			byte[] buf = new byte[byteBuf.readableBytes()];
			byteBuf.getBytes(byteBuf.readerIndex(), buf);

			ChannelObj channelObj = channelObjCache.getChannelObj(ctx.channel());
			if (channelObj != null) {
				TunnelUtil.sendDataBytes(dragoniteSocket, buf, channelObj.getConnId());
			}
			if (ServerTunnel.upFlowMap != null) {
				// 上传流量统计
				Integer currentPort = ((InetSocketAddress) channelObj.getChannel().localAddress()).getPort(); // 绑定的端口
				Double upFlow = ServerTunnel.upFlowMap.get(currentPort);
				upFlow = upFlow == null ? buf.length : (upFlow + buf.length);
				ServerTunnel.upFlowMap.put(currentPort, upFlow);
			}
			
			// ReferenceCountUtil.release(msg); // 释放
			super.channelRead(ctx, msg);
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			// System.out.println("连接打开...");

			ChannelObj channelObj = channelObjCache.addChannelObj(ctx.channel());

			byte flag = (byte) 1;
			byte secondFlag = (byte) 1;

			BinaryWriter writer = new BinaryWriter(6 + 1 + natAddrBytes.length);
			writer.putSignedByte(flag);
			writer.putSignedByte(secondFlag);
			writer.putSignedShort(channelObj.getConnId());
			writer.putBytesGroupWithByteLength(natAddrBytes);

			Util.writeAndFlush(dragoniteSocket, writer.toBytes());

			ctx.channel().config().setAutoRead(false); // 暂时不读数据

			super.channelActive(ctx);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			// System.out.println("连接关闭...");
			closeChannel(ctx);
			super.channelInactive(ctx);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			// System.out.println("发生异常...");
			super.exceptionCaught(ctx, cause);
			Channel channel = ctx.channel();
			if (channel.isActive())
				ctx.close();
		}

		private void closeChannel(ChannelHandlerContext ctx) {
			ChannelObj channelObj = channelObjCache.getChannelObj(ctx.channel());
			if (channelObj == null) {
				return;
			}

			// System.out.println("关闭连接...");
			if (ctx.channel().isActive()) {
				ctx.channel().close();
			}
			// 发送关闭连接消息
			TunnelUtil.sendCloseConn(dragoniteSocket, channelObj.getConnId());
			channelObjCache.removeChannelObj(ctx.channel());
		}
	}

}
