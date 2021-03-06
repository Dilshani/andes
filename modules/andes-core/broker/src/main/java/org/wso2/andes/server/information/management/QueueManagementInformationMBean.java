/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.andes.server.information.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.kernel.*;
import org.wso2.andes.management.common.mbeans.QueueManagementInformation;
import org.wso2.andes.management.common.mbeans.annotations.MBeanOperationParameter;
import org.wso2.andes.server.management.AMQManagedObject;
import org.wso2.andes.server.util.AndesUtils;

import javax.management.NotCompliantMBeanException;

import java.util.ArrayList;
import java.util.List;

public class QueueManagementInformationMBean extends AMQManagedObject implements QueueManagementInformation {

    private static Log log = LogFactory.getLog(QueueManagementInformationMBean.class);

    public QueueManagementInformationMBean() throws NotCompliantMBeanException {
        super(QueueManagementInformation.class, QueueManagementInformation.TYPE);
    }

    public String getObjectInstanceName() {
        return QueueManagementInformation.TYPE;
    }

    public synchronized String[] getAllQueueNames() {

        try {
            List<String> queuesList = AndesContext.getInstance().getAMQPConstructStore().getQueueNames();
            String[] queues= new String[queuesList.size()];
            queuesList.toArray(queues);
            return queues;

        } catch (Exception e) {
          throw new RuntimeException("Error in accessing destination queues",e);
        }

    }

    public boolean isQueueExists(String queueName) {
        try {
            List<String> queuesList = AndesContext.getInstance().getAMQPConstructStore().getQueueNames();
            return queuesList.contains(queueName);
        } catch (Exception e) {
          throw new RuntimeException("Error in accessing destination queues",e);
        }
    }

    @Override
    public void deleteAllMessagesInQueue(@MBeanOperationParameter(name = "queueName",
            description = "Name of the queue to delete messages from") String queueName) {
         //todo: we have to implement this 1. remove all messages in node queue 2.set counter to zero
        //todo: what happens if all messages were not copied to the node queue at the moment?
    }

    /**
     * Delete a selected message list from a given Dead Letter Queue of a tenant.
     *
     * @param messageIDs          The browser message Ids
     * @param deadLetterQueueName The Dead Letter Queue Name for the tenant
     */
    @Override
    public void deleteMessagesFromDeadLetterQueue(@MBeanOperationParameter(name = "messageIDs",
            description = "ID of the Messages to Be Deleted") String[] messageIDs,
                                                  @MBeanOperationParameter(name = "deadLetterQueueName",
            description = "The Dead Letter Queue Name for the selected tenant") String deadLetterQueueName) {
        List<Long> andesMessageIdList = getValidAndesMessageIdList(messageIDs);
        List<AndesRemovableMetadata> removableMetadataList = new ArrayList<AndesRemovableMetadata>(messageIDs.length);
        for (Long messageId : andesMessageIdList) {
            removableMetadataList.add(new AndesRemovableMetadata(messageId, deadLetterQueueName));
        }
        try {
            MessagingEngine.getInstance().deleteMessages(removableMetadataList, false);
        } catch (AndesException e) {
            throw new RuntimeException("Error deleting messages from Dead Letter Channel", e);
        }

        AndesUtils.unregisterBrowserMessageIds(messageIDs);

    }

    /**
     * Restore a given browser message Id list from the Dead Letter Queue to the same queue it was previous in before
     * moving to the Dead Letter Queue
     * and remove them from the Dead Letter Queue.
     *
     * @param messageIDs          The browser message Ids
     * @param deadLetterQueueName The Dead Letter Queue Name for the tenant
     */
    @Override
    public void restoreMessagesFromDeadLetterQueue(@MBeanOperationParameter(name = "messageIDs",
            description = "IDs of the Messages to Be Restored") String[] messageIDs,
                                                   @MBeanOperationParameter(name = "deadLetterQueueName",
            description = "The Dead Letter Queue Name for the selected tenant") String deadLetterQueueName) {
        List<Long> andesMessageIdList = getValidAndesMessageIdList(messageIDs);
        List<AndesMessageMetadata> metadataList = new ArrayList<AndesMessageMetadata>(andesMessageIdList.size());

        for (Long messageId : andesMessageIdList) {
            try {
                List<AndesMessageMetadata> messageMetadataListForOne = MessagingEngine.getInstance().getMetaDataList
                        (deadLetterQueueName, messageId, messageId);
                if (messageMetadataListForOne != null && messageMetadataListForOne.size() > 0) {
                    metadataList.add(messageMetadataListForOne.get(0));
                }
            } catch (AndesException e) {
                log.error("Error retrieving meta data for message Id " + messageId + " to restore messages from Dead " +
                        "Letter Channel.", e);
            }
        }

        try {
            MessagingEngine.getInstance().updateMetaDataInformation(deadLetterQueueName, metadataList);
        } catch (AndesException e) {
            throw new RuntimeException("Error restoring messages from " + deadLetterQueueName, e);
        }

        AndesUtils.unregisterBrowserMessageIds(messageIDs);

    }

