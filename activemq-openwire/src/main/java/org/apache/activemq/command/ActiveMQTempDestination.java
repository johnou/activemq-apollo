/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.command;

import javax.jms.JMSException;
import org.apache.activemq.IConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @openwire:marshaller
 * @version $Revision: 1.5 $
 */
public abstract class ActiveMQTempDestination extends ActiveMQDestination {

    private static final Log LOG = LogFactory.getLog(ActiveMQTempDestination.class);
    protected transient IConnection connection;
    protected transient String connectionId;
    protected transient int sequenceId;

    public ActiveMQTempDestination() {
    }

    public ActiveMQTempDestination(String name) {
        super(name);
    }

    public ActiveMQTempDestination(String connectionId, long sequenceId) {
        super(connectionId + ":" + sequenceId);
    }

    public boolean isTemporary() {
        return true;
    }

    public void delete() throws JMSException {
        connection.deleteTempDestination(this);
    }

    public IConnection getConnection() {
        return connection;
    }

    public void setConnection(IConnection connection) {
        this.connection = connection;
    }

    public void setPhysicalName(String physicalName) {
        super.setPhysicalName(physicalName);
        if (!isComposite()) {
            // Parse off the sequenceId off the end.
            // this can fail if the temp destination is
            // generated by another JMS system via the JMS<->JMS Bridge
            int p = this.physicalName.lastIndexOf(":");
            if (p >= 0) {
                String seqStr = this.physicalName.substring(p + 1).trim();
                if (seqStr != null && seqStr.length() > 0) {
                    try {
                        sequenceId = Integer.parseInt(seqStr);
                    } catch (NumberFormatException e) {
                        LOG.debug("Did not parse sequence Id from " + physicalName);
                    }
                    // The rest should be the connection id.
                    connectionId = this.physicalName.substring(0, p);
                }
            }
        }
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public int getSequenceId() {
        return sequenceId;
    }
}