package com.oss.file.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oss.file.common.exception.BusinessException;
import com.oss.file.common.util.FileHashUtil;
import com.oss.file.system.dto.GalleryAssetResponse;
import com.oss.file.system.entity.GalleryAsset;
import com.oss.file.system.entity.SysUser;
import com.oss.file.system.mapper.GalleryAssetMapper;
import com.oss.file.system.mapper.SysUserMapper;
import com.oss.file.system.service.GalleryAssetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 图床业务实现：图片/视频独立存储、在线预览、秒传去重。
 * 存储根目录与文件托管分离（gallery.storage.path），互不干扰。
 */
@Service
@Slf4j
public class GalleryAssetServiceImpl extends ServiceImpl<GalleryAssetMapper, GalleryAsset>
        implements GalleryAssetService {

    private static final Pattern SAFE_EXTENSION = Pattern.compile("^\\.[A-Za-z0-9]{1,10}$");

    /** 图床只允许图片与视频，杜绝脚本/可执行文件 */
    private static final Set<String> ALLOWED_IMAGE_MIME = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/heic", "image/avif");
    private static final Set<String> ALLOWED_VIDEO_MIME = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-matroska", "video/x-msvideo", "video/3gpp");

    /** HEIC/AVIF 等 Java  undecodable 类型读取尺寸失败时忽略，不影响上传 */
    private static final Set<String> READABLE_IMAGE_MIME = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp");

    @Value("${gallery.storage.path:C:/IT-Gallery}")
    private String storageBasePath;

    private final SysUserMapper sysUserMapper;

    public GalleryAssetServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GalleryAssetResponse upload(MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传内容为空");
        }

        String original = file.getOriginalFilename();
        String extension = safeExtension(original);
        String sniffed = sniffMime(file, extension);
        String mediaType = sniffed.startsWith("image/") ? "IMAGE"
                : sniffed.startsWith("video/") ? "VIDEO" : null;
        if (mediaType == null || !(ALLOWED_IMAGE_MIME.contains(sniffed) || ALLOWED_VIDEO_MIME.contains(sniffed))) {
            throw new BusinessException("图床仅支持图片与视频格式（JPG/PNG/GIF/WEBP/MP4/WEBM 等）");
        }

        String fileHash;
        try (InputStream in = file.getInputStream()) {
            fileHash = FileHashUtil.calculateSHA256(in);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new BusinessException("哈希算法不可用");
        }

        // 秒传：当前用户 + 哈希已存在且物理文件在，直接返回
        GalleryAsset existing = findActive(fileHash, userId);
        if (existing != null && Files.isRegularFile(Paths.get(existing.getStoredPath()))) {
            log.info("图床秒传命中: user={}, hash={}", userId, fileHash);
            return toResponse(existing);
        }

        Path root = Paths.get(storageBasePath).toAbsolutePath().normalize();
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path storageDir = root.resolve(mediaType.toLowerCase(Locale.ROOT)).resolve(dateFolder).normalize();
        Path target = storageDir.resolve(userId + "_" + fileHash + extension).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("非法的存储路径");
        }

        Files.createDirectories(storageDir);
        Path temp = storageDir.resolve(fileHash + "-" + UUID.randomUUID().toString().replace("-", "") + ".part");
        try {
            file.transferTo(temp.toFile());

            Integer width = null;
            Integer height = null;
            if ("IMAGE".equals(mediaType) && READABLE_IMAGE_MIME.contains(sniffed)) {
                try {
                    BufferedImage image = ImageIO.read(temp.toFile());
                    if (image != null) {
                        width = image.getWidth();
                        height = image.getHeight();
                    }
                } catch (Exception e) {
                    log.debug("读取图片尺寸失败，忽略: {}", original);
                }
            }

            GalleryAsset asset;
            if (existing != null) {
                asset = existing;
                applyMeta(asset, file, original, fileHash, sniffed, mediaType, target, userId, width, height);
                baseMapper.updateById(asset);
            } else {
                GalleryAsset deleted = baseMapper.selectDeletedByHash(userId, fileHash);
                if (deleted != null) {
                    asset = deleted;
                    applyMeta(asset, file, original, fileHash, sniffed, mediaType, target, userId, width, height);
                    baseMapper.updateById(asset);
                } else {
                    asset = new GalleryAsset();
                    applyMeta(asset, file, original, fileHash, sniffed, mediaType, target, userId, width, height);
                    try {
                        baseMapper.insert(asset);
                    } catch (DuplicateKeyException dup) {
                        Files.deleteIfExists(temp);
                        GalleryAsset winner = findActive(fileHash, userId);
                        if (winner != null && Files.isRegularFile(Paths.get(winner.getStoredPath()))) {
                            log.info("图床并发重复上传已幂等处理: hash={}", fileHash);
                            return toResponse(winner);
                        }
                        throw new BusinessException("素材已存在，请重试");
                    }
                }
            }

            moveAtomically(temp, target);
            log.info("图床素材已保存: {} ({})", target, mediaType);
            return toResponse(asset);
        } catch (BusinessException e) {
            Files.deleteIfExists(temp);
            throw e;
        } catch (Exception e) {
            Files.deleteIfExists(temp);
            log.error("图床上传失败", e);
            throw new BusinessException("图床上传失败");
        }
    }

    @Override
    public List<GalleryAssetResponse> listAssets(Long userId, int page, int size, String mediaType) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Page<GalleryAsset> pageParam = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<GalleryAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GalleryAsset::getOwnerUserId, userId);
        if (mediaType != null && !mediaType.isBlank()) {
            wrapper.eq(GalleryAsset::getMediaType, mediaType.toUpperCase(Locale.ROOT));
        }
        wrapper.orderByDesc(GalleryAsset::getCreateTime);
        return baseMapper.selectPage(pageParam, wrapper).getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public GalleryAsset getAsset(Long id) {
        return id == null ? null : baseMapper.selectById(id);
    }

    @Override
    public Map<String, Object> stats(Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<GalleryAsset> assets = baseMapper.selectList(
                new LambdaQueryWrapper<GalleryAsset>().eq(GalleryAsset::getOwnerUserId, userId));
        long imageCount = assets.stream().filter(a -> "IMAGE".equals(a.getMediaType())).count();
        long videoCount = assets.stream().filter(a -> "VIDEO".equals(a.getMediaType())).count();
        long totalSize = assets.stream().mapToLong(a -> a.getFileSize() == null ? 0L : a.getFileSize()).sum();
        result.put("total", assets.size());
        result.put("imageCount", imageCount);
        result.put("videoCount", videoCount);
        result.put("totalSize", totalSize);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAsset(Long id, Long userId) {
        GalleryAsset asset = baseMapper.selectById(id);
        if (asset == null) {
            throw new BusinessException("素材不存在");
        }
        if (!userId.equals(asset.getOwnerUserId()) && !isAdmin(userId)) {
            throw new BusinessException(403, "无权删除此素材");
        }
        GalleryAsset update = new GalleryAsset();
        update.setId(id);
        update.setIsDeleted(1);
        update.setDeleteTime(LocalDateTime.now());
        baseMapper.updateById(update);
        try {
            Path root = Paths.get(storageBasePath).toAbsolutePath().normalize();
            Path filePath = Paths.get(asset.getStoredPath()).normalize();
            if (filePath.startsWith(root)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("图床物理文件删除失败: {}", asset.getStoredPath());
        }
        return true;
    }

    @Override
    public GalleryAssetResponse toResponse(GalleryAsset asset) {
        GalleryAssetResponse response = new GalleryAssetResponse();
        response.setId(asset.getId());
        response.setFileName(asset.getFileName());
        response.setFileHash(asset.getFileHash());
        response.setMimeType(asset.getMimeType());
        response.setMediaType(asset.getMediaType());
        response.setFileSize(asset.getFileSize());
        response.setWidth(asset.getWidth());
        response.setHeight(asset.getHeight());
        response.setDuration(asset.getDuration());
        response.setPreviewUrl("/api/gallery/preview/" + asset.getId());
        response.setRawUrl("/api/gallery/raw/" + asset.getId());
        response.setCreateTime(asset.getCreateTime());
        return response;
    }

    /* ---------------- 私有工具 ---------------- */

    private GalleryAsset findActive(String fileHash, Long userId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<GalleryAsset>()
                .eq(GalleryAsset::getFileHash, fileHash)
                .eq(GalleryAsset::getOwnerUserId, userId));
    }

    private void applyMeta(GalleryAsset a, MultipartFile file, String original, String hash, String mime,
                           String mediaType, Path target, Long userId, Integer width, Integer height) {
        a.setOwnerUserId(userId);
        a.setFileHash(hash);
        a.setFileName(sanitize(original));
        a.setStoredPath(target.toString());
        a.setMimeType(mime);
        a.setMediaType(mediaType);
        a.setFileSize(file.getSize());
        a.setWidth(width);
        a.setHeight(height);
        a.setIsDeleted(0);
        a.setDeleteTime(null);
        a.setUpdateTime(LocalDateTime.now());
        if (a.getCreateTime() == null) {
            a.setCreateTime(LocalDateTime.now());
        }
    }

    /**
     * 服务端按文件魔数嗅探真实类型，并与扩展名交叉校验，二者必须一致，
     * 防止把脚本伪装成 .png 上传后在图床中被当作静态资源执行。
     */
    private String sniffMime(MultipartFile file, String extension) throws IOException {
        String fromName = mimeByExtension(extension);
        if (fromName == null) {
            throw new BusinessException("不支持的图床文件类型");
        }
        byte[] header = new byte[16];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(header);
        }
        if (read < 4) {
            throw new BusinessException("文件内容为空或过小");
        }
        String magic = sniffByMagic(header);
        if (magic != null) {
            boolean isImageFamily = fromName.startsWith("image/");
            boolean magicImage = magic.startsWith("image/");
            if (isImageFamily != magicImage) {
                throw new BusinessException("文件类型与扩展名不一致，已拒绝上传");
            }
            return magic;
        }
        // 未识别魔数（如部分 mkv/webm 变体）则回退到扩展名映射，仍受白名单约束
        return fromName;
    }

    /** 依据文件头魔数识别真实 MIME，识别不了返回 null */
    private String sniffByMagic(byte[] h) {
        if (h[0] == (byte) 0xFF && h[1] == (byte) 0xD8 && h[2] == (byte) 0xFF) return "image/jpeg";
        if (h[0] == (byte) 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "image/png";
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') return "image/gif";
        if (h[0] == 'B' && h[1] == 'M') return "image/bmp";
        if (h.length > 11 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p') {
            if (h[8] == 'i' && h[9] == 's' && h[10] == 'o') return "image/heic";
            if (h[8] == 'a' && h[9] == 'v' && h[10] == 'i' && h[11] == 'f') return "image/avif";
            return "video/mp4";
        }
        if (h.length > 11 && h[0] == (byte) 0x1A && h[1] == (byte) 0x45 && h[2] == (byte) 0xDF && h[3] == (byte) 0xA3) {
            return "video/webm";
        }
        if (h.length > 3 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F') return "video/avi";
        return null;
    }

    private String mimeByExtension(String ext) {
        if (ext == null) return null;
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".bmp" -> "image/bmp";
            case ".heic" -> "image/heic";
            case ".avif" -> "image/avif";
            case ".mp4" -> "video/mp4";
            case ".webm" -> "video/webm";
            case ".mov" -> "video/quicktime";
            case ".mkv" -> "video/x-matroska";
            case ".avi" -> "video/x-msvideo";
            case ".3gp" -> "video/3gpp";
            default -> null;
        };
    }

    private String safeExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException("文件缺少扩展名");
        }
        String ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        if (!SAFE_EXTENSION.matcher(ext).matches()) {
            throw new BusinessException("非法的文件扩展名");
        }
        return ext;
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) return "untitled";
        String clean = fileName.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();
        return clean.length() > 200 ? clean.substring(0, 200) : clean;
    }

    private void moveAtomically(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean isAdmin(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }
}
