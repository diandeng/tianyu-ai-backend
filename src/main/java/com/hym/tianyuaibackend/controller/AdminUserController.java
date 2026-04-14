package com.hym.tianyuaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hym.tianyuaibackend.entity.SysUser;
import com.hym.tianyuaibackend.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 管理员用户管理 前端控制器
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/admin/user")
@Tag(name = "管理员用户管理模块")
public class AdminUserController {

    @Autowired
    private ISysUserService sysUserService;

    /**
     * 获取用户列表（分页）
     */
    @GetMapping("/list")
    @Operation(summary = "获取用户列表")
    public Map<String, Object> getUserList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(SysUser::getNickname, keyword)
                    .or()
                    .like(SysUser::getOpenid, keyword);
        }

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        Page<SysUser> resultPage = sysUserService.page(page, queryWrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        data.put("pageNum", resultPage.getCurrent());
        data.put("pageSize", resultPage.getSize());
        data.put("pages", resultPage.getPages());

        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取用户详情")
    public Map<String, Object> getUserDetail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        SysUser user = sysUserService.getById(id);
        if (user == null) {
            result.put("code", 404);
            result.put("msg", "用户不存在");
            return result;
        }

        result.put("code", 200);
        result.put("data", user);
        return result;
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/update")
    @Operation(summary = "更新用户信息")
    public Map<String, Object> updateUser(@RequestBody Map<String, Object> updateData) {
        Map<String, Object> result = new HashMap<>();

        Object idObj = updateData.get("id");
        if (idObj == null) {
            result.put("code", 400);
            result.put("msg", "用户ID不能为空");
            return result;
        }
        Long userId = Long.parseLong(idObj.toString());

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            result.put("code", 404);
            result.put("msg", "用户不存在");
            return result;
        }

        // 更新允许修改的字段
        if (updateData.containsKey("nickname")) {
            user.setNickname((String) updateData.get("nickname"));
        }
        if (updateData.containsKey("avatar")) {
            user.setAvatar((String) updateData.get("avatar"));
        }
        if (updateData.containsKey("roleType")) {
            user.setRoleType((Integer) updateData.get("roleType"));
        }
        if (updateData.containsKey("farmLocation")) {
            user.setFarmLocation((String) updateData.get("farmLocation"));
        }
        if (updateData.containsKey("plantingYears")) {
            Object plantingYears = updateData.get("plantingYears");
            if (plantingYears != null) {
                user.setPlantingYears(Integer.parseInt(plantingYears.toString()));
            }
        }
        if (updateData.containsKey("farmSize")) {
            Object farmSize = updateData.get("farmSize");
            if (farmSize != null) {
                user.setFarmSize(Double.parseDouble(farmSize.toString()));
            }
        }
        if (updateData.containsKey("mainCrops")) {
            user.setMainCrops((List<String>) updateData.get("mainCrops"));
        }
        if (updateData.containsKey("signature")) {
            user.setSignature((String) updateData.get("signature"));
        }

        boolean success = sysUserService.updateById(user);
        if (success) {
            result.put("code", 200);
            result.put("msg", "更新成功");
            result.put("data", sysUserService.getById(userId));
        } else {
            result.put("code", 500);
            result.put("msg", "更新失败");
        }
        return result;
    }

    /**
     * 导出用户列表为CSV
     */
    @GetMapping("/export")
    @Operation(summary = "导出用户列表")
    public void exportUsers(HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        List<SysUser> users = sysUserService.list(queryWrapper);

        response.setContentType("text/csv;charset=GBK");
        response.setCharacterEncoding("GBK");
        response.setHeader("Content-Disposition", "attachment;filename=users.csv");

        PrintWriter writer = response.getWriter();
        // 写入BOM，解决Excel打开CSV中文乱码问题
        writer.write('\uFEFF');
        // 写入表头
        writer.println("ID,昵称,OpenID,角色,农场位置,种植年限,农场规模,主要作物,个性签名,创建时间,最后活跃");

        // 角色转换
        String[] roleNames = {"", "普通农户", "农技专家", "收购商"};

        // 写入数据
        for (SysUser user : users) {
            String roleName = user.getRoleType() != null && user.getRoleType() >= 1 && user.getRoleType() <= 3
                    ? roleNames[user.getRoleType()] : "";
            // mainCrops 是 List<String>，转为逗号分隔字符串
            String mainCropsStr = user.getMainCrops() != null ? String.join(",", user.getMainCrops()) : "";
            writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    user.getId(),
                    escapeCsv(user.getNickname()),
                    escapeCsv(user.getOpenid()),
                    roleName,
                    escapeCsv(user.getFarmLocation()),
                    user.getPlantingYears() != null ? user.getPlantingYears() : "",
                    user.getFarmSize() != null ? user.getFarmSize() : "",
                    escapeCsv(mainCropsStr),
                    escapeCsv(user.getSignature()),
                    user.getCreateTime() != null ? user.getCreateTime().toString() : "",
                    user.getUpdateTime() != null ? user.getUpdateTime().toString() : ""
            ));
        }
        writer.flush();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}