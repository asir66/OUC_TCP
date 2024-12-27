package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;
import java.util.TimerTask;

public class SenderSlidingWindow extends SlidingWindow {
    private volatile UDT_Timer timer = null; // 定时器
    private TCP_Sender tcpSender = null; // 发送端
    private volatile int repAck = 0; // 重复确认次数


    // 构造函数
    public SenderSlidingWindow(TCP_Sender tcpSender) {
        super();
        this.tcpSender = tcpSender;
    }

    // 启动计时器
    void startTimer() {
        if (timer != null) {
            try {
                timer.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        timer = new UDT_Timer();
        timer.schedule( new TimerTask() {
            @Override
            public void run() {
                // 超时重传
                tcpSender.udt_send(dataMap.get(base));
                startTimer();
            }
        }, 3000);
    }

    public boolean putPacket(TCP_PACKET packet){
        if (packet.getTcpH().getTh_seq() > base + windowSize * singlePacketSize) {
            return false;
        }
        dataMap.put(packet.getTcpH().getTh_seq(), packet);
        return true;
    }

    public boolean recvAck(int ack){
        if (ack >= base+windowSize*singlePacketSize) {
            return false;
        }

        if (base > finalSeq) {
            return false;
        }

        if (ack == base - singlePacketSize) { // 快速重传
            repAck++;
            if (repAck == 3) {
//                System.out.println("base:"+base);
//                System.out.println("packet:"+dataMap.get(base).getTcpH().getTh_seq());
                repAck = 0;
                tcpSender.udt_send(dataMap.get(base)); // 这里出现了错误，没有发base
                startTimer();
            }
        } else {
            slide(ack);
        }
        return true;
    }

    void slide(int ack){
        for (; base <= ack; base += singlePacketSize) {
            dataMap.remove(base);
        }
    }

}
