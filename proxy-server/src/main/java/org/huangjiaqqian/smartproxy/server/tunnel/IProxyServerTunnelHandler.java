package org.huangjiaqqian.smartproxy.server.tunnel;

import org.huangjiaqqian.smartproxy.common.ServerTunnelHandler;

public interface IProxyServerTunnelHandler extends ServerTunnelHandler {
	public void startListener(ProxyServerTunnel clientTunnel, int port);
	public void closeListener(ProxyServerTunnel clientTunnel, int port);
}
