package org.huangjiaqqian.smartproxy.server;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.huangjiaqqian.smartproxy.common.Util;
import org.huangjiaqqian.smartproxy.server.config.db.DbConfig;
import org.huangjiaqqian.smartproxy.server.config.web.HttpServerConfig;
import org.huangjiaqqian.smartproxy.server.tunnel.ClientAccept;

import com.vecsight.dragonite.sdk.config.DragoniteSocketParameters;
import com.vecsight.dragonite.sdk.socket.DragoniteServer;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

import cn.hutool.core.convert.Convert;
import net.freeutils.httpserver.HTTPServer;

public class ProxyServerContainer {

	private DragoniteServer dragoniteServer;
	private HTTPServer httpServer;
	private Thread acceptThread;

	public ProxyServerContainer(int tunnelPort, int httpPort) throws InterruptedException, IOException {
		DragoniteSocketParameters dragoniteSocketParameters = new DragoniteSocketParameters();

		dragoniteSocketParameters.setOtherHandler(new DatagramSocketHandler());

		dragoniteServer = new DragoniteServer(tunnelPort, 1024 * 1024, dragoniteSocketParameters);
		httpServer = new HTTPServer(httpPort);
		HttpServerConfig.configVirtualHost(httpServer);
		
		System.out.println("listener tunnelPort:" + tunnelPort);
		System.out.println("listener httpPort:" + httpPort);
	}

	public void start() throws IOException, SQLException {
		DbConfig.initSql();

		tunnelAccept();

		httpServer.start();
	}

	private void tunnelAccept() {
		acceptThread = new Thread(() -> {
			DragoniteSocket dragoniteSocket = null;
			try {
				while ((dragoniteSocket = dragoniteServer.accept()) != null) {
					new Thread(new ClientAccept(dragoniteSocket)::run).start();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}, "PS-DS-ACCEPT");
		acceptThread.start();
	}

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, SQLException {
		Map<String, String> paramMap = Util.argsToMap(args);
		int defaultTunnelPort = 12222;
		int defaultHttpPort = 8088;
		ProxyServerContainer container = new ProxyServerContainer(
				Convert.toInt(paramMap.get("tunnelPort"), defaultTunnelPort),
				Convert.toInt(paramMap.get("httpPort"), defaultHttpPort));
		
		container.start();
	}
}
