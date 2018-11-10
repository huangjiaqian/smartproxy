package org.huangjiaqqian.smartproxy.server.config.web;

import java.io.File;
import java.io.IOException;

import org.huangjiaqqian.smartproxy.common.web.handler.ActionHandler;
import org.huangjiaqqian.smartproxy.server.web.action.HomeAction;
import org.huangjiaqqian.smartproxy.server.web.action.LoginAction;
import org.huangjiaqqian.smartproxy.server.web.action.ToolAction;
import org.huangjiaqqian.smartproxy.server.web.interceptor.PageInterceptor;

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
		PageInterceptor pageInterceptor = new PageInterceptor();
		
		virtualHost.addContext("/home", new ActionHandler(new HomeAction(), pageInterceptor));
		
		virtualHost.addContext("/tool", new ActionHandler(new ToolAction()));
		
		virtualHost.addContext("/login", new ActionHandler(new LoginAction()));
		
		httpServer.addVirtualHost(virtualHost);
	}

}
