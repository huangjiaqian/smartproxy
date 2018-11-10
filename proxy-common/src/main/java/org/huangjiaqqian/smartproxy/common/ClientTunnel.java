package org.huangjiaqqian.smartproxy.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.huangjiaqqian.smartproxy.common.netty.ChannelObj;
import org.huangjiaqqian.smartproxy.common.netty.ClientChannelObjCache;

import com.vecsight.dragonite.sdk.exception.ConnectionNotAliveException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ClientTunnel {

	protected DragoniteSocket dragoniteSocket;

	protected ExecutorService es = Executors.newCachedThreadPool();

	private EventLoopGroup workerGroup;
	private Bootstrap bootstrap;

	public ClientTunnel() throws IOException {
		super();
		workerGroup = new NioEventLoopGroup();
		bootstrap = new Bootstrap();
		bootstrap.group(workerGroup) // 注册线程池
				.channel(NioSocketChannel.class) // 使用NioSocketChannel来作为连接用的channel类
				// .remoteAddress(new InetSocketAddress("127.0.0.1", 6001)) //
				// 绑定连接端口和host信息
				.handler(new ChannelInitializer<SocketChannel>() { // 绑定连接初始化器
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new SocketClientHandler());
					}
				});

		// ChannelFuture channelFuture = b.connect("127.0.0.1", 6001);
		// channelFuture = b.connect("127.0.0.1", 6001);

	}

	private void closeSocket(Channel channel) {
		if (channel != null && channel.isActive()) {
			channel.close();
		}
	}

	private void closeSocketAndRemove(short connId, Channel channel) {
		closeSocket(channel);
		ClientChannelObjCache.removeChannelObj(connId);
	}

	private void writeData(byte[] buf) {
		try {
			Util.writeAndFlush(dragoniteSocket, buf);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void doExecuteWithConn(byte[] buf, BinaryReader reader) {
		byte secondFlag = reader.getSignedByte();
		short connId = reader.getSignedShort();

		if ((byte) 1 == secondFlag) {
			// 创建连接
			byte[] addrBytes = reader.getBytesGroupWithByteLength();
			InetSocketAddress address = Util.genAddr(addrBytes);

			es.execute(() -> {
				BinaryWriter writer = new BinaryWriter(6);
				boolean connectSuccess = true;
				ChannelFuture channelFuture = null;
				try {
					channelFuture = bootstrap.connect(address.getHostString(), address.getPort()).sync();
				} catch (Exception e) {
					//e.printStackTrace();
					connectSuccess = false;
				} finally {
					if(connectSuccess) {
						writer.putSignedByte((byte) 1);
						writer.putSignedByte((byte) 4);
						writer.putSignedShort(connId);

						ClientChannelObjCache.addChannelObj(connId, channelFuture.channel());
						// 发送连接成功消息
						writeData(writer.toBytes());
					} else {
						// 发送连接失败消息
						writer.putSignedByte((byte) 1);
						writer.putSignedByte((byte) 5);
						writer.putSignedShort(connId);

						writeData(writer.toBytes());
					}
				}
			});

		} else if ((byte) 2 == secondFlag) {
			// 发送数据
			byte[] data = new byte[buf.length - 6];
			reader.getBytes(data);
			ChannelObj channelObj = ClientChannelObjCache.getChannelObj(connId);
			if (channelObj == null) {
				return;
			}
			Channel channel = channelObj.getChannel();

			if (channel.isActive()) {
				channel.write(Unpooled.wrappedBuffer(data));
				channel.flush();
			} else {
				closeSocketAndRemove(connId, channel);
				TunnelUtil.sendCloseConn(dragoniteSocket, connId);
			}
			

		} else if ((byte) 3 == secondFlag) {
			// 关闭连接
			ChannelObj channelObj = ClientChannelObjCache.getChannelObj(connId);
			if(channelObj == null) {
				return;
			}
			Channel channel = channelObj.getChannel();
			closeSocketAndRemove(connId, channel);

		}
	}

	/**
	 * 执行除连接的其他操作
	 * 
	 * @param buf
	 * @param reader
	 */
	protected void doExecuteOther(byte[] buf, BinaryReader reader) {

	}

	private void executeRead(byte[] buf) {
		BinaryReader reader = new BinaryReader(buf);
		byte flag = reader.getSignedByte();

		if ((byte) 1 == flag) {
			doExecuteWithConn(buf, reader);
		} else {
			doExecuteOther(buf, reader);
		}

	}

	class SocketClientHandler extends ChannelInboundHandlerAdapter {

	    @Override
	    public void channelActive(ChannelHandlerContext ctx) throws Exception {
	        
	    }

	    @Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
	    	ByteBuf byteBuf = (ByteBuf) msg;
	    	byte[] buf = new byte[byteBuf.readableBytes()];
	    	byteBuf.getBytes(byteBuf.readerIndex(), buf);

			ChannelObj channelObj = ClientChannelObjCache.getChannelObj(ctx.channel());
			if (channelObj != null) {
				TunnelUtil.sendDataBytes(dragoniteSocket, buf, channelObj.getConnId());
			}

			super.channelRead(ctx, msg);
	    }

	    @Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			//System.out.println("连接关闭...");
			closeChannel(ctx);
			super.channelInactive(ctx);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			//System.out.println("发生异常...");
			//closeChannel(ctx);
			super.exceptionCaught(ctx, cause);
			Channel channel = ctx.channel();
			if (channel.isActive())
				ctx.close();
		}

		private void closeChannel(ChannelHandlerContext ctx) {
			ChannelObj channelObj = ClientChannelObjCache.getChannelObj(ctx.channel());
			if (channelObj == null) {
				return;
			}
			//System.out.println("关闭连接...");
			if (ctx.channel().isActive()) {
				ctx.channel().close();
			}
			// 发送关闭连接消息
			TunnelUtil.sendCloseConn(dragoniteSocket, channelObj.getConnId());
			ClientChannelObjCache.removeChannelObj(ctx.channel());
		}
	}

	protected void dragoniteSocketReadFinish() {
		es.shutdownNow(); // 停止所有线程
		ClientChannelObjCache.clear();
		//workerGroup.shutdownGracefully();
	}

	protected void startDragoniteSocketRead() {

		es.execute(() -> {

			byte[] dataBuffer = null; // 数据缓存
			int dataLen = 0; // 数据长度
			int currentLen = 0; // 当前长度

			while (true) {
				try {
					byte[] buf = dragoniteSocket.read();

					int lenByteLen = 0;
					if (dataBuffer == null) {
						lenByteLen = 4;

						byte[] lenByte = new byte[lenByteLen];
						System.arraycopy(buf, 0, lenByte, 0, lenByteLen);

						dataLen = Util.Byte2Int(lenByte);
						dataBuffer = new byte[dataLen]; // 大数据块
						currentLen = 0;

					}

					int currentDataLen = buf.length - lenByteLen;
					System.arraycopy(buf, lenByteLen, dataBuffer, currentLen, currentDataLen);
					currentLen += currentDataLen;

					if (new Integer(currentLen).equals(new Integer(dataLen))) {

						/////
						executeRead(dataBuffer);
						/////

						dataBuffer = null;
					}
				} catch (InterruptedException | ConnectionNotAliveException e) {
					if (dragoniteSocket != null) {
						try {
							dragoniteSocket.closeGracefully();
						} catch (SenderClosedException | InterruptedException | IOException e1) {
							e1.printStackTrace();
						}
					}
					break;
				}
			}
			dragoniteSocketReadFinish();
		});
	}

	public DragoniteSocket getDragoniteSocket() {
		return dragoniteSocket;
	}

}
