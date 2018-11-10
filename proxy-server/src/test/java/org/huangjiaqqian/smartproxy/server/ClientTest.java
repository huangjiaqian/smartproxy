package org.huangjiaqqian.smartproxy.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.pmw.tinylog.Logger;

import com.vecsight.dragonite.mux.conn.MultiplexedConnection;
import com.vecsight.dragonite.mux.conn.Multiplexer;
import com.vecsight.dragonite.sdk.config.DragoniteSocketParameters;
import com.vecsight.dragonite.sdk.exception.IncorrectSizeException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteClientSocket;

public class ClientTest {
		
	public static void main(String[] args) throws Exception {
		DragoniteClientSocket socket = new DragoniteClientSocket(new InetSocketAddress("127.0.0.1", 7777), 1024, new DragoniteSocketParameters());
		
		Multiplexer multiplexer = new Multiplexer(bytes -> {
			try {
				socket.send(bytes);
			} catch (InterruptedException | IncorrectSizeException | IOException | SenderClosedException e) {
				Logger.error(e, "Multiplexer is unable to send data");
			}
		}, 100);
		System.out.println("开始创建请求...");
		MultiplexedConnection connection = multiplexer.createConnection((short)1);
		
		
		System.out.println("已创建请求...");
		connection.send("你好".getBytes());
		
		multiplexer.close();
		
	}
}
