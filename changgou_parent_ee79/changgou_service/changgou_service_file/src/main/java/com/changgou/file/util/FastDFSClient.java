package com.changgou.file.util;

import com.changgou.file.pojo.FastDFSFile;
import com.sun.demo.jvmti.hprof.Tracker;
import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;
import org.springframework.core.io.ClassPathResource;

import java.io.*;

/**
 * 文件上传工具类
 * 实现了文件上传、下载、删除等等操作
 * 同时包装tarcker与storage服务地址信息的获取
 * @author Steven
 * @version 1.0
 * @description com.changgou.file.util
 * @date 2019-12-20
 */
public class FastDFSClient {

    //tracker配置只加载一次
    static {
        try {
            //1、获取配置文件路径-filePath = new ClassPathResource("fdfs_client.conf").getPath()
            String path = new ClassPathResource("fdfs_client.conf").getPath();
            //2、加载配置文件-ClientGlobal.init(配置文件路径)
            ClientGlobal.init(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取TrackerServer对象
     * 包括Tracker信息获取
     * @return
     */
    public static TrackerServer getTrackerServer(){
        TrackerServer trackerServer = null;
        try {
            //3、创建一个TrackerClient对象。直接new一个。
            TrackerClient trackerClient = new TrackerClient();
            //4、使用TrackerClient对象创建连接，getConnection获得一个TrackerServer对象。
            trackerServer = trackerClient.getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trackerServer;
    }

    /**
     * 获取StorageClient
     * 包含文件所有操作
     * @return
     */
    public static StorageClient getStorageClient(){
        TrackerServer trackerServer = getTrackerServer();
        //5、创建一个StorageClient对象，直接new一个，需要两个参数TrackerServer对象、null
        StorageClient storageClient = new StorageClient(trackerServer,null);
        return storageClient;
    }

    /**
     * 文件上传方法
     * @param fastDFSFile 原来的文件基本信息
     * @return
     */
    public static String[] upload(FastDFSFile fastDFSFile) {
        String[] uploadFile = new String[0];
        try {
            StorageClient storageClient = getStorageClient();
            //文件扩展信息
            NameValuePair[] meta_list = new NameValuePair[1];
            meta_list[0] = new NameValuePair("author", fastDFSFile.getAuthor());
            //文件上传到fastDfs
            uploadFile = storageClient.upload_file(fastDFSFile.getContent(), fastDFSFile.getExt(), meta_list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uploadFile;
    }

    /**
     * 获取文件信息
     * @param group_name 组名
     * @param remote_filename fildId
     * @return 文件信息
     */
    public static FileInfo getFileInfo(String group_name, String remote_filename){
        FileInfo info = null;
        try {
            info = getStorageClient().get_file_info(group_name, remote_filename);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        return info;
    }

    /**
     * 下载文件
     * @param group_name 组名
     * @param remote_filename fildId
     * @return 文件输入流
     */
    public static InputStream downloadFile(String group_name, String remote_filename){
        try {
            byte[] bytes = getStorageClient().download_file(group_name, remote_filename);
            return new ByteArrayInputStream(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 文件删除
     * @param group_name 组名
     * @param remote_filename fildId
     * @return 0代表删除成功 否则失败
     */
    public static int deleteFile(String group_name, String remote_filename){
        try {
            int flag = getStorageClient().delete_file(group_name, remote_filename);
            return flag;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * 获取组服务器相关信息
     * @param groupName 组名
     * @return 存储服务器信息
     */
    public static StorageServer getStorageServer(String groupName){
        StorageServer storageServer = null;
        try {
            TrackerClient trackerClient = new TrackerClient();
            storageServer = trackerClient.getStoreStorage(trackerClient.getConnection(), groupName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return storageServer;
    }

    /**
     * 获取组服务器相关信息
     * @param groupName 组名
     * @return 存储服务器信息
     */
    public static ServerInfo[] getServerInfo(String groupName,String fileName){
        try {
            TrackerClient trackerClient = new TrackerClient();
            ServerInfo[] infos = trackerClient.getFetchStorages(trackerClient.getConnection(), groupName, fileName);
            return infos;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取TrackerUrl
     * @return
     */
    public static String getTrackerUrl(){
        /*TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = trackerClient.getConnection();*/

        TrackerServer trackerServer = getTrackerServer();
        //http://192.168.211.132:8080/
        String url = "http://" + trackerServer.getInetSocketAddress().getHostString() + ":"
                + ClientGlobal.getG_tracker_http_port() + "/";
        return url;
    }


    public static void main(String[] args) throws Exception {
        //测试文件上传
        //allUpload();

        //测试获取文件信息
        //group1/M00/00/00/wKjThF38K9SASVanAA832942OCg597.jpg
        /*FileInfo info = getFileInfo("group1", "M00/00/00/wKjThF38K9SASVanAA832942OCg597.jpg");
        System.out.println(info);*/


        //测试文件下载
        /*InputStream is = downloadFile("group1", "M00/00/00/wKjThF38K9SASVanAA832942OCg597.jpg");
        //构建缓冲区
        byte[] buff = new byte[1024];
        //创建输出流
        OutputStream out = new FileOutputStream("D:/1.jpg");
        while (is.read(buff) > -1){
            out.write(buff);
        }
        out.close();
        is.close();*/


        //测试删除
        /*int flag = deleteFile("group1", "M00/00/00/wKjThF38K9SASVanAA832942OCg5979989.jpg");
        System.out.println(flag == 0 ? "删除成功" : "删除失败");*/

        //测试存储服务信息获取
        /*StorageServer storageServer = getStorageServer("group1");
        System.out.println("存储组的索引：" + storageServer.getStorePathIndex());
        System.out.println(storageServer.getInetSocketAddress());*/

        //获取存储服务列表
        /*ServerInfo[] infos = getServerInfo("group1", "M00/00/00/wKjThF38N_KAH4yVAAjgce6FsTU716.jpg");
        for (ServerInfo info : infos) {
            System.out.println(info.getIpAddr() + ":" + info.getPort());
        }*/

        //获取TrackerUrl
        System.out.println(getTrackerUrl());
    }

    /**
     * 文件上传完整api
     * @throws IOException
     * @throws MyException
     */
    private static void allUpload() throws IOException, MyException {
        //1、获取配置文件路径-filePath = new ClassPathResource("fdfs_client.conf").getPath()
        String path = new ClassPathResource("fdfs_client.conf").getPath();
        //2、加载配置文件-ClientGlobal.init(配置文件路径)
        ClientGlobal.init(path);
        //3、创建一个TrackerClient对象。直接new一个。
        TrackerClient trackerClient = new TrackerClient();
        //4、使用TrackerClient对象创建连接，getConnection获得一个TrackerServer对象。
        TrackerServer trackerServer = trackerClient.getConnection();
        //5、创建一个StorageClient对象，直接new一个，需要两个参数TrackerServer对象、null
        StorageClient storageClient = new StorageClient(trackerServer,null);

        //上传文件
        String[] uploadFile = storageClient.upload_file("D:\\WebWork\\360wallpaper (4).jpg", "jpg", null);
        for (String s : uploadFile) {
            System.out.println(s);
        }
    }
}
