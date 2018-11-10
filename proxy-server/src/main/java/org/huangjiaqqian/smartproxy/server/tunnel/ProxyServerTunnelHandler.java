package org.huangjiaqqian.smartproxy.server.tunnel;

import java.sql.SQLException;

import org.huangjiaqqian.smartproxy.common.ServerTunnel;
import org.huangjiaqqian.smartproxy.common.db.SqliteHelper;
import org.huangjiaqqian.smartproxy.server.cache.ClientPool;
import org.huangjiaqqian.smartproxy.server.config.db.DbConfig;
import org.pmw.tinylog.Logger;

public class ProxyServerTunnelHandler implements IProxyServerTunnelHandler {
	
	private SqliteHelper sqliteHelper = DbConfig.getDefaultSqliteHelper();

	
	@Override
	public void startListener(ProxyServerTunnel clientTunnel, int port) {
		try {
			sqliteHelper.executeUpdate(
					"update proxy_client_config set enable='true' where proxy_client_id='" + clientTunnel.getProxyClientId() + "' and inetPort='" + port + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		Logger.info("监听已启动：" + clientTunnel.getProxyClientId() + "  " + port);
	}

	@Override
	public void closeListener(ProxyServerTunnel clientTunnel, int port) {
		Logger.info("监听已关闭：" + clientTunnel.getProxyClientId() + "  " + port);
	}

	// 客户端连接开启
	@Override
	public void start(ServerTunnel baseTunnelClient) {
		ProxyServerTunnel clientTunnel = (ProxyServerTunnel) baseTunnelClient;
		ClientPool.NEW_CLIENT_TUNNEL_MAP.put(clientTunnel.getProxyClientId(), clientTunnel);

		Logger.info("已连接:" + clientTunnel.getProxyClientId());
	}

	// 客户端断开连接
	@Override
	public void close(ServerTunnel baseTunnelClient) {
		ProxyServerTunnel clientTunnel = (ProxyServerTunnel) baseTunnelClient;
		ClientPool.NEW_CLIENT_TUNNEL_MAP.remove(clientTunnel.getProxyClientId());

		try {
			sqliteHelper.executeUpdate(
					"update proxy_client set status='offline' where id='" + clientTunnel.getProxyClientId() + "'");
			sqliteHelper.executeUpdate(
					"update proxy_client_config set enable='false' where proxy_client_id='" + clientTunnel.getProxyClientId() + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}

		Logger.info("连接已断开:" + clientTunnel.getProxyClientId());

		clientTunnel = null;
	}

	

}
