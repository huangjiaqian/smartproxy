package org.huangjiaqqian.smartproxy.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Map;

import org.huangjiaqqian.smartproxy.common.BinaryReader;
import org.huangjiaqqian.smartproxy.common.BinaryWriter;
import org.huangjiaqqian.smartproxy.common.Util;
import org.huangjiaqqian.smartproxy.common.db.SqliteHelper;
import org.huangjiaqqian.smartproxy.server.cache.ClientPool;
import org.huangjiaqqian.smartproxy.server.config.db.DbConfig;
import org.huangjiaqqian.smartproxy.server.tunnel.ProxyServerTunnel;

import com.vecsight.dragonite.sdk.customer.OtherHandler;
import com.vecsight.dragonite.sdk.exception.IncorrectSizeException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

import cn.hutool.core.convert.Convert;

public class DatagramSocketHandler extends OtherHandler {

	private SqliteHelper sqliteHelper = DbConfig.getDefaultSqliteHelper();

	private void doExecuteRead(BinaryReader reader, DatagramPacket datagramPacket)
			throws IOException, SQLException, IncorrectSizeException, SenderClosedException, InterruptedException {
		InetSocketAddress senderAddr = (InetSocketAddress) datagramPacket.getSocketAddress();
		byte flag = reader.getSignedByte();
		if ((byte) -1 == flag) {
			// ping服务器 （去除前两位占位符返回）
			byte[] data = new byte[datagramPacket.getLength() - 2];
			System.arraycopy(datagramPacket.getData(), 2, data, 0, data.length);
			send(new DatagramPacket(data, data.length, datagramPacket.getSocketAddress()));
		} else if ((byte) -2 == flag) {
			// ping服务器（原样返回）
			send(new DatagramPacket(datagramPacket.getData(), datagramPacket.getLength(),
					datagramPacket.getSocketAddress()));
		} else if ((byte) 1 == flag) {
			// p2p客户端请求穿透内网

			byte[] clientKeyBytes = reader.getBytesGroupWithByteLength();
			String clientKey = new String(clientKeyBytes);

			Map<String, Object> map = sqliteHelper
					.executeQueryForMap("select * from proxy_client where clientKey='" + clientKey + "'");

			ProxyServerTunnel tunnel = ClientPool.NEW_CLIENT_TUNNEL_MAP.get(Convert.toInt(map.get("id")));

			if (tunnel == null) {
				// 客户端未连接（发送失败信息 11）
				byte[] data = new byte[] { (byte) 11 };
				send(new DatagramPacket(data, data.length, senderAddr));

			} else {
				DragoniteSocket dragoniteSocket = tunnel.getDragoniteSocket();
				byte[] addrByte = Util.genAddrBytes(senderAddr.getHostString(), senderAddr.getPort());
				BinaryWriter writer = new BinaryWriter(2 + addrByte.length);
				writer.putSignedByte((byte) 2);
				writer.putBytesGroupWithByteLength(addrByte);
				Util.writeAndFlush(dragoniteSocket, writer.toBytes());
			}

		} else if ((byte) 2 == flag) {
			// 客户端已向p2p客户端发送探测包
			byte[] addrBytes = reader.getBytesGroupWithByteLength();
			InetSocketAddress p2pClientAddress = Util.genAddr(addrBytes);

			byte[] senderBytes = Util.genAddrBytes(senderAddr.getHostString(), senderAddr.getPort());

			BinaryWriter writer = new BinaryWriter(2 + senderBytes.length);

			writer.putSignedByte((byte) 12); // 12成功
			writer.putBytesGroupWithByteLength(senderBytes);
			byte[] data = writer.toBytes();
			send(new DatagramPacket(data, data.length, p2pClientAddress));
		}
	}

	@Override
	protected void receive(DatagramPacket datagramPacket) {
		if (datagramPacket.getLength() < 3) {
			return;
		}
		BinaryReader reader = new BinaryReader(datagramPacket.getData());
		reader.getBytes(new byte[2]); // 忽略前两个字节
		try {
			doExecuteRead(reader, datagramPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
