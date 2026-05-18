package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.service.DataAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/data-assets")
@RequiredArgsConstructor
public class DataAssetController {

    private final DataAssetService dataAssetService;

    @GetMapping
    public Result<Map<String, Object>> overview(Principal principal) {
        return Result.success(dataAssetService.overview(principal));
    }

    @PostMapping("/sync-cloud")
    public Result<Map<String, Object>> syncCloud(
        Principal principal,
        @RequestBody(required = false) DataAssetService.SyncRequest request
    ) {
        return Result.success("数据资产已同步到云端", dataAssetService.syncToCloud(principal, request));
    }
}
