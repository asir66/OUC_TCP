package com.ouc.tcp.test;

import java.util.zip.CRC32;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.message.TCP_SEGMENT;

public class CheckSum {
	
	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		int checkSum = 0;
		// 这里好像用不到sum

		TCP_HEADER tcp_header = tcpPack.getTcpH();
		TCP_SEGMENT tcp_segment = tcpPack.getTcpS();

		int seq = tcp_header.getTh_seq();
		int ack = tcp_header.getTh_ack();
		int[] data = tcp_segment.getData();

		int tmp_seq = dealFunc(seq);
		int tmp_ack = dealFunc(ack);
		checkSum += tmp_seq;
		checkSum = checkSum & 0xffff;
		checkSum += tmp_ack;
		checkSum = checkSum & 0xffff;

		for (int i = 0; i < data.length; i++) {
			checkSum += dealFunc(data[i]);
			checkSum = checkSum & 0xffff;
		}
		return (short) checkSum;
	}

	public static int dealFunc(int tmp){
		// 这里用无符号右移动
		int highSum = (tmp >>> 16) & 0xFFFF;
		int lowSum = tmp & 0xFFFF;
		return 0x0000ffff & (highSum + lowSum);
	}
}