    /**
     * Restore a given browser message Id list from the Dead Letter Queue to a different given queue in the same
     * tenant and remove them from the Dead Letter Queue.
     *
     * @param messageIDs          The browser message Ids
     * @param destination         The new destination
     * @param deadLetterQueueName The Dead Letter Queue Name for the tenant
     */
    @Override
    public void restoreMessagesFromDeadLetterQueue(@MBeanOperationParameter(name = "messageIDs",
            description = "IDs of the Messages to Be Restored") String[] messageIDs,
                                                   @MBeanOperationParameter(name = "destination",
            description = "Destination of the message to be restored") String destination,
                                                   @MBeanOperationParameter(name = "deadLetterQueueName",
            description = "The Dead Letter Queue Name for the selected tenant") String deadLetterQueueName) {
        List<Long> andesMessageIdList = getValidAndesMessageIdList(messageIDs);
        List<AndesMessageMetadata> metadataList = new ArrayList<AndesMessageMetadata>(andesMessageIdList.size());

        for (Long messageId : andesMessageIdList) {
            try {
                List<AndesMessageMetadata> messageMetadataListForOne = MessagingEngine.getInstance().getMetaDataList
                        (deadLetterQueueName, messageId, messageId);
                if (messageMetadataListForOne != null && messageMetadataListForOne.size() > 0) {
                    AndesMessageMetadata currentMetaData = messageMetadataListForOne.get(0);

                    // Set the new destination queue
                    currentMetaData.setDestination(destination);
                    currentMetaData.updateMetadata(destination);

                    metadataList.add(currentMetaData);
                }
            } catch (AndesException e) {
                log.error("Error retrieving meta data for message Id " + messageId + " to restore messages from Dead " +
                        "Letter Channel.", e);
            }
        }

        try {
            MessagingEngine.getInstance().updateMetaDataInformation(deadLetterQueueName, metadataList);
        } catch (AndesException e) {
            throw new RuntimeException("Error restoring messages from " + deadLetterQueueName + " to " + destination,
                    e);
        }

        AndesUtils.unregisterBrowserMessageIds(messageIDs);
    }

    /**
     * Retrieve a valid andes messageId list from a given browser message Id list.
     *
     * @param browserMessageIdList List of browser messageIds.
     * @return Valid Andes MessageId list
     */
    private List<Long> getValidAndesMessageIdList(String[] browserMessageIdList) {
        List<Long> andesMessageIdList = new ArrayList<Long>(browserMessageIdList.length);

        for (String browserMessageId : browserMessageIdList) {
            Long andesMessageId = AndesUtils.getAndesMessageId(browserMessageId);

            if (andesMessageId > 0) {
                andesMessageIdList.add(andesMessageId);
            } else {
                log.warn("A valid message could not be found for the message Id : " + browserMessageId);
            }
        }

        return andesMessageIdList;
    }

    //TODO:when deleting queues from UI this is not get called. Instead we use AMQBrokerManagerMBean. Why are we keeping this?
    public void deleteQueue(@MBeanOperationParameter(name = "queueName",
            description = "Name of the queue to be deleted") String queueName) {
/*    	SubscriptionStore subscriptionStore = AndesContext.getInstance().getSubscriptionStore();
        try {
            if(subscriptionStore.getActiveClusterSubscribersForDestination(queueName, false).size() >0) {
                throw new Exception("Queue" + queueName +" Has Active Subscribers. Please Stop Them First.");
            }
            //remove queue
            AndesContext.getInstance().getSubscriptionStore().removeQueue(queueName,false);
            //caller should remove messages from global queue
            ClusterResourceHolder.getInstance().getSubscriptionManager().handleMessageRemovalFromGlobalQueue(queueName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/
    }
    /**
     * We are returning message count to the UI from this method.
     * When it has received Acks from the clients more than the message actual
     * message in the  queue,( This can happen when a copy of a message get
     * delivered to the consumer while the ACK for the previouse message was
     * on the way back to server), Message count is becoming minus.
     *
     * So from now on , we ll not provide minus values to the front end since
     * it is not acceptable
     *
     * */
    public long getMessageCount(String queueName,String msgPattern) {

/*        int messageCount = (int) messageStore.getCassandraMessageCountForQueue(queueName);
        if (messageCount < 0) {
            messageStore.incrementMessageCountForQueue(queueName, Math.abs(messageCount));
            messageCount = 0;
        }
        return messageCount;*/
        if (log.isDebugEnabled()) {
            log.debug("Counting at queue : " + queueName);
        }

        long messageCount = 0;
        try {
        if (msgPattern.equals("queue")) {
            messageCount = MessagingEngine.getInstance().getMessageCountOfQueue(queueName);
        } else if (msgPattern.equals("topic")) {
            //TODO: hasitha - to implement
        }

        } catch (AndesException e) {
            throw new RuntimeException("Error retrieving message count for the queue : " + queueName, e);
        }

        return messageCount;
    }

    public int getSubscriptionCount( String queueName){
        try {
            return AndesContext.getInstance().getSubscriptionStore().numberOfSubscriptionsInCluster(queueName, false);
        } catch (Exception e) {
            throw new RuntimeException("Error in getting subscriber count",e);
        }
    }

}
