/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.common.protocol.header;

import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.annotation.CFNotNull;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;

/**
 * Created by suiyuzeng on 2018/8/7.
 */
public class GetBrokerMaxPhyOffsetResponseHeader implements CommandCustomHeader {
    @CFNotNull
    private Long maxPhyOffset;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public Long getMaxPhyOffset() {
        return maxPhyOffset;
    }

    public void setMaxPhyOffset(Long maxPhyOffset) {
        this.maxPhyOffset = maxPhyOffset;
    }
}
