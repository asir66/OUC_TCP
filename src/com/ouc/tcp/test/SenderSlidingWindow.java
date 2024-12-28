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

        if (base > finalSeq) { // 由于这里是递归的，所以要加上结束
            return;
        }
        timer = new UDT_Timer();
        timer.schedule( new TimerTask() {
            @Override
            public void run() {
                // 超时重传
                System.out.println("超时重传，开始慢开始");
                tcpSender.udt_send(dataMap.get(base));
                ssthresh = cwnd / 2;
                cwnd = 1;
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
        // 不可能出现的现象， 但是为了防止出现
        if (ack >= base+windowSize*singlePacketSize) {
            return false;
        }

        // 结束了
        if (base > finalSeq) {
            return false;
        }

        if (ack == base - singlePacketSize) { // 快速重传
            repAck++;
            if (repAck == 3) {
                System.out.println("这里出现了冗余三次的快速重传，接下来要快恢复");
                repAck = 0;
                tcpSender.udt_send(dataMap.get(base)); // 这里出现了错误，没有发base
                ssthresh = cwnd / 2;
                cwnd = ssthresh;
                startTimer();
            } else if (cwnd < windowSize){
                // 收到冗余ack
                cwnd++;
            }
        } else {
            // 在这里接收到了合理的ack
            slide(ack);
            startTimer();
//            tahoe();
            reno();
        }
        return true;
    }

    void slide(int ack){
        for (; base <= ack; base += singlePacketSize) {
            dataMap.remove(base);
        }
    }

    void reno(){
        if (cwnd < windowSize){
            if (cwnd < ssthresh) {
                System.out.println("慢开始，cwnd = " + cwnd);
                cwnd*=2;
                System.out.println("慢开始，cwnd = " + cwnd);
            } else {
                System.out.println("拥塞避免，cwnd = " + cwnd);
                cwnd++;
                System.out.println("拥塞避免，cwnd = " + cwnd);
            }
        }
    }
}
