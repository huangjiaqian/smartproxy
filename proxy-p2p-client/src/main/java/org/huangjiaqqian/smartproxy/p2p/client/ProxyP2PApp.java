package org.huangjiaqqian.smartproxy.p2p.client;

import java.io.IOException;

import org.huangjiaqqian.smartproxy.p2p.client.config.DbConfig;
import org.huangjiaqqian.smartproxy.p2p.client.config.HttpServerConfig;

import cn.hutool.core.convert.Convert;
import net.freeutils.httpserver.HTTPServer;

/**
 * Hello world!
 *
 */
public class ProxyP2PApp {
	private HTTPServer httpServer;
	
	public ProxyP2PApp(Integer httpPort) throws IOException {
		httpServer = new HTTPServer(httpPort);
		HttpServerConfig.configVirtualHost(httpServer);		
	}
	public void start() throws Exception {
		DbConfig.initSql();
		httpServer.start();
	}
	
	public static void main(String[] args) throws Exception {
		int httpPort = 1995;
		if(args != null && args.length > 0) {
			httpPort = Convert.toInt(args[0]);
		}
		ProxyP2PApp app = new ProxyP2PApp(httpPort);
		app.start();
		/*
		Map<String, String> paramMap = Util.argsToMap(args);

		if (paramMap.get("clientKey") == null) {
			//return;
		}

		String clientKey = Convert.toStr(paramMap.get("clientKey"), "0b42eb00f8a74fe8a39682c71f8e117720181107");
		String remoteHost = Convert.toStr(paramMap.get("remoteHost"), "candytang.cn");
		int remotePort = Convert.toInt(paramMap.get("remotePort"), 12222);

		String natHost = Convert.toStr(paramMap.get("natHost"), "nas.hjq.com");
		int natPort = Convert.toInt(paramMap.get("natPort"), 5000);
		int localPort = Convert.toInt(paramMap.get("localPort"), 7070);

		int downMbps = Convert.toInt(paramMap.get("downMbps"), 12);
		int upMbps = Convert.toInt(paramMap.get("upMbps"), 12);

		ForwarderClientConfig config = new ForwarderClientConfig(new InetSocketAddress(remoteHost, remotePort),
				new InetSocketAddress(natHost, natPort), localPort, downMbps, upMbps);
		ForwarderClientGetter getter = new ForwarderClientGetter(config, clientKey);
		getter.getForwarderClient();
		*/
	}
}
