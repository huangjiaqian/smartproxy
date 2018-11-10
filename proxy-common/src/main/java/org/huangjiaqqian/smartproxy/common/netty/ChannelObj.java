package org.huangjiaqqian.smartproxy.common.netty;

import io.netty.channel.Channel;

/**
 * 连接通道对象
 * @author 黄钱钱
 *
 */
public class ChannelObj {
	public static final ChannelObj genChannelObj(Channel channel) {
		return new ChannelObj(channel, getConnID());
	}
	
	public static final ChannelObj genChannelObjWithConnId(Channel channel, Short connId) {
		return new ChannelObj(channel, connId);
	}
	
	private ChannelObj(Channel channel, Short connId) {
		super();
		this.channel = channel;
		this.connId = connId;
	}


	private Channel channel;
	private Short connId;
	
	public Channel getChannel() {
		return channel;
	}
	public Short getConnId() {
		return connId;
	}
	private static short connID = 1;

	private static synchronized short getConnID() {
		connID++;
		if (connID > Short.MAX_VALUE - 2) {
			connID = 1;
		}
		return connID;
	}
}
