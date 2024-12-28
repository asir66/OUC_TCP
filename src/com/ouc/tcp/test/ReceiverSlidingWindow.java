package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;

public class ReceiverSlidingWindow extends SlidingWindow {

    private TCP_Receiver tcpReceiver = null; // 接收端
    private volatile int base = 1; // 期待的ACK
    volatile UDT_Timer timer = null; // 定时器

    // 构造函数
    public ReceiverSlidingWindow(TCP_Receiver tcpReceiver) {
        super();
        base = tcpReceiver.expectAck;
        this.tcpReceiver = tcpReceiver;
    }

    int putPacket(TCP_PACKET packet) {
        int seq = packet.getTcpH().getTh_seq();
        if (seq > base + windowSize * singlePacketSize || seq < base) {
            return base;
        }

        if (dataMap.containsKey(seq)) { // 发送存在过的包
            return base;
        }
        dataMap.put(seq, packet);

        int tmp = base;
        while(dataMap.containsKey(tmp)) {
            tmp += singlePacketSize;
        }

        return tmp;
    }


    int slide() {
        while(dataMap.containsKey(base)) { // 存在这个包
            // 调用接口实现滑动时顺序上交包
            System.out.println("窗口开始滑动：传输的分组是" + base);
            tcpReceiver.setDataQueue(dataMap.get(base).getTcpS().getData());
            base += singlePacketSize;
        }
        tcpReceiver.base = base;
        return base; // 这个是期待的ack
    }

}
