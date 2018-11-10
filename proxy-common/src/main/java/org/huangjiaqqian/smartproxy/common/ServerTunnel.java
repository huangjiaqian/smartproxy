package org.huangjiaqqian.smartproxy.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.huangjiaqqian.smartproxy.common.netty.ChannelObj;
import org.huangjiaqqian.smartproxy.common.netty.ServerChannelObjCache;

import com.vecsight.dragonite.sdk.exception.ConnectionNotAliveException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

import io.netty.buffer.Unpooled;

public class ServerTunnel {

	public static Map<Integer, Double> upFlowMap = null;

	public static Map<Integer, Double> downFlowMap = null;

	protected DragoniteSocket dragoniteSocket;

	protected ExecutorService es = Executors.newCachedThreadPool();

	protected ServerTunnelHandler tunnelHandler;

	public ServerTunnel() throws IOException {
		super();
	}

	private void doExecuteWithConn(byte[] buf, BinaryReader reader) {
		byte secondFlag = reader.getSignedByte();
		short connId = reader.getSignedShort();

		if ((byte) 1 == secondFlag) {
			// 创建连接

		} else if ((byte) 2 == secondFlag) {
			// 发送数据
			byte[] data = new byte[buf.length - 6];
			reader.getBytes(data);
			ChannelObj channelObj = ServerChannelObjCache.getChannelObj(connId);

			if (channelObj == null) {
				return;
			}

			if (channelObj.getChannel().isActive()) {

				channelObj.getChannel().write(Unpooled.wrappedBuffer(data));
				channelObj.getChannel().flush();
				if (downFlowMap != null) {
					// 下载流量统计
					Integer currentPort = ((InetSocketAddress) channelObj.getChannel().localAddress()).getPort(); // 绑定的端口
					Double downFlow = downFlowMap.get(currentPort);
					downFlow = downFlow == null ? data.length : (downFlow + data.length);
					downFlowMap.put(currentPort, downFlow);
				}
			} else {
				TunnelUtil.closeAndRemoveChannelObj(dragoniteSocket, channelObj, true);
			}

		} else if ((byte) 3 == secondFlag) {
			// 关闭连接
			ChannelObj channelObj = ServerChannelObjCache.getChannelObj(connId);
			if(channelObj != null) {
				TunnelUtil.closeAndRemoveChannelObj(dragoniteSocket, channelObj, true);				
			}
		} else if ((byte) 4 == secondFlag) {
			// 成功创建连接
			ChannelObj channelObj = ServerChannelObjCache.getChannelObj(connId);
			if(channelObj != null) {
				channelObj.getChannel().config().setAutoRead(true); //开始读数据				
			}
		} else if ((byte) 5 == secondFlag) {
			// 创建连接失败
			ChannelObj channelObj = ServerChannelObjCache.getChannelObj(connId);
			if(channelObj != null) {				
				TunnelUtil.closeAndRemoveChannelObj(dragoniteSocket, channelObj, true);
			}
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

	protected void dragoniteSocketReadFinish() {
		es.shutdownNow(); // 停止所有线程

		if (tunnelHandler != null) {
			tunnelHandler.close(this);
		}

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
