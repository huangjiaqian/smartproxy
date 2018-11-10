package org.huangjiaqqian.smartproxy.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.Executors;

import org.huangjiaqqian.smartproxy.common.BinaryReader;
import org.huangjiaqqian.smartproxy.common.BinaryWriter;
import org.huangjiaqqian.smartproxy.common.ClientTunnel;
import org.huangjiaqqian.smartproxy.common.Util;
import org.nnat.dragonite.forwarder.config.ForwarderServerConfig;
import org.nnat.dragonite.forwarder.network.server.ForwarderServer;
import org.pmw.tinylog.Logger;

import com.vecsight.dragonite.sdk.config.DragoniteSocketParameters;
import com.vecsight.dragonite.sdk.exception.ConnectionNotAliveException;
import com.vecsight.dragonite.sdk.exception.IncorrectSizeException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteClientSocket;

import cn.hutool.core.convert.Convert;

public class ProxyClient extends ClientTunnel {

	boolean isFirstInit = true;

	private InetSocketAddress remoteAddr;

	private InetSocketAddress forwarderServerAddr;

	private String clientKey;

	private ForwarderServer forwarderServer;
	
	private static int sendSpeed = 1024 * 1024;

	public ProxyClient(InetSocketAddress remoteAddr, String clientKey, InetSocketAddress forwarderServerAddr)
			throws Exception {
		super();
		this.remoteAddr = remoteAddr;
		this.clientKey = clientKey;
		this.forwarderServerAddr = forwarderServerAddr;

		startForwarderServer(this.forwarderServerAddr); // 开启内网转发服务

		doStart(remoteAddr, clientKey);
		isFirstInit = false;
	}

	@Override
	protected void doExecuteOther(byte[] buf, BinaryReader reader) {
		byte flag = buf[0];
		if (flag == 2) {
			// p2p客户端请求连接
			byte[] addrBytes = reader.getBytesGroupWithByteLength();
			InetSocketAddress p2pClientAddress = Util.genAddr(addrBytes);

			if (forwarderServer == null) {
				return;
			}

			DatagramSocket datagramSocket = Util.getDatagramSocket(forwarderServer);
			byte[] data = new byte[] { (byte) -1 };
			try {
				// 为提高成功率，发送多个端口
				for (int i = 0; i < 1; i++) {
					datagramSocket.send(new DatagramPacket(data, data.length, p2pClientAddress.getAddress(),
							p2pClientAddress.getPort() + i));
				}

				BinaryWriter writer = new BinaryWriter(2 + 2 + addrBytes.length);
				writer.putBytes(new byte[] { (byte) 4, (byte) 4, (byte) 2 });
				writer.putBytesGroupWithByteLength(addrBytes);
				data = writer.toBytes();
				datagramSocket
						.send(new DatagramPacket(data, data.length, remoteAddr.getAddress(), remoteAddr.getPort()));

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		super.doExecuteOther(buf, reader);
	}

	private void startForwarderServer(InetSocketAddress forwarderServerAddr) {
		final ForwarderServerConfig forwarderServerConfig = new ForwarderServerConfig(forwarderServerAddr);
		new Thread(() -> {
			try {
				forwarderServer = new ForwarderServer(forwarderServerConfig);
			} catch (SocketException e) {
				//e.printStackTrace();
			}
		}).start();
	}

	private void doStart(InetSocketAddress remoteAddr, String clientKey) throws Exception {
		dragoniteSocket = new DragoniteClientSocket(remoteAddr, sendSpeed, new DragoniteSocketParameters());

		try {
			dragoniteSocket.send(clientKey.getBytes());

			byte[] successByte = dragoniteSocket.read();
			String success = new String(successByte);
			if (!"true".equals(success)) {
				dragoniteSocket.closeGracefully();
				throw new SocketException(success);
			}

		} catch (IncorrectSizeException | SenderClosedException | InterruptedException | IOException
				| ConnectionNotAliveException e) {
			e.printStackTrace();
			try {
				dragoniteSocket.closeGracefully();
			} catch (SenderClosedException | InterruptedException | IOException e1) {
				e1.printStackTrace();
			}
			throw e;
		}

		if (!isFirstInit) { // 重新连接
			es = Executors.newCachedThreadPool();
		}

		startDragoniteSocketRead();
	}

	private boolean canConnect = false;
	private boolean connectError = false;

	/**
	 * 重新连接
	 */
	private void reConnect() {
		try {
			canConnect = false; // 是否可连接
			connectError = false;
			Logger.info("重新连接...");
			final DatagramSocket socket = new DatagramSocket();
			new Thread(() -> {
				DatagramPacket packet = null;
				byte[] buf = null;
				while (true) {
					buf = new byte[1024];
					packet = new DatagramPacket(buf, buf.length);
					try {
						socket.receive(packet);
						if (packet.getData()[0] == (byte) -1) {
							// 连接可用

							Logger.info("正在连接...");
							try {
								doStart(remoteAddr, clientKey);								
							} catch (Exception e) {
								e.printStackTrace();
								Logger.error(e.getMessage());
								Logger.info("再次尝试连接...");
								continue;
							}
							

							// TODO 执行启动
							canConnect = true;
							socket.close();

							break;
						}
					} catch (IOException e) {
						e.printStackTrace();
						connectError = true;
						break;
					}
				}
			}).start();

			while (true) {
				if (canConnect || connectError) {

					break;
				}
				// 间隔5s发送一次
				byte[] data = new byte[] { (byte) 4, (byte) 4, (byte) -1 };
				socket.send(new DatagramPacket(data, data.length, remoteAddr));
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 连接已关闭
	 */
	@Override
	protected void dragoniteSocketReadFinish() {
		super.dragoniteSocketReadFinish();

		try {
			dragoniteSocket.closeGracefully();
		} catch (SenderClosedException | InterruptedException | IOException e) {
			// e.printStackTrace();
		}
		dragoniteSocket = null;

		new Thread(() -> {
			reConnect();
		}).start();
	}

	public static void main(String[] args) throws Exception {

		Map<String, String> paramMap = Util.argsToMap(args);

		if (paramMap.get("clientKey") == null) {
			// return;
		}

		String clientKey = Convert.toStr(paramMap.get("clientKey"), "0b42eb00f8a74fe8a39682c71f8e117720181107");
		String remoteHost = Convert.toStr(paramMap.get("remoteHost"), "127.0.0.1");
		int remotePort = Convert.toInt(paramMap.get("remotePort"), 12222);
		int localPort = Convert.toInt(paramMap.get("localPort"), 12221);
		sendSpeed = Convert.toInt(paramMap.get("sendSpeed"), sendSpeed);
		
		new ProxyClient(new InetSocketAddress(remoteHost, remotePort), clientKey, new InetSocketAddress(localPort));
	}
}
