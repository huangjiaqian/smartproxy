package org.huangjiaqqian.smartproxy.common.netty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;

public class BaseChannelObjCache {
	
	protected final Map<Short, ChannelObj> connIdToChannelObjMap = new ConcurrentHashMap<>(); //连接对象
	
	protected final Map<Channel, ChannelObj> channelToChannelObjMap = new ConcurrentHashMap<>(); //连接对象
	
	public synchronized final ChannelObj getChannelObj(Channel channel) {
		return channelToChannelObjMap.get(channel);
	}
	
	public synchronized final ChannelObj getChannelObj(Short connId) {
		return connIdToChannelObjMap.get(connId);
	}
	
	public synchronized final void removeChannelObj(Short connId) {
		ChannelObj channelObj = connIdToChannelObjMap.get(connId);
		if(channelObj == null) {
			return;
		}
		connIdToChannelObjMap.remove(connId);
		channelToChannelObjMap.remove(channelObj.getChannel());
	}
	public synchronized final void removeChannelObj(Channel channel) {
		ChannelObj channelObj = channelToChannelObjMap.get(channel);
		if(channelObj == null) {
			return;
		}
		connIdToChannelObjMap.remove(channelObj.getConnId());
		channelToChannelObjMap.remove(channel);
	}
	
	public synchronized final void clear() {
		for(Channel channel: channelToChannelObjMap.keySet()) {
			if(channel.isActive()) 
				channel.close();
		}
		connIdToChannelObjMap.clear();
		channelToChannelObjMap.clear();
	}
}
