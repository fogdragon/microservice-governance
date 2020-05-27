/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.storm.monitor.server.receiver;

import com.storm.monitor.server.model.CustomDataPickLog;
import com.storm.monitor.server.service.CustomDataPickLogService;
import com.storm.monitor.core.util.ApplicationContextUtil;
import com.storm.monitor.core.entity.MessageTree;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO调用消息处理线程
 * <ul>
 * <li> 1、20条数据则批量入库</li>
 * <li> 2、数据量大于0，但时间已经超过1秒，则也触发批量入库的操作</li>
 * <li> 3、批量入库操作后，计数器及计时器均重置为0
 * </ul>
 */
public class ConsumerTaskForDataPickMessage extends Thread {

    private static Logger LOG = LoggerFactory.getLogger(ConsumerTaskForDataPickMessage.class);

    private BlockingQueue<MessageTree> customPickDataQueue;
    
    private volatile long allSaveCount = 0;         //已成功保存的消息数量
    private volatile long errorSaveCount = 0;         //未能保存的消息数量

    public ConsumerTaskForDataPickMessage(BlockingQueue<MessageTree> customPickDataQueue) {
        this.customPickDataQueue = customPickDataQueue;
    }

    @Override
    public void run() {
        CustomDataPickLogService customDataPickLogService = null;
        List<CustomDataPickLog> currentSaveMsg = new ArrayList();
        
        long lastSaveTime = -1;        //最后一次保存的时间(毫秒值)
        int nullIdx = 0;               //连续从消息队列中获取null值（无值）的次数，连续5次获取到null值，则让线程睡眠5毫秒
        while (true) {
            try {
                //从消息队列中获取一个消息
                MessageTree event = customPickDataQueue.poll();

                if (event != null) {
                    CustomDataPickLog customDataPickLog = CustomDataPickLog.buildCustomDataPickLog(event);
                    currentSaveMsg.add(customDataPickLog);
                }

                int saveCount = 0;
                if (currentSaveMsg.size() >= 50
                    || (currentSaveMsg.size() > 0 && (System.currentTimeMillis() - lastSaveTime) >= 1000)) {   //待保存队列有超过50条记录，或者上次保存时间已经超过1秒 ，则进行一次保存
                    if (customDataPickLogService == null) {
                        customDataPickLogService = ApplicationContextUtil.getBean(CustomDataPickLogService.class);
                    }
                    //批量保存一次日志数据
                    saveCount = customDataPickLogService.addCustomDataPickLogBatch(currentSaveMsg);
                    if (saveCount > 0) {  //保存成功，则清空待保存消息队列，重置时间
                        allSaveCount += saveCount;
                        currentSaveMsg.clear();
                        lastSaveTime = System.currentTimeMillis();
                        LOG.info("++++++++++++++++++++++++++++++++++++++++++insert custom pick data:" + saveCount);
                    }
                }

                if (saveCount>0 && (allSaveCount % 1000L < 50L)) {
                    LOG.info("DataPickMessageConsumerTask,all save success message count is {},"
                        + "error save message count is {}", allSaveCount, errorSaveCount);
                }

                if (event == null && saveCount == 0) {   //消息队列为空，并且没有保存入库操作
                    nullIdx++;
                    if (nullIdx >= 5) {
                        nullIdx = 0;
                        Thread.sleep(5);  //连续5次取不到消息，则线程睡眠5毫秒
                    }
                } else {
                    event = null;
                    nullIdx = 0;
                }
            } catch (Exception e) {
                LOG.error("DataPickMessageConsumerTask is error,{}", e);
            }
            //保存失败，有两种处理方式，如果队列数不满500，可以下次再重复保存操作；如果队列数已经超过500，为了防止堆积，只能抛弃（清空）个队列，同时记录异常
            if (currentSaveMsg.size() > 500) {
                errorSaveCount += currentSaveMsg.size();
                currentSaveMsg.clear();
                lastSaveTime = System.currentTimeMillis();
                LOG.info("DaoMessageConsumerTask is error,batch save is error,"
                    + "current message queue count={},now clearAll", currentSaveMsg.size());
            }
        }

    }

    public long getAllSaveCount() {
        return allSaveCount;
    }

    public long getErrorSaveCount() {
        return errorSaveCount;
    }
    
    
}
