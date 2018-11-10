package org.huangjiaqqian.smartproxy.common;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vecsight.dragonite.forwarder.network.server.ForwarderServer;
import com.vecsight.dragonite.sdk.exception.IncorrectSizeException;
import com.vecsight.dragonite.sdk.exception.SenderClosedException;
import com.vecsight.dragonite.sdk.socket.DragoniteServer;
import com.vecsight.dragonite.sdk.socket.DragoniteSocket;

public class Util {

	public static final String packet2Key(DatagramPacket packet) {
		return packet.getAddress().getHostAddress() + ":" + packet.getPort();
	}

	public static final int towByte2IntVal(byte[] b) {
		return (b[0] - Byte.MIN_VALUE) * (Byte.MAX_VALUE - Byte.MIN_VALUE) + (b[1] - Byte.MIN_VALUE);
	}

	public static byte[] port2TowByte(int intVal) {
		return new byte[] { (byte) (intVal / (Byte.MAX_VALUE - Byte.MIN_VALUE) - Byte.MIN_VALUE),
				(byte) (intVal % (Byte.MAX_VALUE - Byte.MIN_VALUE) - Byte.MIN_VALUE) };
	}

	public static final int Byte2Int(byte[] bytes) {
		return (bytes[0] & 0xff) << 24 | (bytes[1] & 0xff) << 16 | (bytes[2] & 0xff) << 8 | (bytes[3] & 0xff);
	}

