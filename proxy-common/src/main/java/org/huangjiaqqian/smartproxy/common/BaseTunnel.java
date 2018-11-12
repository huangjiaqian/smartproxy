package org.huangjiaqqian.smartproxy.common;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vecsight.dragonite.sdk.exception.ConnectionNotAliveException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class BaseTunnel {
	
	protected DragoniteSocket dragoniteSocket;
	
	protected ExecutorService es = Executors.newCachedThreadPool();
	
	protected void doExecuteWithConn(byte[] buf, BinaryReader reader) {
	}
	
	/**
	 * 执行除连接的其他操作
	 * 
	 * @param buf
	 * @param reader
	 */
	protected void doExecuteOther(byte[] buf, BinaryReader reader) {
	}
	
	protected void executeRead(byte[] buf) {
		BinaryReader reader = new BinaryReader(buf);
		byte flag = reader.getSignedByte();

		if ((byte) 1 == flag) {
			doExecuteWithConn(buf, reader);
		} else {
			doExecuteOther(buf, reader);
		}

	}
	
	protected void dragoniteSocketReadFinish() {
	}
	
	protected void startDragoniteSocketRead() {

		es.execute(() -> {

			byte[] dataBuffer = null; // 数据缓存
			int dataLen = 0; // 数据长度
			int currentLen = 0; // 当前长度

			while (true) {
				try {
					byte[] buf = dragoniteSocket.read();

					int lenByteLen = 0;
					if (dataBuffer == null) {
						lenByteLen = 4;

						byte[] lenByte = new byte[lenByteLen];
						System.arraycopy(buf, 0, lenByte, 0, lenByteLen);

						dataLen = Util.Byte2Int(lenByte);
						dataBuffer = new byte[dataLen]; // 大数据块
						currentLen = 0;

					}

					int currentDataLen = buf.length - lenByteLen;
					System.arraycopy(buf, lenByteLen, dataBuffer, currentLen, currentDataLen);
					currentLen += currentDataLen;

					if (new Integer(currentLen).equals(new Integer(dataLen))) {

						/////
						executeRead(dataBuffer);
						/////

						dataBuffer = null;
					}
				} catch (InterruptedException | ConnectionNotAliveException e) {
					if (dragoniteSocket != null) {
						try {
							dragoniteSocket.closeGracefully();
						} catch (SenderClosedException | InterruptedException | IOException e1) {
							e1.printStackTrace();
						}
					}
					break;
				}
			}
			dragoniteSocketReadFinish();
		});
	}
}
