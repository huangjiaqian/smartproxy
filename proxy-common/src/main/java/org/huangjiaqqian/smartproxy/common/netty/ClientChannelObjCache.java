package org.huangjiaqqian.smartproxy.common.netty;

import io.netty.channel.Channel;

public class ClientChannelObjCache extends BaseChannelObjCache {
	
	public ClientChannelObjCache() {
		
	}
	
	public synchronized final ChannelObj addChannelObj(Short connId, Channel channel) {
		ChannelObj channelObj = ChannelObj.genChannelObjWithConnId(channel, connId);
		connIdToChannelObjMap.put(channelObj.getConnId(), channelObj);
		channelToChannelObjMap.put(channel, channelObj);
		return channelObj;
	}
	
}