	/** * int转byte数组 * @param bytes * @return */
	public static final byte[] IntToByte(int num) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) ((num >> 24) & 0xff);
		bytes[1] = (byte) ((num >> 16) & 0xff);
		bytes[2] = (byte) ((num >> 8) & 0xff);
		bytes[3] = (byte) (num & 0xff);
		return bytes;
	}

	public static final InetSocketAddress key2Addr(String key) {
		if (key == null || key.indexOf(":") == -1) {
			return null;
		}
		String str[] = key.split(":");
		return new InetSocketAddress(str[0], Integer.parseInt(str[1]));
	}

	public static final String addr2Key(InetSocketAddress addr) {
		return addr.getHostString() + ":" + addr.getPort();
	}

	public static final InetSocketAddress getSocketAddr(String ip, int port) {
		return new InetSocketAddress(ip, port);
	}

	public static final byte[] ip2Byte(String ip) {
		String[] str = ip.split("\\.");
		byte[] b = new byte[str.length];
		for (int i = 0; i < str.length; i++) {
			b[i] = (byte) (Integer.parseInt(str[i]) - 128);
		}
		return b;
	}

	public static final byte[] genAddrBytes(String hostname, int port) {
		String key = hostname + "|" + port;
		return key.getBytes();
	}
	
	public static final InetSocketAddress genAddr(byte[] addrBytes) {
		InetSocketAddress address = null;
		try {
			String key = new String(addrBytes, "UTF-8");
			int splitIndex = key.lastIndexOf("|");
			String hostname = key.substring(0, splitIndex);
			String port = key.substring(splitIndex + 1, key.length());
			address = new InetSocketAddress(hostname, Integer.parseInt(port));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return address;
	}
	
	/*
	public static final byte[] genAddrByte(String ip, int port) {
		byte[] ipByte = Util.ip2Byte(ip);
		byte[] portByte = Util.port2TowByte(port);
		byte[] buf = new byte[ipByte.length + portByte.length];

		System.arraycopy(ipByte, 0, buf, 0, ipByte.length);
		System.arraycopy(portByte, 0, buf, 4, portByte.length);

		return buf;
	}
	 */

	/*
	public static final InetSocketAddress addrByte2Addr(byte[] b) {
		byte[] ipByte = new byte[4];
		byte[] portByte = new byte[2];

		System.arraycopy(b, 0, ipByte, 0, ipByte.length);
		System.arraycopy(b, ipByte.length, portByte, 0, portByte.length);

		return new InetSocketAddress(byte2IP(ipByte), towByte2IntVal(portByte));
	}

	public static final String addrByte2Key(byte[] b) {
		return addr2Key(addrByte2Addr(b));
	}
	 */

	public static final String byte2IP(byte[] b) {
		StringBuffer sb = new StringBuffer();
		for (byte c : b) {
			sb.append(sb.length() > 0 ? "." : "").append(((int) c) + 128);
		}
		return sb.toString();
	}

	public static final List<byte[]> splitList(byte[] data, int maxLen) {
		if (data == null || data.length < 1) {
			return null;
		}
		List<byte[]> list = new ArrayList<>();
		int dataLen = data.length;
		if (dataLen < maxLen) {
			list.add(data);
			return list; // 无需切割
		}

		byte[] temp = null;
		for (int i = 0; i < dataLen; i += maxLen) {
			int len = maxLen;
			if (i + maxLen > dataLen) {
				len = dataLen - i;
			}
			temp = new byte[len];
			System.arraycopy(data, i, temp, 0, len);
			list.add(temp);
		}

		return list;
	}
	
	public static final void splitBytes(byte[] buf, byte[]...b) {
		int currentLen = 0;
		for (byte[] bs : b) {
			if(bs == null || bs.length == 0) {
				continue;
			}
			System.arraycopy(buf, currentLen, bs, 0, bs.length);
			currentLen += bs.length;
		}
	}
	
	public static final byte[] appendBytes(Integer lastLen, byte[]... b) {
		int totalLen = 0;
		int i = 0;
		for (byte[] bs : b) {
			if(lastLen != null && lastLen != 0 && i == b.length - 1) {
				totalLen += lastLen;
			} else {
				totalLen += bs.length;				
			}
			i++;
		}
		byte[] buf = new byte[totalLen];
		int currentLen = 0;
		i = 0;
		for (byte[] bs : b) {
			int bsLen = bs.length;
			if(bs == null || bsLen == 0) {
				continue;
			}
			
			if(lastLen != null && lastLen != 0 && i == b.length - 1) {
				bsLen = lastLen;
			}
			System.arraycopy(bs, 0, buf, currentLen, bsLen);
			currentLen += bs.length;
			
			i++;
		}
		return buf;
	}
	
	public static final byte[] appendBytes(byte[]... b) {
		
		return appendBytes(null, b);
	}
	
	public static final void writeAndFlush(DragoniteSocket dragoniteSocket, byte[] data) throws IncorrectSizeException, SenderClosedException, InterruptedException, IOException {
		if(dragoniteSocket == null || !dragoniteSocket.isAlive()) {
			return;
		}
		byte[] buf = new byte[data.length + 4];
		
		byte[] lenByte = Util.IntToByte(data.length);
		System.arraycopy(lenByte, 0, buf, 0, lenByte.length);
		
		System.arraycopy(data, 0, buf, lenByte.length, data.length);
		
		try {
			
			dragoniteSocket.send(buf);			
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

	}
	
	public static final void writeAndFlush(Socket socket, byte[] data) throws IOException {
		socket.getOutputStream().write(data);
		socket.getOutputStream().flush();
	}
	
	public static final Object getFieldObj(Object src, String fieldName) {
		Object fieldObj = null;
		try {
			Field field = src.getClass().getDeclaredField(fieldName);
			if(field != null) {
				field.setAccessible(true);
				fieldObj = field.get(src);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return fieldObj;
	}
	
	public static final DatagramSocket getDatagramSocket(DragoniteServer server) {
		return (DatagramSocket) getFieldObj(server, "datagramSocket");
	}
	
	public static final DatagramSocket getDatagramSocket(org.nnat.dragonite.forwarder.network.server.ForwarderServer server) {
		DatagramSocket datagramSocket = null;
		DragoniteServer dragoniteServer = (DragoniteServer) getFieldObj(server, "dragoniteServer");
		if(dragoniteServer != null) {
			datagramSocket = getDatagramSocket(dragoniteServer);
		}
		return datagramSocket;
	}
	
	public static final DatagramSocket getDatagramSocket(ForwarderServer server) {
		DatagramSocket datagramSocket = null;
		DragoniteServer dragoniteServer = (DragoniteServer) getFieldObj(server, "dragoniteServer");
		if(dragoniteServer != null) {
			datagramSocket = getDatagramSocket(dragoniteServer);
		}
		return datagramSocket;
	}
	
	public static final Class<?>[] list2ArrCls(List<Class<?>> list) {
		if(list == null) {
			return null;
		}
		Class<?>[] ts = new Class<?>[list.size()];
		list.toArray(ts);
		return ts;
	}
	
	public static final Object[] list2ArrObj(List<Object> list) {
		if(list == null) {
			return null;
		}
		Object[] ts = new Object[list.size()];
		list.toArray(ts);
		return ts;
	}
	
	public static final Method[] getMethodByName(Class<?> cls, String methodName) {
		Method[] methods = cls.getMethods();
		if(methods == null || methodName == null || "".equals(methodName)) {
			return null;
		}
		
		List<Method> methodList = new ArrayList<>();
		for (Method method : methods) {
			if(methodName.equals(method.getName())) {
				methodList.add(method);
			}
		}
		if(methodList.isEmpty()) {
			return null;
		}
		methods = new Method[methodList.size()];
		methodList.toArray(methods);
		return methods;
	}
	
	public static final Map<String, String> argsToMap(String[] args) {
		Map<String, String> paramMap = new HashMap<>();

		for (String s : args) {
			String key = null;
			String value = null;
			String[] ss = s.split("=");

			if (ss.length < 2) {
				continue;
			}
			key = ss[0];
			value = ss[1];

			paramMap.put(key, value);
		}
		return paramMap;
	}
	
}
