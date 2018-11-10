package org.huangjiaqqian.smartproxy.p2p.client.config;

import java.io.File;
import java.io.IOException;

import org.huangjiaqqian.smartproxy.common.web.handler.ActionHandler;
import org.huangjiaqqian.smartproxy.p2p.client.web.action.HomeAction;

import net.freeutils.httpserver.HTTPServer;
import net.freeutils.httpserver.HTTPServer.VirtualHost;

public class HttpServerConfig {
	public static final String WEBAPPS_ROOT = "webapps";
	public static final String WEBAPPS_ROOT_PATH = System.getProperty("user.dir") + "/" + WEBAPPS_ROOT + "/";

	public static final void configVirtualHost(HTTPServer httpServer) throws IOException {
		VirtualHost virtualHost = new VirtualHost(VirtualHost.DEFAULT_HOST_NAME);
		//virtualHost.addAlias("localhost");
		virtualHost.setDirectoryIndex("index.html");
		
		// 配置静态资源
		virtualHost.addContext("/", new HTTPServer.FileContextHandler(new File(HttpServerConfig.WEBAPPS_ROOT_PATH)));

		// 配置handler
		
		
		virtualHost.addContext("/home", new ActionHandler(new HomeAction()));
		
		httpServer.addVirtualHost(virtualHost);
	}

}
