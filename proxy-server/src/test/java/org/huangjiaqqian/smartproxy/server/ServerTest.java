package org.huangjiaqqian.smartproxy.server;

import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.vecsight.dragonite.mux.conn.MultiplexedConnection;
import com.vecsight.dragonite.mux.conn.Multiplexer;
import com.vecsight.dragonite.sdk.config.DragoniteSocketParameters;
import com.vecsight.dragonite.sdk.exception.ConnectionNotAliveException;
import com.vecsight.dragonite.sdk.exception.IncorrectSizeException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteServer;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class ServerTest {
	public static void main(String[] args) throws Exception {
		DragoniteServer dragoniteServer = new DragoniteServer(7777, 1024, new DragoniteSocketParameters());
		DragoniteSocket dragoniteSocket = dragoniteServer.accept();
		
		Multiplexer multiplexer = new Multiplexer(bytes -> {
            try {
                dragoniteSocket.send(bytes);
            } catch (InterruptedException | IncorrectSizeException | IOException | SenderClosedException e) {
                Logger.error(e, "Multiplexer is unable to send data");
            }
        }, 100);
		
		new Thread(() -> {
			byte[] buf;
			
				try {
					while ((buf = dragoniteSocket.read()) != null) {
						multiplexer.onReceiveBytes(buf);
					}
				} catch (ConnectionNotAliveException | InterruptedException e) {
					//e.printStackTrace();
				} finally {
					try {
						multiplexer.close();
						dragoniteSocket.closeGracefully();
					} catch (SenderClosedException | InterruptedException | IOException e) {
						//e.printStackTrace();
					}
				}
			
		}, "FC-MuxReceive").start();;
		
		System.out.println("开始接收请求...");
		MultiplexedConnection connection = multiplexer.acceptConnection();
		System.out.println("已创建请求...");
		byte[] buf = connection.read();
		System.out.println(new String(buf));
		
		connection.close();
		
		dragoniteServer.destroy();
		
	}
}
