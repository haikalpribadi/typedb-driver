/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package grakn.client.rpc;

import grakn.client.GraknClient;
import grakn.client.GraknOptions;
import grakn.common.concurrent.NamedThreadFactory;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.TimeUnit;

public class ClientRPC implements GraknClient {

    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final String THREAD_POOL_NAME = "grakn-client-rpc";
    private final ManagedChannel channel;
    private final DatabaseManagerRPC databases;

    public ClientRPC(String address) {
        channel = ManagedChannelBuilder
                .forTarget(address)
                .executor(new NioEventLoopGroup(THREAD_POOL_SIZE, NamedThreadFactory.create(THREAD_POOL_NAME)))
                .usePlaintext()
                .build();
        databases = new DatabaseManagerRPC(channel);
    }

    @Override
    public SessionRPC session(String database, GraknClient.Session.Type type) {
        return session(database, type, GraknOptions.core());
    }

    @Override
    public SessionRPC session(String database, GraknClient.Session.Type type, GraknOptions options) {
        return new SessionRPC(this, database, type, options);
    }

    @Override
    public DatabaseManagerRPC databases() {
        return databases;
    }

    @Override
    public boolean isOpen() {
        return !channel.isShutdown();
    }

    @Override
    public void close() {
        try {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Channel channel() {
        return channel;
    }
}