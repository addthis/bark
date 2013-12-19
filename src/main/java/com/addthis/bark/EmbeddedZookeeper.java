/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.bark;

import java.io.File;
import java.io.IOException;

import java.net.InetSocketAddress;

import com.google.common.io.Files;

import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ZooKeeperServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedZookeeper {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedZookeeper.class);

    private static final int START_RETRIES = 20;

    private File snapDir;
    private File logDir;

    private NIOServerCnxn.Factory connFactory;
    private int port;

    public EmbeddedZookeeper(int startPort) throws Exception {
        snapDir = Files.createTempDir();
        logDir = Files.createTempDir();
        int retries = 0;
        port = startPort;
        boolean success = false;
        while (true) {
            try {
                ZooKeeperServer zookeeper = new ZooKeeperServer(snapDir, logDir, 3000);
                connFactory = new NIOServerCnxn.Factory(new InetSocketAddress("127.0.0.1", port));
                connFactory.startup(zookeeper);
                success = true;
            } catch (IOException e) {
                if (retries++ >= START_RETRIES) {
                    logger.error("Max retries exceeded, fatal failure");
                } else {
                    port++;
                }
            } catch (InterruptedException e) {
                logger.error("Unable to start embedded zk because it was interrupted", e);
                throw e;
            }
            if (success) {
                break;
            }
        }

    }

    public int getPort() {
        return port;
    }

    public int getLocalPort() {
        return connFactory.getLocalPort();
    }

    public void shutdown() {
        connFactory.shutdown();
        try {
            com.addthis.basis.util.Files.deleteDir(snapDir);
            com.addthis.basis.util.Files.deleteDir(logDir);
        } catch (Exception e) {
            logger.error("failed to cleanup embedded zookeeper", e);
        }
    }
}
