package com.hym.tianyuaibackend.controller;

import com.hym.tianyuaibackend.common.BizType;
import com.hym.tianyuaibackend.manager.CosManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/common")
@Tag(name = "通用功能模块")
public class CommonController {

    @Autowired
    private CosManager cosManager;

    /**
     * 通用上传接口
     *
     * @param file 文件
     * @param biz  业务类型: avatar/post/news/ai/temp
     */
    @PostMapping("/upload")
    @Operation(summary = "通用图片上传", description = "上传图片到腾讯云，返回URL")
    public Map<String, Object> upload(@RequestPart("file") MultipartFile file, @Parameter(description = "业务类型: avatar/post/news/ai") @RequestParam(value = "biz", defaultValue = "temp") String biz) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 根据 biz 获取存储路径前缀
            BizType bizType = BizType.fromCode(biz);
            String pathPrefix = bizType.getPathPrefix();

            // 执行上传
            String url = cosManager.uploadFile(file, pathPrefix);

            // 返回结果
            result.put("code", 200);
            result.put("msg", "上传成功");
            Map<String, Object> data = new HashMap<>();
            data.put("url", url);
            result.put("data", data);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "上传失败: " + e.getMessage());
        }
        return result;
    }
}
