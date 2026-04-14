package com.hym.tianyuaibackend.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class CosManager {
    @Value("${cos.secretId}")
    private String secretId;

    @Value("${cos.secretKey}")
    private String secretKey;

    @Value("${cos.region}")
    private String region;

    @Value("${cos.bucketName}")
    private String bucketName;

    @Value("${cos.baseUrl}")
    private String baseUrl;

    private COSClient cosClient;

    @PostConstruct
    public void init() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        cosClient = new COSClient(cred, clientConfig);
    }

    /**
     * 上传文件并返回访问 URL
     *
     * @param file       待上传的文件
     * @param pathPrefix 文件路径前缀
     * @return 文件访问 URL
     */
    public String uploadFile(MultipartFile file, String pathPrefix) {
        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String key = pathPrefix + UUID.randomUUID() + suffix;

            // 转换文件流 (COS 需要 File 对象或 InputStream)
            File localFile = File.createTempFile("temp", null);
            file.transferTo(localFile);

            // 上传
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
            cosClient.putObject(putObjectRequest);

            // 清理临时文件
            localFile.delete();

            // 返回完整访问路径
            return baseUrl + "/" + key;

        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }
}