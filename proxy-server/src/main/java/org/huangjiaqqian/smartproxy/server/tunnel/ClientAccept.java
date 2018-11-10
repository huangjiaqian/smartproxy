package org.huangjiaqqian.smartproxy.server.tunnel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.huangjiaqqian.smartproxy.common.db.SqliteHelper;
import org.huangjiaqqian.smartproxy.server.cache.ClientPool;
import org.huangjiaqqian.smartproxy.server.config.db.DbConfig;

import com.vecsight.dragonite.sdk.exception.ConnectionNotAliveException;
import com.vecsight.dragonite.sdk.exception.IncorrectSizeException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

import cn.hutool.core.convert.Convert;

public class ClientAccept {

	private SqliteHelper sqliteHelper = DbConfig.getDefaultSqliteHelper();

	private DragoniteSocket dragoniteSocket;

	private static ProxyServerTunnelHandler tunnelHandler = new ProxyServerTunnelHandler();

	public ClientAccept(DragoniteSocket dragoniteSocket) {
		super();
		this.dragoniteSocket = dragoniteSocket;
	}


	public void run() {

		try {
			byte[] keyByte = dragoniteSocket.read();
			String clientKey = new String(keyByte);
			
			Map<String, Object> map = sqliteHelper
					.executeQueryForMap("select * from proxy_client where clientKey='" + clientKey + "'");

			if (map == null) {
				dragoniteSocket.send(("false:key值（" + clientKey + "）不存在或已被修改").getBytes());

				dragoniteSocket.closeGracefully();
				return;
			} else {
				Integer clientId = Convert.toInt(map.get("id"));
				ProxyServerTunnel tunnel = ClientPool.NEW_CLIENT_TUNNEL_MAP.get(clientId);
				
				if(tunnel != null) {
					dragoniteSocket.send("false:已存在已登录的用户".getBytes());
					try {
						dragoniteSocket.closeGracefully();
					} catch (SenderClosedException | InterruptedException | IOException e1) {
						e1.printStackTrace();
					}
					return;
				}
				
				dragoniteSocket.send("true".getBytes());
				sqliteHelper.executeUpdate("update proxy_client set status='online' where id='" + clientId + "'");

				tunnel = new ProxyServerTunnel(dragoniteSocket, tunnelHandler, clientId);

				List<Map<String, Object>> list = sqliteHelper.executeQueryForList(
						"select * from proxy_client_config where proxy_client_id = '" + clientId + "'");
				if (list != null && !list.isEmpty()) {
					for (Map<String, Object> mapTemp : list) {

						try {
							tunnel.startSocketListener(Convert.toInt(mapTemp.get("inetPort")),
									new InetSocketAddress(Convert.toStr(mapTemp.get("lanHost")),
											Convert.toInt(mapTemp.get("lanPort"))));
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}
			}

		} catch (ConnectionNotAliveException | InterruptedException | SQLException | IncorrectSizeException
				| SenderClosedException | IOException e) {
			e.printStackTrace();

			try {
				dragoniteSocket.closeGracefully();
			} catch (SenderClosedException | InterruptedException | IOException e1) {
				e1.printStackTrace();
			}
			return;
		}

	}

}
