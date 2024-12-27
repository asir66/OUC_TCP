package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

public class ReceiverSlidingWindow extends SlidingWindow {

    private volatile int base = 1; // 期待的ACK

    // 构造函数
    public ReceiverSlidingWindow(TCP_Receiver tcpReceiver) {
        super();
        base = tcpReceiver.expectAck;
    }

    int putPacket(TCP_PACKET packet) {
        if (dataMap.containsKey(packet.getTcpH().getTh_seq())) { // 发送存在过的包
            return base;
        }
        try {
            dataMap.put(packet.getTcpH().getTh_seq(), packet.clone());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        return slide();
    }

    int slide() {
        while(dataMap.containsKey(base)) { // 存在这个包
            base += singlePacketSize;
        }
        return base; // 这个是期待的ack
    }
}
