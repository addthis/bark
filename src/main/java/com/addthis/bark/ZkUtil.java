package com.addthis.bark;

import com.addthis.basis.util.Parameter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryOneTime;

public class ZkUtil {
    private static final String zkHosts = Parameter.value("zk.servers", "127.0.0.1:2181");
    private static final String zkChroot = Parameter.value("zk.chroot", "");
    private static final int zkSessionTimeout = Integer.parseInt(System.getProperty("zk.sessionTimeout", "180000"));
    private static final int zkConnectionTimeout = Integer.parseInt(System.getProperty("zk.connectionTimeout", "600000"));
    private static final int baseZkRetryTimeMs = Integer.parseInt(System.getProperty("zk.zkRetryTimeMs", "1000"));
    private static final int zkRetryMaxAttempts = Integer.parseInt(System.getProperty("zk.baseZkRetryMaxAttempts", "500"));


    // if the caller wants to close this later, that's their buisness.
    public static CuratorFramework makeStandardClient() {
        return makeCustomChrootClient(zkHosts, zkChroot);
    }

    public static CuratorFramework makeStandardClient(String zkHosts, boolean useChroot) {
        String chroot = (useChroot) ? "/" + zkChroot : "";
        return makeCustomChrootClient(zkHosts, chroot);
    }

    /**
     * The chroot znode *must* already exist.
     */

    public static CuratorFramework makeCustomChrootClient(String chroot) {
        return makeCustomChrootClient(zkHosts, chroot);
    }


    public static CuratorFramework makeCustomChrootClient(String zkHosts, String chroot) {
        CuratorFramework framework = CuratorFrameworkFactory.builder()
                .sessionTimeoutMs(zkSessionTimeout)
                .connectionTimeoutMs(zkConnectionTimeout)
                .connectString(zkHosts + "/" + chroot)
                .retryPolicy(new ExponentialBackoffRetry(baseZkRetryTimeMs, zkRetryMaxAttempts))
                .defaultData(null)
                .build();
        framework.start();
        return framework;
    }
}
