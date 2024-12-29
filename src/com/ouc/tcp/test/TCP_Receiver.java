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
	// 所以ackPack复用的
	TCP_PACKET ackPack;
	// 接收端维护的期望序号
	volatile int expectAck = 1; // 用来表征期待的ACK，所以是比收到的大的，返回的是真正收到的
	volatile int base = 1; // 窗口的后沿 || ==1 同步

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
//		System.out.println("**************************\n接收包: " + recvPack.getTcpH().getTh_seq());
		//检查校验码
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			// 填包,一定要用clone
            try {
                expectAck = rsWindow.putPacket(recvPack.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }else{
			// 即包出错
			System.out.println("This packet is wrong");
			System.out.println("Recieve : "+CheckSum.computeChkSum(recvPack));
			System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
		}

		// 造回复包
		tcpH.setTh_ack(expectAck - rsWindow.singlePacketSize);
		ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
		tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));

		// 三种情况
		// 1. 是期待的序号
		// 2. 是连续滑动的序号
		// 3. 重复/出错/乱序

		if (recvPack.getTcpH().getTh_seq() == base){ // 期待的包
			System.out.println("拿到窗口后沿的包，等待500ms");
			startTimer();
		} else if (expectAck > base){ // 滑动
			if (timer != null) {
				timer.cancel();
			}
			rsWindow.slide();
			reply(ackPack);
		} else { // if (recvPack.getTcpH().getTh_seq() > base - rsWindow.singlePacketSize * rsWindow.windowSize){ // 收到后面的包或者重复包或者错误的包
			reply(ackPack);
		}

		if (dataQueue.size() >= 20 || expectAck >= rsWindow.finalSeq){
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
		tcpH.setTh_eflag((byte)7);
		//发送数据报
		client.send(replyPack);
	}

	void startTimer() {
		if (timer != null) {
			timer.cancel();
		}

		timer = new UDT_Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				rsWindow.slide();
				reply(ackPack);
				if (ackPack.getTcpH().getTh_ack() >= rsWindow.finalSeq){
					timer.cancel();
				}
			}
		}, 500);
	}
	void setDataQueue(int[] data) {
		dataQueue.add(data);
	}
}
