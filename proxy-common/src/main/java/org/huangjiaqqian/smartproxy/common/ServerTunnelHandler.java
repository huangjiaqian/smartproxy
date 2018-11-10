package org.huangjiaqqian.smartproxy.common;

public interface ServerTunnelHandler {
	public void start(ServerTunnel tunnelClient);
	public void close(ServerTunnel tunnelClient);
}
