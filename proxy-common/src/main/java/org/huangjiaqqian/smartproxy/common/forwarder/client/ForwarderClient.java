package org.huangjiaqqian.smartproxy.common.forwarder.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.huangjiaqqian.smartproxy.common.BinaryReader;
import org.huangjiaqqian.smartproxy.common.ServerTunnel;
import org.huangjiaqqian.smartproxy.common.netty.NettyTcpServer;

import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class ForwarderClient extends ServerTunnel {

	private Map<Integer, NettyTcpServer> nettyServerMap = new ConcurrentHashMap<>();

	private InetSocketAddress remoteAddr;

	private boolean active;
	
	public ForwarderClient(DragoniteSocket dragoniteSocket) throws IOException {
		this.dragoniteSocket = dragoniteSocket;
		remoteAddr = (InetSocketAddress) dragoniteSocket.getRemoteSocketAddress();
		active = true;

		startDragoniteSocketRead();
	}
	

	public void startSocketListener(int bindPort, InetSocketAddress natAddr) throws IOException, InterruptedException {
		if (!active) {
			return;
		}
		NettyTcpServer nettyServer = new NettyTcpServer(dragoniteSocket, bindPort, natAddr, channelObjCache);
		nettyServer.start();
		nettyServerMap.put(bindPort, nettyServer);
	}

	public void closeListener(int port) {
		NettyTcpServer nettyServer = nettyServerMap.get(port);
		
		if (nettyServer != null) {
			nettyServer.close();
		}

		nettyServerMap.remove(port);

	}

	/**
	 * 连接已关闭
	 */
	@Override
	protected void dragoniteSocketReadFinish() {
		super.dragoniteSocketReadFinish();

		for (int port : nettyServerMap.keySet()) {
			closeListener(port); // 停止所有监听
		}

		try {
			dragoniteSocket.closeGracefully();
		} catch (SenderClosedException | InterruptedException | IOException e) {
			// e.printStackTrace();
		}
		dragoniteSocket = null;
		active = false;

		System.err.println("连接：" + remoteAddr + " 已关闭！");
	}

	@Override
	protected void doExecuteOther(byte[] buf, BinaryReader reader) {

		super.doExecuteOther(buf, reader);
	}

	public boolean isActive() {
		return active;
	}


}
