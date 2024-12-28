/***************************2.1: ACK/NACK
**************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.message.*;
import java.util.concurrent.LinkedBlockingQueue;

public class TCP_Sender extends TCP_Sender_ADT {
	
	private TCP_PACKET tcpPack;	//待发送的TCP数据报
	private volatile int flag = 1; // 窗口满标志


	private SenderSlidingWindow ssWindow = new SenderSlidingWindow(this); // 发送窗口

	/*构造函数*/
	public TCP_Sender() {
		super();	//调用超类构造函数
		super.initTCP_Sender(this);		//初始化TCP发送端
	}

	@Override
	//可靠发送（应用层调用）：封装应用层数据，产生TCP数据报；需要修改
	public void rdt_send(int dataIndex, int[] appData) {
		//生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
		tcpH.setTh_seq(dataIndex * appData.length +1);
		tcpS.setData(appData);
		tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
		//更新带有checksum的TCP 报文头
		tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);

		// 判断窗口空与否，填充窗口
		if (ssWindow.isFull()) {
			flag = 0;
			System.out.println("Window is full");
		}

		while (flag == 0);

		// 将数据包放入窗口
		try {
			ssWindow.putPacket(tcpPack.clone());
		} catch (Exception e) {
			e.printStackTrace();
		}
		//发送TCP数据报
        try {
            udt_send(tcpPack.clone());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        // 发送包
//		System.out.println("**********************\n发送包:" + tcpPack.getTcpH().getTh_seq());
	}

	@Override
	//不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送；仅需修改错误标志
	// 1. 出错 2. 丢包 3. 延迟 4. 出错丢包 5. 出错延迟 6. 丢包延迟 7. 出错丢包延迟
	public void udt_send(TCP_PACKET stcpPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte)7);
		//System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());				
		//发送数据报
		client.send(stcpPack);
	}

	// 收到的包
	private LinkedBlockingQueue<Integer> recvAckQueue = new LinkedBlockingQueue<>();
	@Override
	//需要修改
	public void waitACK() {
		//循环检查ackQueue
		while (!recvAckQueue.isEmpty()) {
			int ack = recvAckQueue.poll();
			ssWindow.recvAck(ack);
			if (!ssWindow.isFull()){
				flag = 1;
			} else {
				flag = 0;
			}
		}
	}

	@Override
	//接收到ACK报文：检查校验和，将确认号插入ack队列;NACK的确认号为－1；不需要修改
	public void recv(TCP_PACKET recvPack) {
		// 检查校验和
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()){
			try {
				recvAckQueue.put(recvPack.getTcpH().getTh_ack());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			waitACK();
		}else {
			System.out.println("This packet:" + recvPack.getTcpH().getTh_seq() + "is corrupted");
		}
	}
}