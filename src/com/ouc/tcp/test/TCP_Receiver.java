/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;	//回复的ACK报文段
	int sequence=1;//用于记录当前待接收的包序号，注意包序号不完全是
	// 不是很懂这里的不完全是是什么意思

	// 接收端维护的期望序号
	private volatile int expectAck = 1; // 用来表征期待的ACK

		
	/*构造函数*/
	public TCP_Receiver() {
		super();	//调用超类构造函数
		super.initTCP_Receiver(this);	//初始化TCP接收端
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		//检查校验码，生成ACK
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum() && recvPack.getTcpH().getTh_seq() == expectAck) {
			// 得到期待的包
			expectAck += recvPack.getTcpS().getData().length;
			// 准备交付
			dataQueue.add(recvPack.getTcpS().getData());
			sequence++;
		}else{
			// 即包出错 || 包重复
			System.out.println("This packet is wrong or repeated");
			System.out.println("Recieve : "+CheckSum.computeChkSum(recvPack));
			System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
			System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
		}
		// 生成ACK
		tcpH.setTh_ack(expectAck - recvPack.getTcpS().getData().length); // 代码复用，太优雅了
		// 处理结束，发送返回包
		ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
		tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
		//回复ACK报文段
		reply(ackPack);

		System.out.println();
		//交付数据（每20组数据交付一次）
		if(dataQueue.size() == 20) 
			deliver_data();	
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
//		tcpH.setTh_eflag((byte)0);	//eFlag=0，信道无错误
		tcpH.setTh_eflag((byte)4);
		//发送数据报
		client.send(replyPack);
	}
}
