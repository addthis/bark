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

import com.google.common.io.Files;

import java.io.File;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.List;

import org.I0Itec.zkclient.ZkClient;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkCmdLine {

    private static final Logger logger = LoggerFactory.getLogger(ZkCmdLine.class);

    private static final int zkSessionTimeout = Integer.parseInt(System.getProperty("zk.sessionTimeout", "10000"));
    private static final int zkConnectionTimeout = Integer.parseInt(System.getProperty("zk.connectionTimeout", "60000"));

    private CommandLine cmdline;
    private ZkClient zkClient;


    public ZkCmdLine(CommandLine cmdline) {
        this.cmdline = cmdline;
        this.zkClient = new ZkClient(cmdline.getOptionValue("zk") + "/" + cmdline.getOptionValue("chroot"),
                zkSessionTimeout, zkConnectionTimeout, new StringSerializer());
    }

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "something helpful.");
        options.addOption("z", "zk", true, "zk servers,port");
        options.addOption("c", "chroot", true, "chroot");
        options.addOption("n", "znode", true, "znode");
        options.addOption("d", "dir", true, "directory root");
        options.addOption("p", "put-data", true, "directory root");
        // for copying
        options.addOption("", "to-zk", true, "zk servers,port");
        options.addOption("", "to-chroot", true, "chroot");
        options.addOption("", "to-znode", true, "znode");

        //options.addOption("t", "ktopic", true, "kafka topic");

        CommandLineParser parser = new PosixParser();
        CommandLine cmdline = null;
        try {
            cmdline = parser.parse(options, args);
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            System.exit(0);
        }

        HelpFormatter formatter = new HelpFormatter();
        if (cmdline.hasOption("help") || cmdline.getArgList().size() < 1) {
            System.out.println("commands: get jclean jcleanrecur kids grandkids");
            formatter.printHelp("ZkCmdLine", options);
            System.exit(0);
        }


        ZkCmdLine zkcl = new ZkCmdLine(cmdline);
        zkcl.runCmd((String) cmdline.getArgList().get(0));
    }

    public void runCmd(String cmd) throws Exception {
        if ("get".equals(cmd)) {
            cmdGet();
        } else if ("put".equals(cmd)) {
            cmdPut();
        } else if ("delete".equals(cmd)) {
            cmdDelete();
        } else if ("jclean".equals(cmd)) {
            cmdCleanJsonString();
        } else if ("jcleanrecur".equals(cmd)) {
            cmdCleanJsonStringRecursive();
        } else if ("kids".equals(cmd)) {
            cmdKids();
        } else if ("grandkids".equals(cmd)) {
            cmdGrandKids();
        } else if ("export".equals(cmd)) {
            cmdExport();
        } else if ("exportkids".equals(cmd)) {
            cmdExportChildren();
        } else if ("copy".equals(cmd)) {
            cmdCopyZnode();
        } else if ("copyrecur".equals(cmd)) {
            cmdCopyZnode();
        }

        // todo: import from files
    }

    public void cmdGet() {
        String data = ZkHelpers.readData(zkClient, cmdline.getOptionValue("znode"));
        System.out.println(data);
    }

    public void cmdPut() {
        String znode = cmdline.getOptionValue("znode");
        String data = null;
        if (cmdline.hasOption("put-data")) {
            data = cmdline.getOptionValue("put-data");
        }
        if (data == null) {
            ZkHelpers.makeSurePersistentPathExists(zkClient, znode);
        } else {
            ZkHelpers.updatePersistentPath(zkClient, znode, data);
        }
    }


    public void cmdDelete() {
        zkClient.delete(cmdline.getOptionValue("znode"));
    }


    public void cmdKids() {
        List<String> kids = zkClient.getChildren(cmdline.getOptionValue("znode"));
        System.out.println(kids);
    }

    public void cmdGrandKids() {
        System.out.println(getFamily(cmdline.getOptionValue("znode")));
    }


    public void cmdCleanJsonString() {
        cleanNodeJsonString(cmdline.getOptionValue("znode"));
    }


    public void cmdCleanJsonStringRecursive() {
        List<Object> graph = getFamily(cmdline.getOptionValue("znode"));
        cleanNodeJsonStringList(graph);
    }


    public void cmdExport() throws Exception {
        String path = cmdline.getOptionValue("znode");
        String dir = cmdline.getOptionValue("dir");
        exportPath(path, dir);
    }

    public void cmdExportChildren() throws Exception {
        String path = cmdline.getOptionValue("znode");
        String dir = cmdline.getOptionValue("dir");
        List<String> kids = zkClient.getChildren(path);
        System.out.println(kids);
        for (String kid : kids) {
            exportPath(path + "/" + kid, dir);
        }
    }


    public void cmdCopyZnode() throws Exception {
        String fromZnode = cmdline.getOptionValue("znode");
        String toZnode = fromZnode;
        if (cmdline.hasOption("to-znode")) {
            toZnode = cmdline.getOptionValue("to-znode");
        }

        ZkClient toZkClient = new ZkClient((cmdline.hasOption("to-zk") ? cmdline.getOptionValue("to-zk") : cmdline.getOptionValue("zk")) +
                                           "/" +
                                           (cmdline.hasOption("to-chroot") ? cmdline.getOptionValue("to-chroot") : cmdline.getOptionValue("chroot")),
                zkSessionTimeout, zkConnectionTimeout, new StringSerializer());

        copyZnode(fromZnode, toZnode, this.zkClient, toZkClient);
    }

    public void cmdCopyZnodeRecur() throws Exception {
        String fromZnode = cmdline.getOptionValue("znode");
        String toZnode = fromZnode;
        // use chroot for new root...
        // if (cmdline.hasOption("to-znode")) {
        //     toZnode = cmdline.getOptionValue("to-znode");
        // }

        ZkClient toZkClient = new ZkClient((cmdline.hasOption("to-zk") ? cmdline.getOptionValue("to-zk") : cmdline.getOptionValue("zk")) +
                                           "/" +
                                           (cmdline.hasOption("to-chroot") ? cmdline.getOptionValue("to-chroot") : cmdline.getOptionValue("chroot")),
                zkSessionTimeout, zkConnectionTimeout, new StringSerializer());

        List<String> znodes = flatten(getFamily(fromZnode));
        for (String znode : znodes) {
            copyZnode(znode, znode, this.zkClient, toZkClient);
        }
    }


    private void copyZnode(String fromZnode, String toZnode, ZkClient fromZkClient, ZkClient toZkClient) {
        String fromData = ZkHelpers.readData(zkClient, fromZnode);
        System.out.println("tonode: " + toZnode + " from data " + fromData);
        if (fromData == null) {
            ZkHelpers.makeSurePersistentPathExists(toZkClient, toZnode);
        } else {
            ZkHelpers.updatePersistentPath(toZkClient, toZnode, fromData);
        }
    }


    private void exportPath(String path, String dir) throws Exception {
        String data = ZkHelpers.readData(zkClient, path);
        if (data == null) {
            System.out.println("no data to export for " + path);
            return;
        }
        File outFile = new File(dir + path);
        Files.createParentDirs(outFile);
        Files.write(data, outFile, Charset.forName("UTF-8"));
    }


    private void cleanNodeJsonStringList(List<Object> graph) {
        for (Object o : graph) {
            if (o instanceof String) {
                cleanNodeJsonString((String) o);
            } else {
                cleanNodeJsonStringList((List) o);
            }
        }
    }

    private void cleanNodeJsonString(String path) {
        String data = ZkHelpers.readData(zkClient, path);
        if (data == null) {
            System.out.println("no data on path : " + path);
        } else {
            int idx = data.indexOf("{");
            if (idx == 0) {
                System.out.println("nothing to do");
                return;
            }
            System.out.println("Old Data on path  " + path + " : " + data);
            String newData = data.substring(idx);
            System.out.println("New Data on path  " + path + " : " + newData);
            zkClient.writeData(path, newData);
        }
    }


    private List<Object> getFamily(String path) {
        List<String> kids = zkClient.getChildren(path);
        List<Object> graph = new ArrayList<Object>();
        graph.add(path);
        for (String kid : kids) {
            String kidpath = path + "/" + kid;
            int numKids = zkClient.countChildren(kidpath);
            if (numKids == 0) {
                graph.add(kidpath);
            } else {
                graph.add(getFamily(kidpath));
            }
        }
        return graph;
    }

    public <T> List<T> flatten(List<?> list) {
        List<T> retVal = new ArrayList<T>();
        flatten(list, retVal);
        return retVal;
    }

    public <T> void flatten(List<?> fromTreeList, List<T> toFlatList) {
        for (Object item : fromTreeList) {
            if (item instanceof List<?>) {
                flatten((List<?>) item, toFlatList);
            } else {
                toFlatList.add((T) item);
            }
        }
    }

}
