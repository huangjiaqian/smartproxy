package org.huangjiaqqian.smartproxy.common.forwarder.server;

import java.net.SocketException;

import com.vecsight.dragonite.sdk.config.DragoniteSocketParameters;
import com.vecsight.dragonite.sdk.socket.DragoniteServer;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class ForwarderServer {

	private DragoniteServer dragoniteServer;
	
	private Thread acceptThread;
	
	public ForwarderServer(int bindPort) throws SocketException {
		super();
		this.dragoniteServer = new DragoniteServer(bindPort, 1024 * 1024, new DragoniteSocketParameters());
		forwarderServerAccept();
	}
	
	private void forwarderServerAccept() {
		acceptThread = new Thread(() -> {
			DragoniteSocket dragoniteSocket = null;
			try {
				while ((dragoniteSocket = dragoniteServer.accept()) != null) {
					new ForwarderClientHandler(dragoniteSocket);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}, "FS-DS-ACCEPT");
		acceptThread.start();
	}
	
}
