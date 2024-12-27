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
            tcpReceiver.setDataQueue(dataMap.get(base).getTcpS().getData());
            base += singlePacketSize;
        }
        return base; // 这个是期待的ack
    }


    //////

    void startTimer(TCP_PACKET ackPack) {
        if (timer != null) {
            timer.cancel();
        }

        if (base > finalSeq) {
            return;
        }

        timer = new UDT_Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                tcpReceiver.expectAck = base;
                tcpReceiver.reply(ackPack);
                startTimer(tcpReceiver.ackPack);
            }
        }, 500);
    }





}
