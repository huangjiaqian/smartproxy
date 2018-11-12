package org.huangjiaqqian.smartproxy.server.tunnel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.huangjiaqqian.smartproxy.common.BinaryReader;
import org.huangjiaqqian.smartproxy.common.ServerTunnel;
import org.huangjiaqqian.smartproxy.common.db.SqliteHelper;
import org.huangjiaqqian.smartproxy.common.netty.NettyTcpServer;
import org.huangjiaqqian.smartproxy.server.config.db.DbConfig;

import com.vecsight.dragonite.sdk.config.DragoniteSocketParameters;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteServer;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

import cn.hutool.core.convert.Convert;

public class ProxyServerTunnel extends ServerTunnel {

	private Integer proxyClientId;

	private Map<Integer, NettyTcpServer> nettyServerMap = new ConcurrentHashMap<>();

	private InetSocketAddress remoteAddr;

	private boolean active;

	private static boolean isStartStatisticsFlow = false;
	
	public ProxyServerTunnel(DragoniteSocket dragoniteSocket) throws IOException {
		this.dragoniteSocket = dragoniteSocket;
		remoteAddr = (InetSocketAddress) dragoniteSocket.getRemoteSocketAddress();
		active = true;

		statisticsFlow();// 开始流量统计

		startDragoniteSocketRead();
	}
	
	public ProxyServerTunnel(DragoniteSocket dragoniteSocket, IProxyServerTunnelHandler tunnelHandler,
			Integer proxyClientId) throws IOException {
		this(dragoniteSocket);
		this.tunnelHandler = tunnelHandler;
		this.proxyClientId = proxyClientId;

		
		if (tunnelHandler != null) {
			tunnelHandler.start(this);
		}
		
	}

	/**
	 * 流量统计
	 */
	public static void statisticsFlow() {
		if (isStartStatisticsFlow) {
			return;
		}
		isStartStatisticsFlow = true;

		upFlowMap = new ConcurrentHashMap<Integer, Double>(); // 上行流量
		downFlowMap = new ConcurrentHashMap<Integer, Double>(); // 下行流量
		final SqliteHelper sqliteHelper = DbConfig.getDefaultSqliteHelper();

		try {
			List<Map<String, Object>> list = sqliteHelper.executeQueryForList("select * from proxy_client_config");
			if(list != null && !list.isEmpty()) {
				for (Map<String, Object> map : list) {
					Integer port = Convert.toInt(map.get("inetPort"));
					Double upFlow = Convert.toDouble(map.get("upFlow"), 0D);
					Double downFlow = Convert.toDouble(map.get("downFlow"), 0D);
					upFlowMap.put(port, upFlow);
					downFlowMap.put(port, downFlow);
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
				
		
		ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
		ses.scheduleWithFixedDelay(() -> {
			for (Integer port : upFlowMap.keySet()) {

				try {
					StringBuilder sb = new StringBuilder("update proxy_client_config set ");
					boolean hasDownFlow = false;
					if (upFlowMap.get(port) != null) {
						hasDownFlow = true;
						sb.append(" upFlow='").append(upFlowMap.get(port)).append("' ");
					}

					if (downFlowMap.get(port) != null) {
						sb.append(hasDownFlow ? "," : "").append(" downFlow='").append(downFlowMap.get(port))
								.append("' ");
					}

					sb.append("where inetPort='").append(port).append("'");

					sqliteHelper.executeUpdate(sb.toString());
				} catch (SQLException e) {
					e.printStackTrace();
				}

			}

		}, 120, 120, TimeUnit.SECONDS); //2分钟统计一次
	}




	public void startSocketListener(int bindPort, InetSocketAddress natAddr) throws IOException, InterruptedException {
		if (!active) {
			return;
		}
		NettyTcpServer nettyServer = new NettyTcpServer(dragoniteSocket, bindPort, natAddr, channelObjCache);
		nettyServer.start();
		nettyServerMap.put(bindPort, nettyServer);
		
		if (tunnelHandler != null) {
			((ProxyServerTunnelHandler) tunnelHandler).startListener(this, bindPort);
		}
		
	}

	public void closeListener(int port) {
		NettyTcpServer nettyServer = nettyServerMap.get(port);
		
		if (nettyServer != null) {
			nettyServer.close();
		}

		nettyServerMap.remove(port);

		if (tunnelHandler != null) {
			((ProxyServerTunnelHandler) tunnelHandler).closeListener(this, port);
		}

	}

	/**
	 * 连接已关闭
	 */
	@Override
	protected void dragoniteSocketReadFinish() {
		super.dragoniteSocketReadFinish();

		for (int port : nettyServerMap.keySet()) {
			closeListener(port); // 停止所有监听
		}

		try {
			dragoniteSocket.closeGracefully();
		} catch (SenderClosedException | InterruptedException | IOException e) {
			// e.printStackTrace();
		}
		dragoniteSocket = null;
		active = false;

		System.err.println("连接：" + remoteAddr + " 已关闭！");
	}

	@Override
	protected void doExecuteOther(byte[] buf, BinaryReader reader) {

		super.doExecuteOther(buf, reader);
	}

	public boolean isActive() {
		return active;
	}

	public Integer getProxyClientId() {
		return proxyClientId;
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		DragoniteServer server = new DragoniteServer(11001, 1024 * 1024, new DragoniteSocketParameters());
		DragoniteSocket socket = server.accept();
		ProxyServerTunnel clientTunnel = new ProxyServerTunnel(socket);
		clientTunnel.startSocketListener(8888, new InetSocketAddress("nas.hjq.com", 5000));
	}
}
