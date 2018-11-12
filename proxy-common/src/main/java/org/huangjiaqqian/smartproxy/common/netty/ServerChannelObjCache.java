package org.huangjiaqqian.smartproxy.common.netty;

import io.netty.channel.Channel;

public class ServerChannelObjCache extends BaseChannelObjCache {
	
	public synchronized final ChannelObj addChannelObj(Channel channel) {
		ChannelObj channelObj = ChannelObj.genChannelObj(channel);
		connIdToChannelObjMap.put(channelObj.getConnId(), channelObj);
		channelToChannelObjMap.put(channel, channelObj);
		return channelObj;
	}
}
