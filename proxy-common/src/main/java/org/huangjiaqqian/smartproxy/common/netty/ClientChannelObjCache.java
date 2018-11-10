package org.huangjiaqqian.smartproxy.common.netty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;

public class ClientChannelObjCache {
	
	private static final Map<Short, ChannelObj> CONNID_TO_CHANNEL_OBJ_MAP = new ConcurrentHashMap<>(); //连接对象
	
	private static final Map<Channel, ChannelObj> CHANNEL_TO_CHANNEL_OBJ_MAP = new ConcurrentHashMap<>(); //连接对象
	
	public synchronized static final ChannelObj getChannelObj(Channel channel) {
		return CHANNEL_TO_CHANNEL_OBJ_MAP.get(channel);
	}
	
	public synchronized static final ChannelObj getChannelObj(Short connId) {
		return CONNID_TO_CHANNEL_OBJ_MAP.get(connId);
	}
	
	public synchronized static final void removeChannelObj(Short connId) {
		ChannelObj channelObj = CONNID_TO_CHANNEL_OBJ_MAP.get(connId);
		if(channelObj == null) {
			return;
		}
		CONNID_TO_CHANNEL_OBJ_MAP.remove(connId);
		CHANNEL_TO_CHANNEL_OBJ_MAP.remove(channelObj.getChannel());
	}
	public synchronized static final void removeChannelObj(Channel channel) {
		ChannelObj channelObj = CHANNEL_TO_CHANNEL_OBJ_MAP.get(channel);
		if(channelObj == null) {
			return;
		}
		CONNID_TO_CHANNEL_OBJ_MAP.remove(channelObj.getConnId());
		CHANNEL_TO_CHANNEL_OBJ_MAP.remove(channel);
	}
	
	public synchronized static final ChannelObj addChannelObj(Short connId, Channel channel) {
		ChannelObj channelObj = ChannelObj.genChannelObjWithConnId(channel, connId);
		CONNID_TO_CHANNEL_OBJ_MAP.put(channelObj.getConnId(), channelObj);
		CHANNEL_TO_CHANNEL_OBJ_MAP.put(channel, channelObj);
		return channelObj;
	}
	public synchronized static final void clear() {
		CONNID_TO_CHANNEL_OBJ_MAP.clear();
		for(Channel channel: CHANNEL_TO_CHANNEL_OBJ_MAP.keySet()) {
			if(channel.isActive()) 
				channel.close();
		}
	}
}
