package org.huangjiaqqian.smartproxy.server.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.huangjiaqqian.smartproxy.server.tunnel.ProxyServerTunnel;

public class ClientPool {
	public static final Map<Integer, ProxyServerTunnel> NEW_CLIENT_TUNNEL_MAP = new ConcurrentHashMap<>();
}
