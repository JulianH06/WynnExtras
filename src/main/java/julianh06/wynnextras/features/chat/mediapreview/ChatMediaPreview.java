package julianh06.wynnextras.features.chat.mediapreview;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatMediaPreview {
    private static final Pattern URL_PATTERN = Pattern.compile("https://[^\\s<>{}\"']+(?:(?:\\R[^\\p{ASCII}\\r\\n]+[ \\t]*|\\R[ \\t]*(?=-))[^\\s<>{}\"']+)*");
    private static final Pattern URL_WRAP_PATTERN = Pattern.compile("\\R(?:[^\\p{ASCII}\\r\\n]+[ \\t]*|[ \\t]*(?=-))");
    private static final Pattern CHAT_SENDER_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])([A-Za-z0-9_]{3,16}):\\s");
    private static final Pattern TENOR_IMAGE_META_PATTERN = Pattern.compile("<meta\\b[^>]*(?:property|name)=\"(?:og:image|twitter:image)\"[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_ATTRIBUTE_PATTERN = Pattern.compile("\\bcontent=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final int MAX_CACHE_ENTRIES = 20;
    private static final int MAX_PENDING_LOADS = 8;
    private static final int MAX_URL_LENGTH = 2048;
    private static final int REQUEST_TIMEOUT_SECONDS = 8;
    private static final long MAX_HTML_DOWNLOAD_BYTES = 512 * 1024L;
    private static final long MAX_GIF_DECODED_BYTES = 64 * 1024 * 1024L;
    private static final long MAX_CACHED_DECODED_BYTES = 128 * 1024 * 1024L;
    private static final long AUTO_PREVIEW_IMAGE_DURATION_MS = 5000;
    private static final Set<String> TRUSTED_HOSTS = Set.of(
            "cdn.discordapp.com",
            "media.discordapp.net",
            "i.imgur.com",
            "imgur.com",
            "tenor.com",
            "www.tenor.com",
            "c.tenor.com",
            "media.tenor.com"
    );
    private static final RenderPipeline PREVIEW_PIPELINE = RenderPipelines.GUI_TEXTURED;
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            2,
            2,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING_LOADS),
            new PreviewThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
    );
    private static final Semaphore DECODE_SLOTS = new Semaphore(2);
    private static final AtomicInteger TEXTURE_COUNTER = new AtomicInteger();
    private static final Object CACHE_LOCK = new Object();
    private static final LinkedHashMap<String, PreviewEntry> CACHE = new LinkedHashMap<>(32, 0.75f, true);
    private static volatile PreviewEntry automaticPreviewEntry;
    private static String hoveredPreviewKey;
    private static long hoveredPreviewStartedAtMs;

    private ChatMediaPreview() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderAutomatic(context));
    }

    public static Text processMessage(Text message) {
        if (message == null || !WynnExtrasConfig.INSTANCE.chatMediaPreviewEnabled) return message;
        String plainMessage = message.getString();
        if (!plainMessage.contains("https://")) return message;

        MutableText result = Text.empty();
        List<StyledSegment> segments = new ArrayList<>();
        StringBuilder rawMessage = new StringBuilder();
        message.visit((style, string) -> {
            int start = rawMessage.length();
            rawMessage.append(string);
            segments.add(new StyledSegment(start, rawMessage.length(), style));
            return Optional.empty();
        }, Style.EMPTY);
        appendWithMediaLinks(result, rawMessage.toString(), segments);
        return result;
    }

    public static void render(DrawContext context, int mouseX, int mouseY) {
        if (!WynnExtrasConfig.INSTANCE.chatMediaPreviewEnabled) {
            resetHoveredPreview();
            return;
        }
        Style style = getStyleAt(mouseX, mouseY);
        URI uri = getAllowedMediaUri(style);
        if (uri == null) {
            resetHoveredPreview();
            return;
        }

        String key = normalizeKey(uri);
        if (!key.equals(hoveredPreviewKey)) {
            hoveredPreviewKey = key;
            hoveredPreviewStartedAtMs = 0;
        }
        PreviewEntry entry = getEntry(key);
        if (entry == null && WynnExtrasConfig.INSTANCE.chatMediaPreviewLoadPolicy == WynnExtrasConfig.ChatMediaPreviewLoadPolicy.HOVER) {
            entry = startLoading(uri);
        }

        if (entry == null) {
            drawStatus(context, mouseX, mouseY, "Click to load preview");
            return;
        }
        if (entry.status == Status.LOADING) {
            drawStatus(context, mouseX, mouseY, "Loading preview...");
            return;
        }
        if (entry.status == Status.FAILED) {
            drawStatus(context, mouseX, mouseY, entry.errorMessage == null ? "Preview unavailable" : entry.errorMessage);
            return;
        }
        if (hoveredPreviewStartedAtMs == 0) {
            hoveredPreviewStartedAtMs = System.currentTimeMillis();
        }
        drawPreview(context, entry, WynnExtrasConfig.INSTANCE.chatMediaPreviewHoverPosition, hoveredPreviewStartedAtMs, null);
    }

    private static void resetHoveredPreview() {
        hoveredPreviewKey = null;
        hoveredPreviewStartedAtMs = 0;
    }

    private static void showAutomatically(URI uri, String sender) {
        PreviewEntry entry = startLoading(uri);
        if (entry.automaticPreviewConsumed) return;
        entry.automaticPreviewStartedAtMs = 0;
        entry.automaticPreviewSender = sender == null ? "unknown" : sender;
        automaticPreviewEntry = entry;
    }

    private static void renderAutomatic(DrawContext context) {
        if (!WynnExtrasConfig.INSTANCE.chatMediaPreviewEnabled || !WynnExtrasConfig.INSTANCE.chatMediaPreviewAutoDisplay) {
            automaticPreviewEntry = null;
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options.hudHidden) return;

        PreviewEntry entry = automaticPreviewEntry;
        if (entry == null || entry.status == Status.LOADING) return;
        if (entry.status == Status.FAILED) {
            if (automaticPreviewEntry == entry) automaticPreviewEntry = null;
            return;
        }

        long now = System.currentTimeMillis();
        if (entry.automaticPreviewStartedAtMs == 0) {
            entry.automaticPreviewStartedAtMs = now;
        }
        long duration = entry.animated ? entry.totalDurationMs() : AUTO_PREVIEW_IMAGE_DURATION_MS;
        if (now - entry.automaticPreviewStartedAtMs >= duration) {
            entry.automaticPreviewConsumed = true;
            if (automaticPreviewEntry == entry) automaticPreviewEntry = null;
            return;
        }
        drawPreview(context, entry, WynnExtrasConfig.INSTANCE.chatMediaPreviewPosition, entry.automaticPreviewStartedAtMs, entry.automaticPreviewSender);
    }

    public static boolean mouseClicked(Click click) {
        if (!WynnExtrasConfig.INSTANCE.chatMediaPreviewEnabled) return false;
        if (WynnExtrasConfig.INSTANCE.chatMediaPreviewLoadPolicy != WynnExtrasConfig.ChatMediaPreviewLoadPolicy.CLICK_TO_LOAD) return false;
        if (click.button() != 0) return false;

        Style style = getStyleAt((int) click.x(), (int) click.y());
        URI uri = getAllowedMediaUri(style);
        if (uri == null) return false;

        PreviewEntry entry = getEntry(normalizeKey(uri));
        if (entry == null || entry.status == Status.FAILED) {
            startLoading(uri);
        }
        return true;
    }

    public static boolean handleClick(Style style) {
        if (!WynnExtrasConfig.INSTANCE.chatMediaPreviewEnabled) return false;
        if (WynnExtrasConfig.INSTANCE.chatMediaPreviewLoadPolicy != WynnExtrasConfig.ChatMediaPreviewLoadPolicy.CLICK_TO_LOAD) return false;
        URI uri = getAllowedMediaUri(style);
        if (uri == null) return false;

        PreviewEntry entry = getEntry(normalizeKey(uri));
        if (entry == null || entry.status == Status.FAILED) {
            startLoading(uri);
        }
        return true;
    }

    private static void appendWithMediaLinks(MutableText result, String string, List<StyledSegment> segments) {
        Matcher matcher = URL_PATTERN.matcher(string);
        int cursor = 0;
        String sender = null;
        boolean senderResolved = false;
        boolean autoDisplay = WynnExtrasConfig.INSTANCE.chatMediaPreviewAutoDisplay;
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                appendStyledRange(result, string, segments, cursor, matcher.start());
            }

            String raw = matcher.group();
            String normalized = unwrapUrl(raw);
            String url = trimTrailingPunctuation(normalized);
            String suffix = normalized.substring(url.length());
            URI uri = parseAllowedMediaUri(url);
            if (uri == null) {
                appendStyledRange(result, string, segments, matcher.start(), matcher.end());
            } else {
                if (autoDisplay) {
                    if (!senderResolved) {
                        sender = extractSender(string);
                        senderResolved = true;
                    }
                    showAutomatically(uri, sender);
                }
                Style linkStyle = styleAt(segments, matcher.start())
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.OpenUrl(uri));
                result.append(Text.literal(url).setStyle(linkStyle));
                if (!suffix.isEmpty()) {
                    result.append(Text.literal(suffix).setStyle(styleAt(segments, matcher.end() - 1)));
                }
            }
            cursor = matcher.end();
        }
        if (cursor < string.length()) {
            appendStyledRange(result, string, segments, cursor, string.length());
        }
    }

    private static void appendStyledRange(MutableText result, String string, List<StyledSegment> segments, int start, int end) {
        for (StyledSegment segment : segments) {
            int segmentStart = Math.max(start, segment.start);
            int segmentEnd = Math.min(end, segment.end);
            if (segmentStart < segmentEnd) {
                result.append(Text.literal(string.substring(segmentStart, segmentEnd)).setStyle(segment.style));
            }
        }
    }

    private static Style styleAt(List<StyledSegment> segments, int index) {
        for (StyledSegment segment : segments) {
            if (index >= segment.start && index < segment.end) return segment.style;
        }
        return Style.EMPTY;
    }

    private static String unwrapUrl(String url) {
        return URL_WRAP_PATTERN.matcher(url).replaceAll("");
    }

    private static String extractSender(String message) {
        int mediaStart = message.indexOf("https://");
        Matcher matcher = CHAT_SENDER_PATTERN.matcher(mediaStart < 0 ? message : message.substring(0, mediaStart));
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String trimTrailingPunctuation(String url) {
        int end = url.length();
        while (end > 0 && ".,!?;:)]".indexOf(url.charAt(end - 1)) >= 0) {
            end--;
        }
        return url.substring(0, end);
    }

    private static Style getStyleAt(int mouseX, int mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || !(mc.currentScreen instanceof ChatScreen) || mc.inGameHud == null || mc.textRenderer == null) return null;

        ChatHud chatHud = mc.inGameHud.getChatHud();
        int screenHeight = mc.getWindow().getScaledHeight();
        DrawnTextConsumer.ClickHandler handler = new DrawnTextConsumer.ClickHandler(mc.textRenderer, mouseX, mouseY);
        handler.insert(false);
        chatHud.render(handler, screenHeight, mc.inGameHud.getTicks(), true);
        return handler.getStyle();
    }

    private static URI getAllowedMediaUri(Style style) {
        if (style == null || !(style.getClickEvent() instanceof ClickEvent.OpenUrl(URI uri))) return null;
        return parseAllowedMediaUri(uri.toString());
    }

    private static URI parseAllowedMediaUri(String value) {
        try {
            if (value.length() > MAX_URL_LENGTH) return null;
            URI uri = withoutFragment(URI.create(value).normalize());
            if (!isSafeNetworkUri(uri)) return null;
            if (!hasSupportedMediaExtension(uri) && !isTenorViewUri(uri)) return null;
            return uri;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String normalizedHost(URI uri) {
        String host = uri.getHost();
        return host == null ? "" : host.toLowerCase(Locale.ROOT);
    }

    private static boolean isAllowedHost(String host) {
        return TRUSTED_HOSTS.contains(host) || isTenorHost(host);
    }

    private static boolean isTenorHost(String host) {
        if (TRUSTED_HOSTS.contains(host) && host.endsWith("tenor.com")) return true;
        if (!host.startsWith("media") || !host.endsWith(".tenor.com")) return false;
        String number = host.substring("media".length(), host.length() - ".tenor.com".length());
        if (number.isEmpty()) return false;
        for (int i = 0; i < number.length(); i++) {
            if (!Character.isDigit(number.charAt(i))) return false;
        }
        return true;
    }

    private static boolean isTenorMediaHost(String host) {
        return host.equals("c.tenor.com")
                || host.equals("media.tenor.com")
                || host.startsWith("media") && host.endsWith(".tenor.com") && isTenorHost(host);
    }

    private static boolean isSafeNetworkUri(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme())
                && uri.getUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443)
                && isAllowedHost(normalizedHost(uri));
    }

    private static URI withoutFragment(URI uri) {
        if (uri.getRawFragment() == null) return uri;
        try {
            return new URI(uri.getScheme(), uri.getRawAuthority(), uri.getRawPath(), uri.getRawQuery(), null);
        } catch (Exception ignored) {
            return uri;
        }
    }

    private static boolean isTenorViewUri(URI uri) {
        String host = normalizedHost(uri);
        String path = uri.getPath();
        return (host.equals("tenor.com") || host.equals("www.tenor.com"))
                && path != null
                && path.contains("/view/");
    }

    private static boolean isTenorPageUri(URI uri) {
        String host = normalizedHost(uri);
        return host.equals("tenor.com") || host.equals("www.tenor.com");
    }

    private static boolean hasSupportedMediaExtension(URI uri) {
        String path = uri.getPath();
        if (path == null) return false;
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".gif");
    }

    private static String normalizeKey(URI uri) {
        return uri.normalize().toString();
    }

    private static PreviewEntry getEntry(String key) {
        synchronized (CACHE_LOCK) {
            return CACHE.get(key);
        }
    }

    private static PreviewEntry startLoading(URI uri) {
        String key = normalizeKey(uri);
        URI downloadUri = toDownloadUri(uri);
        PreviewEntry existing;
        synchronized (CACHE_LOCK) {
            existing = CACHE.get(key);
            if (existing != null && existing.status != Status.FAILED) return existing;
            PreviewEntry entry = PreviewEntry.loading(key, downloadUri);
            CACHE.put(key, entry);
            evictIfNeeded();
            if (!isSafeNetworkUri(downloadUri)) {
                entry.status = Status.FAILED;
                entry.errorMessage = "Blocked URL";
                return entry;
            }
            try {
                entry.loadTask = EXECUTOR.submit(() -> load(entry));
            } catch (RuntimeException e) {
                entry.status = Status.FAILED;
                entry.errorMessage = "Preview queue full";
            }
            return entry;
        }
    }

    private static URI toDownloadUri(URI uri) {
        String host = normalizedHost(uri);
        if (!host.equals("media.discordapp.net")) return uri;
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        String format = null;
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            format = "jpeg";
        } else if (path.endsWith(".png")) {
            format = "png";
        } else if (path.endsWith(".gif")) {
            format = "gif";
        }
        if (format == null || uri.getRawQuery() == null || !uri.getRawQuery().contains("format=webp")) return uri;

        String[] parts = uri.getRawQuery().split("&", -1);
        StringBuilder query = new StringBuilder();
        for (String part : parts) {
            if (!query.isEmpty()) query.append('&');
            if (part.equals("format=webp")) {
                query.append("format=").append(format);
            } else {
                query.append(part);
            }
        }
        try {
            return new URI(uri.getScheme(), uri.getRawAuthority(), uri.getRawPath(), query.toString(), uri.getRawFragment());
        } catch (Exception ignored) {
            return uri;
        }
    }

    private static void load(PreviewEntry entry) {
        boolean decodeSlotAcquired = false;
        try {
            ensureActive(entry);
            URI downloadUri = resolveDownloadUri(entry.uri);
            ensureActive(entry);
            DownloadedContent downloaded = download(downloadUri, DownloadType.IMAGE);
            ensureActive(entry);
            DECODE_SLOTS.acquire();
            decodeSlotAcquired = true;
            DecodedMedia decoded = decodeMedia(downloaded.bytes, downloaded.contentType);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                closeImages(decoded.frames);
                markFailed(entry, "Preview unavailable");
                return;
            }
            client.execute(() -> {
                try {
                    registerDecoded(entry, decoded);
                } finally {
                    DECODE_SLOTS.release();
                }
            });
            decodeSlotAcquired = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markFailed(entry, "Preview cancelled");
        } catch (Exception e) {
            markFailed(entry, readableError(e));
        } finally {
            if (decodeSlotAcquired) DECODE_SLOTS.release();
        }
    }

    private static void ensureActive(PreviewEntry entry) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        synchronized (CACHE_LOCK) {
            if (CACHE.get(entry.key) != entry) throw new InterruptedException();
        }
    }

    private static URI resolveDownloadUri(URI uri) throws Exception {
        if (!isTenorPageUri(uri)) return uri;
        String html = new String(download(uri, DownloadType.HTML).bytes, StandardCharsets.UTF_8);
        Matcher metaMatcher = TENOR_IMAGE_META_PATTERN.matcher(html);
        while (metaMatcher.find()) {
            Matcher contentMatcher = CONTENT_ATTRIBUTE_PATTERN.matcher(metaMatcher.group());
            if (!contentMatcher.find()) continue;
            URI mediaUri = URI.create(contentMatcher.group(1).replace("&amp;", "&")).normalize();
            if (isSafeNetworkUri(mediaUri)
                    && isTenorMediaHost(normalizedHost(mediaUri))
                    && hasSupportedMediaExtension(mediaUri)) {
                return mediaUri;
            }
        }
        throw new IOException("Tenor preview unavailable");
    }

    private static DownloadedContent download(URI uri, DownloadType downloadType) throws Exception {
        URI current = uri;
        String originalHost = normalizedHost(uri);
        for (int redirect = 0; redirect < 3; redirect++) {
            if (!isSafeNetworkUri(current)) throw new IOException("Blocked URL");
            validateResolvedAddress(current);
            HttpURLConnection connection = (HttpURLConnection) current.toURL().openConnection();
            try {
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(REQUEST_TIMEOUT_SECONDS * 1000);
                connection.setReadTimeout(REQUEST_TIMEOUT_SECONDS * 1000);
                connection.setRequestProperty("User-Agent", "WynnExtras media preview");
                int status = connection.getResponseCode();
                if (status >= 300 && status < 400) {
                    closeResponseBody(connection);
                    String location = connection.getHeaderField("location");
                    if (location == null) throw new IOException("Bad redirect");
                    URI next = withoutFragment(current.resolve(location).normalize());
                    if (!isSafeNetworkUri(next)) throw new IOException("Blocked redirect");
                    if (!isSameAllowedHostFamily(originalHost, normalizedHost(next))) throw new IOException("Blocked redirect");
                    current = next;
                    continue;
                }
                if (status < 200 || status >= 300) {
                    closeResponseBody(connection);
                    if (status == 404 && isDiscordHost(normalizedHost(current))) {
                        throw new IOException("Discord attachment unavailable");
                    }
                    throw new IOException("Preview unavailable");
                }
                String contentType = normalizedContentType(connection.getHeaderField("content-type"));
                if (!downloadType.accepts(contentType)) {
                    closeResponseBody(connection);
                    throw new IOException("Unexpected content type");
                }
                long maxBytes = downloadType == DownloadType.HTML
                        ? MAX_HTML_DOWNLOAD_BYTES
                        : Math.max(1, WynnExtrasConfig.INSTANCE.chatMediaPreviewMaxDownloadMb) * 1024L * 1024L;
                String lengthHeader = connection.getHeaderField("content-length");
                if (lengthHeader != null) {
                    try {
                        if (Long.parseLong(lengthHeader) > maxBytes) {
                            closeResponseBody(connection);
                            throw new IOException("Preview too large");
                        }
                    } catch (NumberFormatException ignored) { }
                }
                return new DownloadedContent(readLimited(connection.getInputStream(), maxBytes), contentType);
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Too many redirects");
    }

    private static String normalizedContentType(String contentType) {
        if (contentType == null) return "";
        int separator = contentType.indexOf(';');
        String type = separator < 0 ? contentType : contentType.substring(0, separator);
        return type.trim().toLowerCase(Locale.ROOT);
    }

    private static void closeResponseBody(HttpURLConnection connection) {
        try (InputStream stream = connection.getErrorStream()) {
            if (stream != null) return;
        } catch (IOException ignored) { }
        try (InputStream stream = connection.getInputStream()) {
        } catch (IOException ignored) { }
    }

    private static boolean isSameAllowedHostFamily(String originalHost, String nextHost) {
        if (!isAllowedHost(nextHost)) return false;
        boolean originalDiscord = isDiscordHost(originalHost);
        boolean nextDiscord = isDiscordHost(nextHost);
        boolean originalImgur = originalHost.endsWith("imgur.com");
        boolean nextImgur = nextHost.endsWith("imgur.com");
        boolean originalTenor = isTenorHost(originalHost);
        boolean nextTenor = isTenorHost(nextHost);
        return originalHost.equals(nextHost) || originalDiscord && nextDiscord || originalImgur && nextImgur || originalTenor && nextTenor;
    }

    private static boolean isDiscordHost(String host) {
        return host.endsWith("discordapp.com")
                || host.endsWith("discordapp.net")
                || host.endsWith("discord.com");
    }

    private static void validateResolvedAddress(URI uri) throws Exception {
        String host = normalizedHost(uri);
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (isBlockedAddress(address)) {
                throw new IOException("Blocked host");
            }
        }
    }

    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            return (bytes[0] & 0xFE) == 0xFC;
        }
        if (bytes.length != 4) return true;
        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        int third = bytes[2] & 0xFF;
        return first == 0
                || first == 100 && second >= 64 && second <= 127
                || first == 192 && second == 0 && (third == 0 || third == 2)
                || first == 198 && (second == 18 || second == 19 || second == 51 && third == 100)
                || first == 203 && second == 0 && third == 113
                || first >= 240;
    }

    private static byte[] readLimited(InputStream stream, long maxBytes) throws IOException {
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) throw new IOException("Preview too large");
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static DecodedMedia decodeMedia(byte[] bytes, String contentType) throws IOException {
        MediaFormat format = detectMediaFormat(bytes);
        if (!format.acceptsContentType(contentType)) throw new IOException("Content type mismatch");
        return format == MediaFormat.GIF ? decodeGif(bytes) : decodeStatic(bytes, format);
    }

    private static MediaFormat detectMediaFormat(byte[] bytes) throws IOException {
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 'P'
                && bytes[2] == 'N'
                && bytes[3] == 'G'
                && bytes[4] == '\r'
                && bytes[5] == '\n'
                && bytes[6] == 0x1A
                && bytes[7] == '\n') {
            return MediaFormat.PNG;
        }
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xFF
                && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF) {
            return MediaFormat.JPEG;
        }
        if (bytes.length >= 6
                && bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == '8'
                && (bytes[4] == '7' || bytes[4] == '9')
                && bytes[5] == 'a') {
            return MediaFormat.GIF;
        }
        throw new IOException("Unsupported image format");
    }

    private static DecodedMedia decodeStatic(byte[] bytes, MediaFormat format) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw new IOException("Invalid image");
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format.imageIoName);
            if (!readers.hasNext()) throw new IOException("Unsupported image format");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                validateDimensions(reader.getWidth(0), reader.getHeight(0));
                BufferedImage buffered = reader.read(0);
                if (buffered == null) throw new IOException("Invalid image");
                validateDimensions(buffered.getWidth(), buffered.getHeight());
                NativeImage image = toNativeImage(buffered);
                return new DecodedMedia(List.of(new DecodedFrame(image, 1000)), false, decodedBytes(image.getWidth(), image.getHeight(), 1));
            } finally {
                reader.dispose();
            }
        }
    }

    private static DecodedMedia decodeGif(byte[] bytes) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw new IOException("Invalid GIF");
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) throw new IOException("No GIF reader");
            ImageReader reader = readers.next();
            reader.setInput(stream, false);
            List<DecodedFrame> frames = new ArrayList<>();
            try {
                int frameCount = reader.getNumImages(true);
                int maxFrames = Math.max(1, WynnExtrasConfig.INSTANCE.chatMediaPreviewMaxGifFrames);
                if (frameCount <= 0) throw new IOException("Empty GIF");
                if (frameCount > maxFrames) throw new IOException("GIF has too many frames");

                GifScreen screen = readGifScreen(reader);
                validateDimensions(screen.width, screen.height);
                long decodedBytes = decodedBytes(screen.width, screen.height, frameCount);
                if (decodedBytes > MAX_GIF_DECODED_BYTES) {
                    throw new IOException("GIF too large");
                }

                BufferedImage canvas = new BufferedImage(screen.width, screen.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = canvas.createGraphics();
                try {
                    for (int i = 0; i < frameCount; i++) {
                        ensureNotInterrupted();
                        IIOMetadata metadata = reader.getImageMetadata(i);
                        GifFrameMeta meta = readGifFrameMeta(metadata);
                        validateGifFrame(meta, screen);
                        BufferedImage previous = null;
                        if ("restoreToPrevious".equals(meta.disposalMethod)) {
                            previous = copyImage(canvas);
                        }
                        BufferedImage frame = reader.read(i);
                        validateDimensions(frame.getWidth(), frame.getHeight());
                        if (frame.getWidth() > screen.width || frame.getHeight() > screen.height) {
                            throw new IOException("GIF frame too large");
                        }
                        graphics.drawImage(frame, meta.left, meta.top, null);
                        frames.add(new DecodedFrame(toNativeImage(canvas), meta.delayMs));
                        if ("restoreToBackgroundColor".equals(meta.disposalMethod)) {
                            clearTransparent(graphics, meta.left, meta.top, meta.width, meta.height);
                        } else if (previous != null) {
                            graphics.dispose();
                            canvas = previous;
                            graphics = canvas.createGraphics();
                        }
                    }
                } finally {
                    graphics.dispose();
                }
                if (frames.isEmpty()) throw new IOException("Empty GIF");
                return new DecodedMedia(frames, frames.size() > 1, decodedBytes);
            } catch (IOException | RuntimeException e) {
                closeImages(frames);
                throw e;
            } finally {
                reader.dispose();
            }
        }
    }

    private static void validateGifFrame(GifFrameMeta frame, GifScreen screen) throws IOException {
        if (frame.left < 0
                || frame.top < 0
                || frame.width <= 0
                || frame.height <= 0
                || frame.left + (long) frame.width > screen.width
                || frame.top + (long) frame.height > screen.height) {
            throw new IOException("GIF frame out of bounds");
        }
    }

    private static long decodedBytes(int width, int height, int frames) throws IOException {
        try {
            return Math.multiplyExact(Math.multiplyExact((long) width, height), Math.multiplyExact((long) frames, 4L));
        } catch (ArithmeticException e) {
            throw new IOException("Preview dimensions too large");
        }
    }

    private static void ensureNotInterrupted() throws IOException {
        if (Thread.currentThread().isInterrupted()) throw new IOException("Preview cancelled");
    }

    private static GifScreen readGifScreen(ImageReader reader) throws IOException {
        try {
            IIOMetadata metadata = reader.getStreamMetadata();
            if (metadata == null) throw new IOException("Missing GIF metadata");
            org.w3c.dom.Node root = metadata.getAsTree("javax_imageio_gif_stream_1.0");
            org.w3c.dom.Node descriptor = findNode(root, "LogicalScreenDescriptor");
            if (descriptor == null) throw new IOException("Missing GIF dimensions");
            org.w3c.dom.NamedNodeMap attrs = descriptor.getAttributes();
            int width = parseRequiredIntAttr(attrs, "logicalScreenWidth");
            int height = parseRequiredIntAttr(attrs, "logicalScreenHeight");
            return new GifScreen(width, height);
        } catch (RuntimeException e) {
            throw new IOException("Invalid GIF metadata", e);
        }
    }

    private static GifFrameMeta readGifFrameMeta(IIOMetadata metadata) throws IOException {
        try {
            org.w3c.dom.Node root = metadata.getAsTree("javax_imageio_gif_image_1.0");
            org.w3c.dom.Node descriptor = findNode(root, "ImageDescriptor");
            if (descriptor == null) throw new IOException("Missing GIF frame dimensions");
            org.w3c.dom.Node gce = findNode(root, "GraphicControlExtension");
            org.w3c.dom.NamedNodeMap descAttrs = descriptor.getAttributes();
            org.w3c.dom.NamedNodeMap gceAttrs = gce == null ? null : gce.getAttributes();
            int left = parseRequiredIntAttr(descAttrs, "imageLeftPosition");
            int top = parseRequiredIntAttr(descAttrs, "imageTopPosition");
            int width = parseRequiredIntAttr(descAttrs, "imageWidth");
            int height = parseRequiredIntAttr(descAttrs, "imageHeight");
            int delay = Math.max(20, parseIntAttr(gceAttrs, "delayTime", 10) * 10);
            String disposal = parseStringAttr(gceAttrs, "disposalMethod", "none");
            return new GifFrameMeta(left, top, width, height, delay, disposal);
        } catch (RuntimeException e) {
            throw new IOException("Invalid GIF frame metadata", e);
        }
    }

    private static org.w3c.dom.Node findNode(org.w3c.dom.Node node, String name) {
        if (node == null) return null;
        if (name.equals(node.getNodeName())) return node;
        org.w3c.dom.Node child = node.getFirstChild();
        while (child != null) {
            org.w3c.dom.Node found = findNode(child, name);
            if (found != null) return found;
            child = child.getNextSibling();
        }
        return null;
    }

    private static int parseIntAttr(org.w3c.dom.NamedNodeMap attrs, String name, int fallback) {
        if (attrs == null || attrs.getNamedItem(name) == null) return fallback;
        try {
            return Integer.parseInt(attrs.getNamedItem(name).getNodeValue());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseRequiredIntAttr(org.w3c.dom.NamedNodeMap attrs, String name) throws IOException {
        if (attrs == null || attrs.getNamedItem(name) == null) throw new IOException("Missing GIF metadata");
        try {
            return Integer.parseInt(attrs.getNamedItem(name).getNodeValue());
        } catch (NumberFormatException e) {
            throw new IOException("Invalid GIF metadata", e);
        }
    }

    private static String parseStringAttr(org.w3c.dom.NamedNodeMap attrs, String name, String fallback) {
        if (attrs == null || attrs.getNamedItem(name) == null) return fallback;
        return attrs.getNamedItem(name).getNodeValue();
    }

    private static BufferedImage copyImage(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private static void clearTransparent(Graphics2D graphics, int x, int y, int width, int height) {
        Composite oldComposite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(x, y, width, height);
        graphics.setComposite(oldComposite);
    }

    private static NativeImage toNativeImage(BufferedImage image) throws IOException {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
        try {
            for (int y = 0; y < image.getHeight(); y++) {
                ensureNotInterrupted();
                for (int x = 0; x < image.getWidth(); x++) {
                    nativeImage.setColorArgb(x, y, image.getRGB(x, y));
                }
            }
            return nativeImage;
        } catch (IOException | RuntimeException | Error e) {
            nativeImage.close();
            throw e;
        }
    }

    private static void validateDimensions(int width, int height) throws IOException {
        long pixels = (long) width * height;
        long maxPixels = Math.max(1, WynnExtrasConfig.INSTANCE.chatMediaPreviewMaxPixels);
        if (width <= 0 || height <= 0 || pixels > maxPixels) {
            throw new IOException("Preview dimensions too large");
        }
    }

    private static void registerDecoded(PreviewEntry entry, DecodedMedia decoded) {
        synchronized (CACHE_LOCK) {
            if (CACHE.get(entry.key) != entry) {
                closeImages(decoded.frames);
                return;
            }
        }
        List<PreviewFrame> frames = new ArrayList<>();
        int id = TEXTURE_COUNTER.incrementAndGet();
        int consumedFrames = 0;
        try {
            for (int i = 0; i < decoded.frames.size(); i++) {
                DecodedFrame frame = decoded.frames.get(i);
                Identifier textureId = Identifier.of(WynnExtras.MOD_ID, "dynamic/chat_media/" + id + "/" + i);
                NativeImageBackedTexture texture = new NativeImageBackedTexture(textureId::toString, frame.image);
                try {
                    MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
                } catch (RuntimeException e) {
                    texture.close();
                    consumedFrames++;
                    throw e;
                }
                frames.add(new PreviewFrame(textureId, frame.image.getWidth(), frame.image.getHeight(), frame.delayMs));
                consumedFrames++;
            }
        } catch (RuntimeException e) {
            destroyTextures(frames);
            closeImages(decoded.frames.subList(consumedFrames, decoded.frames.size()));
            markFailed(entry, "Preview unavailable");
            return;
        }
        synchronized (CACHE_LOCK) {
            if (CACHE.get(entry.key) != entry) {
                destroyTextures(frames);
                return;
            }
            entry.frames = frames;
            entry.status = Status.READY;
            entry.animated = decoded.animated;
            entry.decodedBytes = decoded.decodedBytes;
            evictIfNeeded();
        }
    }

    private static void markFailed(PreviewEntry entry, String message) {
        synchronized (CACHE_LOCK) {
            entry.status = Status.FAILED;
            entry.errorMessage = message;
        }
    }

    private static String readableError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) return "Preview unavailable";
        if (message.length() > 48) return message.substring(0, 48) + "...";
        return message;
    }

    private static void drawPreview(DrawContext context, PreviewEntry entry, WynnExtrasConfig.ChatMediaPreviewPosition position, long animationStartedAtMs, String sender) {
        PreviewFrame frame = entry.currentFrame(animationStartedAtMs);
        if (frame == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int maxScreenPercent = Math.clamp(WynnExtrasConfig.INSTANCE.chatMediaPreviewMaxScreenPercent, 10, 50);
        int maxWidth = Math.max(1, screenWidth * maxScreenPercent / 100);
        int maxHeight = Math.max(1, screenHeight * maxScreenPercent / 100);
        double scale = Math.min((double) maxWidth / frame.width, (double) maxHeight / frame.height);
        scale = Math.min(1.0d, scale);
        int drawWidth = Math.max(1, (int) Math.floor(frame.width * scale));
        int drawHeight = Math.max(1, (int) Math.floor(frame.height * scale));
        String senderText = sender == null ? null : "Sent by " + sender;
        int senderHeight = senderText == null ? 0 : 13;
        int contentWidth = senderText == null ? drawWidth : Math.max(drawWidth, mc.textRenderer.getWidth(senderText) + 4);
        int x = getPreviewX(position, screenWidth, contentWidth);
        int y = getPreviewY(position, screenHeight, drawHeight + senderHeight);
        int imageX = x + (contentWidth - drawWidth) / 2;

        if (senderText != null) {
            context.fill(x - 3, y - 3, x + contentWidth + 3, y + senderHeight, 0xE0101010);
            context.drawTextWithShadow(mc.textRenderer, senderText, x + 2, y + 2, 0xFFFFFFFF);
            y += senderHeight;
        }
        context.fill(imageX - 3, y - 3, imageX + drawWidth + 3, y + drawHeight + 3, 0xE0101010);
        context.drawTexture(PREVIEW_PIPELINE, frame.texture, imageX, y, 0, 0, drawWidth, drawHeight, frame.width, frame.height, frame.width, frame.height);
        if (entry.animated) {
            context.fill(imageX - 3, y + drawHeight - 11, imageX + 30, y + drawHeight + 3, 0xA0000000);
            context.drawTextWithShadow(mc.textRenderer, "GIF", imageX + 2, y + drawHeight - 9, 0xFFFFFFFF);
        }
    }

    private static int getPreviewX(WynnExtrasConfig.ChatMediaPreviewPosition position, int screenWidth, int drawWidth) {
        return switch (position) {
            case TOP_LEFT, LEFT, BOTTOM_LEFT -> 4;
            case TOP, CENTER, BOTTOM -> (screenWidth - drawWidth) / 2;
            case TOP_RIGHT, RIGHT, BOTTOM_RIGHT -> screenWidth - drawWidth - 4;
        };
    }

    private static int getPreviewY(WynnExtrasConfig.ChatMediaPreviewPosition position, int screenHeight, int drawHeight) {
        return switch (position) {
            case TOP_LEFT, TOP, TOP_RIGHT -> 4;
            case LEFT, CENTER, RIGHT -> (screenHeight - drawHeight) / 2;
            case BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT -> screenHeight - drawHeight - 4;
        };
    }

    private static void drawStatus(DrawContext context, int mouseX, int mouseY, String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;
        int width = mc.textRenderer.getWidth(text) + 10;
        int height = 18;
        int x = MathHelper.clamp(mouseX + 12, 4, context.getScaledWindowWidth() - width - 4);
        int y = MathHelper.clamp(mouseY - 24, 4, context.getScaledWindowHeight() - height - 4);
        context.fill(x, y, x + width, y + height, 0xE0101010);
        context.drawTextWithShadow(mc.textRenderer, text, x + 5, y + 5, 0xFFFFFFFF);
    }

    private static void evictIfNeeded() {
        while (CACHE.size() > MAX_CACHE_ENTRIES || cachedDecodedBytes() > MAX_CACHED_DECODED_BYTES) {
            Map.Entry<String, PreviewEntry> eldest = CACHE.entrySet().iterator().next();
            CACHE.remove(eldest.getKey());
            destroy(eldest.getValue());
        }
    }

    private static long cachedDecodedBytes() {
        long total = 0;
        for (PreviewEntry entry : CACHE.values()) total += entry.decodedBytes;
        return total;
    }

    private static void destroy(PreviewEntry entry) {
        Future<?> loadTask = entry.loadTask;
        if (loadTask != null && entry.status == Status.LOADING) loadTask.cancel(true);
        if (entry.frames == null || entry.frames.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        Runnable destroyTask = () -> {
            destroyTextures(entry.frames);
        };
        if (client.isOnThread()) {
            destroyTask.run();
        } else {
            client.execute(destroyTask);
        }
    }

    private static void destroyTextures(List<PreviewFrame> frames) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        for (PreviewFrame frame : frames) {
            client.getTextureManager().destroyTexture(frame.texture);
        }
    }

    private static void closeImages(List<DecodedFrame> frames) {
        for (DecodedFrame frame : frames) {
            frame.image.close();
        }
    }

    private enum Status {
        LOADING,
        READY,
        FAILED
    }

    private static final class PreviewEntry {
        private final String key;
        private final URI uri;
        private volatile Status status;
        private volatile String errorMessage;
        private volatile boolean animated;
        private volatile long automaticPreviewStartedAtMs;
        private volatile boolean automaticPreviewConsumed;
        private volatile String automaticPreviewSender;
        private volatile long decodedBytes;
        private volatile Future<?> loadTask;
        private volatile List<PreviewFrame> frames = List.of();

        private PreviewEntry(String key, URI uri) {
            this.key = key;
            this.uri = uri;
            this.status = Status.LOADING;
        }

        private static PreviewEntry loading(String key, URI uri) {
            return new PreviewEntry(key, uri);
        }

        private PreviewFrame currentFrame(long animationStartedAtMs) {
            List<PreviewFrame> localFrames = frames;
            if (localFrames.isEmpty()) return null;
            if (localFrames.size() == 1) return localFrames.get(0);
            long elapsed = System.currentTimeMillis() - animationStartedAtMs;
            long total = totalDurationMs();
            if (total <= 0) return localFrames.get(0);
            long position = elapsed % total;
            long cursor = 0;
            for (PreviewFrame frame : localFrames) {
                cursor += frame.delayMs;
                if (position < cursor) return frame;
            }
            return localFrames.get(localFrames.size() - 1);
        }

        private long totalDurationMs() {
            long total = 0;
            for (PreviewFrame frame : frames) total += frame.delayMs;
            return Math.max(1, total);
        }
    }

    private record PreviewFrame(Identifier texture, int width, int height, int delayMs) {
    }

    private record StyledSegment(int start, int end, Style style) {
    }

    private record DecodedMedia(List<DecodedFrame> frames, boolean animated, long decodedBytes) {
    }

    private record DecodedFrame(NativeImage image, int delayMs) {
    }

    private record DownloadedContent(byte[] bytes, String contentType) {
    }

    private record GifScreen(int width, int height) {
    }

    private record GifFrameMeta(int left, int top, int width, int height, int delayMs, String disposalMethod) {
    }

    private enum DownloadType {
        HTML,
        IMAGE;

        private boolean accepts(String contentType) {
            return switch (this) {
                case HTML -> contentType.equals("text/html") || contentType.equals("application/xhtml+xml");
                case IMAGE -> contentType.equals("image/png")
                        || contentType.equals("image/jpeg")
                        || contentType.equals("image/jpg")
                        || contentType.equals("image/gif");
            };
        }
    }

    private enum MediaFormat {
        PNG("png", Set.of("image/png")),
        JPEG("jpeg", Set.of("image/jpeg", "image/jpg")),
        GIF("gif", Set.of("image/gif"));

        private final String imageIoName;
        private final Set<String> contentTypes;

        MediaFormat(String imageIoName, Set<String> contentTypes) {
            this.imageIoName = imageIoName;
            this.contentTypes = contentTypes;
        }

        private boolean acceptsContentType(String contentType) {
            return contentTypes.contains(contentType);
        }
    }

    private static final class PreviewThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "WynnExtras-MediaPreview-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}