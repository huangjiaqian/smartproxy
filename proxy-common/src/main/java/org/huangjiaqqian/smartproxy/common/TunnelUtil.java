package org.huangjiaqqian.smartproxy.common;

import java.io.IOException;
import java.util.Arrays;

import org.huangjiaqqian.smartproxy.common.netty.ChannelObj;
import org.huangjiaqqian.smartproxy.common.netty.ClientChannelObjCache;
import org.huangjiaqqian.smartproxy.common.netty.ServerChannelObjCache;

import com.vecsight.dragonite.sdk.exception.IncorrectSizeException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class TunnelUtil {
	/**
	 * 发送数据块
	 * 
	 * @param buf
	 * @param len
	 * @param connId
	 */
	public static final void sendDataBytes(DragoniteSocket dragoniteSocket, byte[] buf, short connId) {
		sendDataBytes(dragoniteSocket, buf, null, connId);
	}

	/**
	 * 发送数据块
	 * 
	 * @param buf
	 * @param len
	 * @param connId
	 */
	public static final void sendDataBytes(DragoniteSocket dragoniteSocket, byte[] buf, Integer len, short connId) {
		BinaryWriter writer = new BinaryWriter(6 + (len == null ? buf.length : len));
		writer.putSignedByte((byte) 1);
		writer.putSignedByte((byte) 2);
		writer.putSignedShort(connId);
		writer.putBytes(len == null ? buf : Arrays.copyOf(buf, len));
		// 发送数据块
		try {
			Util.writeAndFlush(dragoniteSocket, writer.toBytes());
		} catch (IncorrectSizeException | SenderClosedException | InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 发送关闭连接消息
	 * 
	 * @param connId
	 */
	public static final void sendCloseConn(DragoniteSocket dragoniteSocket, short connId) {
		if (dragoniteSocket == null) {
			return;
		}
		BinaryWriter writer = new BinaryWriter(6);
		writer.putSignedByte((byte) 1);
		writer.putSignedByte((byte) 3);
		writer.putSignedShort(connId);
		try {
			Util.writeAndFlush(dragoniteSocket, writer.toBytes());
		} catch (IncorrectSizeException | SenderClosedException | InterruptedException | IOException e1) {
			e1.printStackTrace();
		}
	}

	public static final void closeAndRemoveChannelObj(DragoniteSocket dragoniteSocket, ChannelObj channelObj, boolean isServerTunnel) {
		if (channelObj == null) {
			return;
		}
		if (channelObj.getChannel().isActive()) {
			channelObj.getChannel().close();
		}
		// 发送关闭连接消息
		TunnelUtil.sendCloseConn(dragoniteSocket, channelObj.getConnId());
		if(isServerTunnel) {
			ServerChannelObjCache.removeChannelObj(channelObj.getChannel());			
		} else {
			ClientChannelObjCache.removeChannelObj(channelObj.getChannel());
		}
	}
	
}
