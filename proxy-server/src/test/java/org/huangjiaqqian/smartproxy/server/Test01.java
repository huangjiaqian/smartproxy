package org.huangjiaqqian.smartproxy.server;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class Test01 {

	@Test
	public void test() {
		BigDecimal bigDecimal = new BigDecimal(Double.MAX_VALUE);
		System.out.println(bigDecimal.toString());

		bigDecimal = new BigDecimal(Float.MAX_VALUE);
		System.out.println(bigDecimal.toString());

		bigDecimal = new BigDecimal(Short.MAX_VALUE);
		System.out.println(bigDecimal.toString());

		bigDecimal = new BigDecimal(Double.MIN_VALUE);
		System.out.println(bigDecimal.toString());
	}

	public static int id = 100001;
	public static int bufferSize = 2048;

	public static void main(String[] args) throws IOException, InterruptedException {
		ServerSocketChannel acceptorSvr = ServerSocketChannel.open();
		acceptorSvr.socket().bind(new InetSocketAddress(9999));

		acceptorSvr.configureBlocking(false); // 非阻塞

		Selector selector = Selector.open();

		// ServerSocketChannel注册到Reactor线程的多路复用器Selector上，监听ACCEPT事件
		acceptorSvr.register(selector, SelectionKey.OP_ACCEPT).attach(id++);

		final List<SocketChannel> list = new ArrayList<SocketChannel>();
		new Thread(() -> {
			while (true) {
				for (SocketChannel socketChannel : list) {

					ByteBuffer sendBuf = ByteBuffer.allocate(bufferSize);
					String sendText = "thank！\n";
					sendBuf.put(sendText.getBytes());
					sendBuf.flip(); // 写完数据后调用此方法
					try {
						socketChannel.write(sendBuf);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						socketChannel.close();
						return;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}).start();

		while (true) {
			selector.select();
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> it = selectedKeys.iterator();
			while (it.hasNext()) {
				SelectionKey selectionKey = (SelectionKey) it.next();
				// 判断是哪个事件
				if (selectionKey.isAcceptable()) {// 客户请求连接
					System.out.println(selectionKey.attachment() + " - 接受请求事件");
					// 获取通道 接受连接,
					// 设置非阻塞模式（必须），同时需要注册 读写数据的事件，这样有消息触发时才能捕获
					ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
					SocketChannel socketChannel = serverSocketChannel.accept();
					socketChannel.configureBlocking(false).register(selector, SelectionKey.OP_READ).attach(id++);
					/*
					 * ByteBuffer sendBuf = ByteBuffer.allocate(bufferSize);
					 * String sendText = "thank！\n";
					 * sendBuf.put(sendText.getBytes()); sendBuf.flip(); //
					 * 写完数据后调用此方法 socketChannel.write(sendBuf);
					 */
					list.add(socketChannel);

					System.out.println(selectionKey.attachment() + " - 已连接");
				} else if (selectionKey.isValid() && selectionKey.isReadable()) {// 读数据
					System.out.println(selectionKey.attachment() + " - 读数据事件");
					SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
					ByteBuffer receiveBuf = ByteBuffer.allocate(bufferSize);
					if(clientChannel.read(receiveBuf) == -1) {
						list.remove(clientChannel);
						clientChannel.close();
					} else {
						System.out.println(selectionKey.attachment() + " - 读取数据：" + getString(receiveBuf));						
					}
					/*
					 * ByteBuffer sendBuf = ByteBuffer.allocate(bufferSize);
					 * String sendText = "hello\n";
					 * sendBuf.put(sendText.getBytes()); sendBuf.flip(); //
					 * 写完数据后调用此方法 clientChannel.write(sendBuf);
					 */
				} else if (selectionKey.isValid() && selectionKey.isWritable()) {// 写数据
					System.out.println(selectionKey.attachment() + " - 写数据事件");
					SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
					ByteBuffer sendBuf = ByteBuffer.allocate(bufferSize);
					String sendText = "hello\n";
					sendBuf.put(sendText.getBytes());
					sendBuf.flip(); // 写完数据后调用此方法
					clientChannel.write(sendBuf);

				} else if (selectionKey.isConnectable()) {
					System.out.println(selectionKey.attachment() + " - 连接事件");
				} else if (!selectionKey.isValid() ) {
					System.out.println("连接关闭");
				}
				// 必须removed 否则会继续存在，下一次循环还会进来,
				// 注意removed 的位置，针对一个.next() remove一次
				it.remove();
			}

		}

	}

	/**
	 * ByteBuffer 转换 String
	 * 
	 * @param buffer
	 * @return
	 */
	public static String getString(ByteBuffer buffer) {
		String string = "";
		try {
			for (int i = 0; i < buffer.position(); i++) {
				string += (char) buffer.get(i);
			}
			return string;
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}
	}

}
