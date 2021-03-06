/*
*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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
package org.wso2.andes.mqtt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dna.mqtt.wso2.AndesMQTTBridge;
import org.wso2.andes.kernel.*;
import org.wso2.andes.server.ClusterResourceHolder;

import java.nio.ByteBuffer;

/**
 * This class mainly focusses on negotiating the connections and exchanging data
 * The class will interface with the Andes kernal and will ensure that the information thats received from the bridge
 * is conforming to the data structure expected by the kernal, The basic operations done through this class will be
 * conbverting between the meta data and message content, indicate subscriptions and disconnections
 */

public class MQTTChannel {

    private static Log log = LogFactory.getLog(MQTTChannel.class);
    private static MQTTChannel instance = new MQTTChannel();
    private static final String MQTT_TOPIC_PREFIX = "MQTT_TOPIC_";

    /**
     * The class will be declared as singleton since only one channel shold be declared across the JVM
     */
    private MQTTChannel() {
    }

    /**
     * @return Retrun the instance which is delcalred
     */
    public static MQTTChannel getInstance() {
        return instance;
    }

    /**
     * Adds the message body to the andes kernal interfaces
     *
     * @param messageBody        message content
     * @param mqttLocalMessageID channel id in which the message was published
     */
    private void addMessageBody(AndesMessagePart messageBody, int mqttLocalMessageID) {
        MessagingEngine.getInstance().messageContentReceived(messageBody);
        if (log.isDebugEnabled()) {
            log.debug("Content of the message with id " + mqttLocalMessageID + " added to the kernal");
        }
    }

    /**
     * Adds the message meta data to the kernal
     *
     * @param messageHeader      the meta information of the specific mqtt message
     * @param mqttLocalMessageID the channel id which is generated through mqtt protocol engine
     * @throws MQTTException at an event where the kernal could not intert the header
     */
    private void addMessageHeader(AndesMessageMetadata messageHeader, int mqttLocalMessageID) throws MQTTException {
        try {

            MessagingEngine.getInstance().messageReceived(messageHeader);
            if (log.isDebugEnabled()) {
                log.debug("Message meta data added for the message with id " + mqttLocalMessageID);
            }

        } catch (AndesException e) {
            final String error = "Error while adding the message header to the andes kernal ";
            log.error(error + e.getMessage());
            throw new MQTTException(error, e);
        }
    }

    /**
     * Will add the message content which will be recived
     *
     * @param message            the content of the message which was published
     * @param messageID          the message idntifier
     * @param topic              the name of the topic which the message was published
     * @param qosLevel           the level of the qos the message was published
     * @param mqttLocalMessageID the channel id the subscriber is bound to
     * @param retain             whether the message requires to be persisted
     * @throws MQTTException occurs if there was an errro while adding the message content
     */
    public void addMessageContent(ByteBuffer message, long messageID, String topic, int qosLevel,
                                  int mqttLocalMessageID, boolean retain) throws MQTTException {
        //Will start converting the message body
        AndesMessagePart msg = MQTTUtils.convertToAndesMessage(message, messageID);
        //Will Create the Andes Header
        AndesMessageMetadata metaHeader = MQTTUtils.convertToAndesHeader(messageID, topic, qosLevel,
                message.array().length, retain);
        //Will write the message body
        addMessageBody(msg, mqttLocalMessageID);
        //Will add the message header
        addMessageHeader(metaHeader, mqttLocalMessageID);
    }

    /**
     * Will add and indicate the subscription to the kernal the bridge will be provided as the channel
     * since per topic we will only be creating one channel with andes
     *
     * @param channel  the bridge connection as the channel
     * @param topic    the name of the topic which has subscriber/s
     * @param clientID the id which will distinguish the topic channel
     */
    public void addSubscriber(AndesMQTTBridge channel, String topic, String clientID) throws MQTTException {
        //Will create a new local subscription object
        MQTTLocalSubscription localSubscription = new MQTTLocalSubscription(MQTT_TOPIC_PREFIX + topic);
        localSubscription.setMqqtServerChannel(channel);
        localSubscription.setTopic(topic);
        localSubscription.setSubscriptionID(clientID);
        //TODO need to investigate the times this should be false
        //TODO need to figure out the impact where theres a case which has multiple qos levels of subscription
        localSubscription.setIsActive(true);
        //Shold indicate the record in the cluster
        try {
            ClusterResourceHolder.getInstance().getSubscriptionManager().addSubscription(localSubscription);
            if (log.isDebugEnabled()) {
                log.debug("Subscription registered to the " + topic + " with channel id " + clientID);
            }
        } catch (AndesException e) {
            final String message = "Error ocured while creating the topic subscription in the kernal";
            log.error(message);
            throw new MQTTException(message, e);
        }
    }

    /**
     * Will trigger when subscriber disconnets from the session
     *
     * @param channel         the connection refference to the bridge
     * @param subscribedTopic the topic the subscription disconnection should be made
     * @param clientID        the channel id of the diconnection client
     */
    public void removeSubscriber(AndesMQTTBridge channel, String subscribedTopic, String clientID)
            throws MQTTException {
        try {

            //Will create a new local subscription object
            MQTTLocalSubscription localSubscription = new MQTTLocalSubscription(MQTT_TOPIC_PREFIX + subscribedTopic);
            localSubscription.setMqqtServerChannel(channel);
            localSubscription.setTopic(subscribedTopic);
            localSubscription.setSubscriptionID(clientID);
            localSubscription.setIsActive(false);
            ClusterResourceHolder.getInstance().getSubscriptionManager().closeLocalSubscription(localSubscription);
            if (log.isDebugEnabled()) {
                log.debug("Disconnected subscriber from topic " + subscribedTopic);
            }

        } catch (AndesException e) {
            final String message = "Error occured while removing the subscriber ";
            log.error(message + e.getMessage());
            throw new MQTTException(message, e);
        }
    }


}
