package org.huangjiaqqian.smartproxy.p2p.client.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nnat.dragonite.forwarder.network.client.ForwarderClient;

public class ConfigPool {
	public static final Map<Integer, ForwarderClient> FORWARDER_CLIENT_MAP = new ConcurrentHashMap<>();
}
