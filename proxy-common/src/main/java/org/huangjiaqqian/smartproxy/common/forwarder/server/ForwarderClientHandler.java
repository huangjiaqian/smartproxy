package org.huangjiaqqian.smartproxy.common.forwarder.server;

import org.huangjiaqqian.smartproxy.common.ClientTunnel;

import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class ForwarderClientHandler extends ClientTunnel {
	
	public ForwarderClientHandler(DragoniteSocket dragoniteSocket) {
		super();
		super.dragoniteSocket = dragoniteSocket;
		
		// TODO 对连接的客户端进行判断
		
		startDragoniteSocketRead();
	}

	@Override
	protected void dragoniteSocketReadFinish() {
		super.dragoniteSocketReadFinish();
		super.workerGroup.shutdownGracefully();
	}
	
}
