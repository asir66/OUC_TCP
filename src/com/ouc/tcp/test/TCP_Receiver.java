/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TimerTask;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {
	
//	private TCP_PACKET ackPack;	//回复的ACK报文段
	TCP_PACKET ackPack;
	// 接收端维护的期望序号
	volatile int expectAck = 1; // 用来表征期待的ACK，所以是比收到的大的，返回的是真正收到的
	volatile int contSeq = 1; // 用来表征收到了的连续的最新的包号

	ReceiverSlidingWindow rsWindow = new ReceiverSlidingWindow(this); // 接收窗口
	private UDT_Timer timer = null; // 定时器

	/*构造函数*/
	public TCP_Receiver() {
		super();	//调用超类构造函数
		super.initTCP_Receiver(this);	//初始化TCP接收端
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		contSeq = expectAck - rsWindow.singlePacketSize;
		//检查校验码
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            try {
                contSeq = rsWindow.putPacket(recvPack.clone()) - rsWindow.singlePacketSize;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }else{
			// 即包出错
			System.out.println("This packet is wrong");
			System.out.println("Recieve : "+CheckSum.computeChkSum(recvPack));
			System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
		}

		tcpH.setTh_ack(contSeq);
		ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
		tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));

		if (contSeq == expectAck) { // 遇到期待的序号,延迟
//			expectAck += rsWindow.singlePacketSize;
//			reply(ackPack);
			rsWindow.startTimer(ackPack);
		} else if (contSeq > expectAck) { // 连续滑动
			expectAck = contSeq + rsWindow.singlePacketSize;
			reply(ackPack);
			rsWindow.startTimer(ackPack);
		} else { // 重复/出错/乱序
			reply(ackPack);
			rsWindow.startTimer(ackPack);
		}

		if (dataQueue.size() >= 20){
			deliver_data();
		}
	}

	@Override
	//交付数据（将数据写入文件）；不需要修改
	public void deliver_data() {
		//检查dataQueue，将数据写入文件
		File fw = new File("recvData.txt");
		BufferedWriter writer;
		
		try {
			writer = new BufferedWriter(new FileWriter(fw, true));
			
			//循环检查data队列中是否有新交付数据
			while(!dataQueue.isEmpty()) {
				int[] data = dataQueue.poll();
				
				//将数据写入文件
				for(int i = 0; i < data.length; i++) {
					writer.write(data[i] + "\n");
				}

				writer.flush();		//清空输出缓存
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	//回复ACK报文段
	public void reply(TCP_PACKET replyPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte)1);
		//发送数据报
		client.send(replyPack);
	}

//	void startTimer() {
//		if (timer != null) {
//			timer.cancel();
//		}
//
//		if (expectAck > finalSeq) {
//			return;
//		}
//
//		timer = new UDT_Timer();
//		timer.schedule(new TimerTask() {
//			@Override
//			public void run() {
//				expectAck = contSeq + rsWindow.singlePacketSize;
//				reply(ackPack);
//				startTimer();
//			}
//		}, 500);
//	}

	void setDataQueue(int[] data) {
		dataQueue.add(data);
	}
}
