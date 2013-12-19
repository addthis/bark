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

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
    Adapted from Kafka zkutils circa 0.7.0
 */
public class ZkHelpers {

    private static final Logger logger = LoggerFactory.getLogger(ZkHelpers.class);

    /**
     * Makes sure a persistent path exists in ZK (creates the path if not exist).
     */
    public static void makeSurePersistentPathExists(ZkClient client, String path) {
        if (!client.exists(path)) {
            // won't throw NoNodeException or NodeExistsException
            client.createPersistent(path, true);
        }
    }

    private static void createParentPath(ZkClient client, String path) {
        String parentDir = path.substring(0, path.lastIndexOf('/'));
        if (!parentDir.isEmpty()) {
            client.createPersistent(parentDir, true);
        }
    }

    /**
     * Updates the value of a persistent node with the given path and data.
     *
     * This method creates parent directory if necessary. It never throws ZkNoNodeException or ZkNoNodeException.
     */
    public static void updatePersistentPath(ZkClient client, String path, String data) {
        try {
            client.writeData(path, data);
        } catch (ZkNoNodeException e) {
            createParentPath(client, path);
            try {
                client.createPersistent(path, data);
            } catch (ZkNodeExistsException e1) {
                client.writeData(path, data);
            } catch (Throwable e2) {
                throw e2;
            }
        } catch (Throwable e2) {
            throw e2;
        }
    }

    public static void deletePath(ZkClient client, String path) {
        try {
            client.delete(path);
        } catch (ZkNoNodeException e) {
            // this can happen during a connection loss event, return normally
            logger.info(path + " deleted during connection loss; this is ok");
        } catch (Throwable e2) {
            throw e2;
        }
    }

    public static void deletePathRecursive(ZkClient client, String path) {
        try {
            client.deleteRecursive(path);
        } catch (ZkNoNodeException e) {
            // this can happen during a connection loss event, return normally
            logger.info(path + " deleted during connection loss; this is ok");
        } catch (Throwable e2) {
            throw e2;
        }

    }

    /**
     * @throws ZkNoNodeException if path does not exist
     */
    public static String readData(ZkClient client, String path) {
        return client.readData(path);
    }

    /**
     * Similar to {@link #readData(ZkClient, String)} but returns <code>null</code> if path does not exist.
     */
    public static String readDataMaybeNull(ZkClient client, String path) {
        return client.readData(path, true);
    }

    /**
     * Checks if the given path exists.
     */
    public static boolean pathExists(ZkClient client, String path) {
        return client.exists(path);
    }

}
