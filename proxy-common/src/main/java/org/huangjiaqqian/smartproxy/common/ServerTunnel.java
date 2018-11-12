package org.huangjiaqqian.smartproxy.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import org.huangjiaqqian.smartproxy.common.netty.ChannelObj;
import org.huangjiaqqian.smartproxy.common.netty.ServerChannelObjCache;

import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

import io.netty.buffer.Unpooled;

public class ServerTunnel extends BaseTunnel {

	public static Map<Integer, Double> upFlowMap = null;

	public static Map<Integer, Double> downFlowMap = null;

	protected ServerTunnelHandler tunnelHandler;
	
	protected ServerChannelObjCache channelObjCache;

	public ServerTunnel() throws IOException {
		super();
		this.channelObjCache = new ServerChannelObjCache();
	}

	protected void doExecuteWithConn(byte[] buf, BinaryReader reader) {
		byte secondFlag = reader.getSignedByte();
		short connId = reader.getSignedShort();

		if ((byte) 1 == secondFlag) {
			// 创建连接

		} else if ((byte) 2 == secondFlag) {
			// 发送数据
			byte[] data = new byte[buf.length - 6];
			reader.getBytes(data);
			ChannelObj channelObj = channelObjCache.getChannelObj(connId);

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
				TunnelUtil.closeAndRemoveChannelObj(dragoniteSocket, channelObj, channelObjCache);
			}

		} else if ((byte) 3 == secondFlag) {
			// 关闭连接
			ChannelObj channelObj = channelObjCache.getChannelObj(connId);
			if(channelObj != null) {
				TunnelUtil.closeAndRemoveChannelObj(dragoniteSocket, channelObj, channelObjCache);				
			}
		} else if ((byte) 4 == secondFlag) {
			// 成功创建连接
			ChannelObj channelObj = channelObjCache.getChannelObj(connId);
			if(channelObj != null) {
				channelObj.getChannel().config().setAutoRead(true); //开始读数据				
			}
		} else if ((byte) 5 == secondFlag) {
			// 创建连接失败
			ChannelObj channelObj = channelObjCache.getChannelObj(connId);
			if(channelObj != null) {				
				TunnelUtil.closeAndRemoveChannelObj(dragoniteSocket, channelObj, channelObjCache);
			}
		}
	}


	protected void dragoniteSocketReadFinish() {
		es.shutdownNow(); // 停止所有线程

		if (tunnelHandler != null) {
			tunnelHandler.close(this);
		}

	}

	public DragoniteSocket getDragoniteSocket() {
		return dragoniteSocket;
	}

}
