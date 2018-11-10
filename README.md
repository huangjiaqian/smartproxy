# smartproxy
smartproxy是基于java实现的将局域网电脑的端口映射到公网。或不走公网流量使用直连客户端方式映射局域网电脑端口到另一个局域网电脑上。支持任何tcp上层协议（访问ssh、远程桌面、网站等）。

### 说明
 - 本项目引用到了Dragonite的项目文件,进行在服务器和客户端之间建立连接。Dragonite 是一个基于 UDP 的可靠传输协议，能针对高丢包与不稳定的网络极大提高传输速度。详情： https://github.com/dragonite-network/dragonite-java
 - 本项目的web端设置页面使用了lanproxy的页面文件，详情：https://github.com/ffay/lanproxy
### 使用

#### 获取发布包

-	拉取源码，发布包放在runtime-file目录下
- smartproxy-server.zip 需放到公网环境下运行
- smartproxy-client.jar 在局域网内运行
- smartproxy-p2p-client.zip 是之连局域网客户端，可在需要直接穿透局域网的时候使用

#### 配置和使用

##### 公网服务器配置
 - 解压 smartproxy-server.zip 包，运行里面的proxy-server.jar文件即可
 - nohup java -jar proxy-server.jar &
   
 - 运行后可用浏览器打开http://ip:8088 ，在里面进行配置，添加客户端
   
 - 默认隧道端口号为12222，http服务端口为8088，可用如下参数运行，修改默认值(如分别改为12221和8080)
   nohup java -jar proxy-server.jar tunnelPort=12221 httpPort=8080 &
  
##### 局域网客户端配置
 - 运行smartproxy-client.jar文件：
 - nohup java -jar smartproxy-client.jar remoteHost=公网ip clientKey=0b42eb00f8a74fe8a39682c71f8e117720181107 &
 - 参数信息如下：
 - clientKey: 公网服务器端配置的秘钥
 - remoteHost: 公网IP或域名
 - remotePort: 公网隧道端口号（默认为12222）
 - localPort: p2p直连时使用到的端口（默认为12221）
 - sendSpeed: 发送速度，根据客户端到服务器的网络关键配置（默认为1M/s: 1024 * 1024）
  
##### 使用
 - 局域网连接成功后，浏览器打开http://公网ip:8088 进行端口配置
 
##### 直连客户端使用
 - 解压smartproxy-p2p-client.zip
 - nohup java -jar proxy-p2p-client.jar 1995 &
 - 浏览器打开 http://localhost:1995，进行连接配置。
 - 配置完成后即可不经公网转发流量直接访问另一个局域网的客户端
  
  


 
