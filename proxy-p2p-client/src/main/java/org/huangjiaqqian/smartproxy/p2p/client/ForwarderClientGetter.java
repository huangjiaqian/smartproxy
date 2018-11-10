package org.huangjiaqqian.smartproxy.p2p.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.huangjiaqqian.smartproxy.common.BinaryReader;
import org.huangjiaqqian.smartproxy.common.BinaryWriter;
import org.huangjiaqqian.smartproxy.common.Util;
import org.nnat.dragonite.forwarder.config.ForwarderClientConfig;
import org.nnat.dragonite.forwarder.network.client.ForwarderClient;

import com.vecsight.dragonite.forwarder.exception.IncorrectHeaderException;
import com.vecsight.dragonite.forwarder.exception.ServerRejectedException;
import com.vecsight.dragonite.sdk.exception.DragoniteException;

public class ForwarderClientGetter {

	private ForwarderClientConfig config;

	private DatagramSocket socket;

	private InetSocketAddress remoteSocketAddress;

	public ForwarderClientGetter(ForwarderClientConfig config,  String clientKey) throws InterruptedException, SocketException {
		this.config = config;
		this.socket = config.getDatagramSocket();

		if (this.socket == null) {
			this.socket = new DatagramSocket();
		}

		Object connectLock = new Object();

		Thread readThread = new Thread(() -> {
			DatagramPacket packet = null;
			byte[] buf = null;
			while (true) {
				try {
					buf = new byte[2048];
					packet = new DatagramPacket(buf, 2048);
					socket.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				BinaryReader reader = new BinaryReader(packet.getData());
				byte flag = reader.getSignedByte();

				if ((byte) 12 == flag) {
					// 成功
					byte[] addrBytes = reader.getBytesGroupWithByteLength();
					remoteSocketAddress = Util.genAddr(addrBytes);
					try {
						synchronized (connectLock) {
							connectLock.notifyAll();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					return;
				} else if ((byte) 11 == flag) {
					// 失败
					try {
						synchronized (connectLock) {
							connectLock.notifyAll();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					return;
				}

			}
		});
		readThread.start();

		Thread.sleep(100);

		byte[] clientKeyBytes = clientKey.getBytes();
		BinaryWriter writer = new BinaryWriter(4 + clientKeyBytes.length);
		writer.putBytes(new byte[] { (byte) 4, (byte) 4, (byte) 1 });
		writer.putBytesGroupWithByteLength(clientKeyBytes);
		byte[] sendData = writer.toBytes();

		for (int i = 0; i < 3; i++) {
			try {
				socket.send(new DatagramPacket(sendData, sendData.length, config.getRemoteAddress()));
			} catch (IOException e) {
				e.printStackTrace();
			}

			synchronized (connectLock) {
				connectLock.wait(3000); // 等待3s
			}

			if (remoteSocketAddress != null) {
				break;
			}
		}

		if (remoteSocketAddress == null) {
			readThread.interrupt();
			throw new SocketException("can not connect... ");
		}

		config.setDatagramSocket(socket);
		config.setRemoteAddress(remoteSocketAddress);
	}

	public ForwarderClient getForwarderClient() throws IOException, InterruptedException, DragoniteException,
			IncorrectHeaderException, ServerRejectedException {
		ForwarderClient client = new ForwarderClient(config);
		
		return client;
	}

}
