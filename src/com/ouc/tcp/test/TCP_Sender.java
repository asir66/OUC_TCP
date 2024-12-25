/***************************2.1: ACK/NACK
**************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer; // 用于实现定时器，伏笔
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

import java.util.TimerTask;

public class TCP_Sender extends TCP_Sender_ADT {
	
	private TCP_PACKET tcpPack;	//待发送的TCP数据报
	private volatile int flag = 0;


	/*构造函数*/
	public TCP_Sender() {
		super();	//调用超类构造函数
		super.initTCP_Sender(this);		//初始化TCP发送端
	}

	UDT_Timer timer = new UDT_Timer(); // 初始化实例

	@Override
	//可靠发送（应用层调用）：封装应用层数据，产生TCP数据报；需要修改
	public void rdt_send(int dataIndex, int[] appData) {
		
		//生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
		// 因为TCP是按字节流编号，数据会事先计算MTU为多少，这里的appData就变成了规律的数，直至最后一个包
		// 数据由于mtu的原因，appData是一致的,这里表征这个数据包中首个字节的编号
		tcpH.setTh_seq(dataIndex * appData.length +1);
		tcpS.setData(appData);
		// destinAdd是目的地址
		tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);		
		//更新带有checksum的TCP 报文头		
		tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);
		
		//发送TCP数据报
		flag = 0;
		udt_send(tcpPack);
		startTimer();

		//等待ACK报文
		//waitACK();
		while (flag==0);
	}

	private void startTimer() {
		// 如果定时器存在要取消先前的计时器
		if (timer != null) {
			timer.cancel();
		}
		// 创建新的
		timer = new UDT_Timer();
		// 设置调度任务
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (flag == 0) {
					// 超时
					System.out.println("Time out, Retransmit: "+tcpPack.getTcpH().getTh_seq());
					// 重传
					udt_send(tcpPack);
					startTimer();
				}
			}
		}, 1000);
	}
	
	@Override
	//不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送；仅需修改错误标志
	// 1. 出错 2. 丢包 3. 延迟 4. 出错丢包 5. 出错延迟 6. 丢包延迟 7. 出错丢包延迟
	public void udt_send(TCP_PACKET stcpPack) {
		//设置错误控制标志
//		tcpH.setTh_eflag((byte)0);
		tcpH.setTh_eflag((byte)4);
		//System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());				
		//发送数据报
		client.send(stcpPack);
	}
	
	@Override
	//需要修改
	public void waitACK() {
		//循环检查ackQueue
		//循环检查确认号对列中是否有新收到的ACK		
		if(!ackQueue.isEmpty()){
			// 取出当前的ACK确认号
			int currentAck=ackQueue.poll();
			// System.out.println("CurrentAck: "+currentAck);
			// 如果当前的包确认号是对的话代表这个包收到了
			// 如果当前包确认号正确，那么就可以发送下一个包
			if (currentAck == tcpPack.getTcpH().getTh_seq()){
				System.out.println("Clear: "+tcpPack.getTcpH().getTh_seq());
				flag = 1;
				//break;
			}else{
				System.out.println("Retransmit: "+tcpPack.getTcpH().getTh_seq());
				udt_send(tcpPack);
				startTimer();
				flag = 0;
			}
		}
	}

	@Override
	//接收到ACK报文：检查校验和，将确认号插入ack队列;NACK的确认号为－1；不需要修改
	public void recv(TCP_PACKET recvPack) {
		// 检查校验和
		if(CheckSum.computeChkSum(recvPack) != recvPack.getTcpH().getTh_sum()){
			udt_send(tcpPack);
			startTimer();
			flag = 0;
		}else {
			System.out.println("Receive ACK Number： "+ recvPack.getTcpH().getTh_ack());
			ackQueue.add(recvPack.getTcpH().getTh_ack());
	    	System.out.println();
			if (timer != null) {
				timer.cancel(); // 取消定时器
			}
	   
	    	//处理ACK报文
	    	waitACK();
		}
	}
}