package com.changgou.file.controller;

import com.changgou.file.pojo.FastDFSFile;
import com.changgou.file.util.FastDFSClient;
import entity.Result;
import entity.StatusCode;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.file.controller
 * @date 2019-12-20
 */
@RestController
@CrossOrigin
public class FileController {

    @RequestMapping("upload")
    public Result<String> upload(MultipartFile file){
        try {
            //1、包装文件上传方法需要的参数
            String oldName = file.getOriginalFilename();
            //计算后缀名
            String extName = StringUtils.getFilenameExtension(oldName);
            FastDFSFile dfsFile = new FastDFSFile(
                    oldName,  //原来的文件名
                    file.getBytes(),  //用户上传文件的字节流
                    extName
            );
            //文件作者，可以从登录信息获取
            dfsFile.setAuthor("Steven");
            //2、调用FastDfsClient文件上传方法，完成文件上传到FastDfs中
            String[] upload = FastDFSClient.upload(dfsFile);
            //3、拼接url并返回
            //http://192.168.211.132:8080/group1/M00/00/00/wKjThF38K9SASVanAA832942OCg597.jpg
            //String url = "http://192.168.211.132:8080/" + upload[0] + "/" + upload[1];
            String url = FastDFSClient.getTrackerUrl() + upload[0] + "/" + upload[1];
            return new Result(true, StatusCode.OK, "文件上传成功",url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Result(false, StatusCode.ERROR, "文件上传失败");
    }
}
