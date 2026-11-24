package com.oss.file.app.util;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * HTTP Range 支持：浏览器 <video>/<audio> 拖动进度条依赖 206 分段响应。
 * 无 Range 时返回 200 全量；非法 Range 返回 416。
 */
public final class RangeSupport {

    private RangeSupport() {}

    /**
     * 构建文件响应，自动处理 Range。
     *
     * @param path        物理文件
     * @param rangeHeader 请求头 Range 原值，可为 null
     * @param mediaType   响应类型
     * @param inline      true=内联预览，false=附件下载
     * @param downloadName 下载时的文件名（inline 可忽略）
     */
    public static ResponseEntity<?> respond(Path path, String rangeHeader, MediaType mediaType,
                                            boolean inline, String downloadName) throws IOException {
        if (!Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        long length = Files.size(path);
        Resource resource = new FileSystemResource(path.toFile());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set("Accept-Ranges", "bytes");
        headers.setCacheControl("public, max-age=3600");
        if (!inline) {
            String safe = downloadName == null || downloadName.isBlank() ? "download" : downloadName;
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(safe, java.nio.charset.StandardCharsets.UTF_8));
        }

        if (rangeHeader == null || rangeHeader.isBlank()) {
            headers.setContentLength(length);
            return ResponseEntity.ok().headers(headers).body(resource);
        }

        List<HttpRange> ranges;
        try {
            ranges = HttpRange.parseRanges(rangeHeader);
        } catch (IllegalArgumentException e) {
            return unsatisfiable(headers, length);
        }
        if (ranges.isEmpty()) {
            return ResponseEntity.ok().headers(headers).body(resource);
        }

        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(length);
        long end = range.getRangeEnd(length);
        if (start < 0 || start > end || start >= length) {
            return unsatisfiable(headers, length);
        }
        ResourceRegion region = new ResourceRegion(resource, start, end - start + 1);
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + length);
        headers.setContentLength(end - start + 1);
        return ResponseEntity.status(206).headers(headers).body(region);
    }

    private static ResponseEntity<?> unsatisfiable(HttpHeaders headers, long length) {
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + length);
        return ResponseEntity.status(416).headers(headers).build();
    }
}
