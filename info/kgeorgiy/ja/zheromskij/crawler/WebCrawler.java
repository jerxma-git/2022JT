package info.kgeorgiy.ja.zheromskij.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

/**
 * Web crawler implementation.
 */
public class WebCrawler implements Crawler {
    /**
     * Launch the crawler.
     *
     * @param args command line arguments in form
     *             {@code url [depth [downloads [extractors [perHost]]]]},
     *             where {@code []} denotes optional argument.
     *             <ul>
     *             <li>{@code url} - root URL of the site to crawl</li>
     *             <li>{@code depth} - maximum depth of the site to crawl (default
     *             is {@code 1})</li>
     *             <li>{@code downloads} - maximum number of downloaded files
     *             (default is {@code 1})</li>
     *             <li>{@code extractors} - maximum number of extracted files
     *             (default is {@code 1})</li>
     *             <li>{@code perHost} - maximum number of threads per host (default
     *             is {@code 1})</li>
     *             </ul>
     * @author Zheromskij Maxim
     */
    public static void main(final String[] args) {
        if (args == null || args.length < 1 || args.length > 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Usage: url [depth [downloads [extractors [perHost]]]]");
            System.exit(1);
        }
        final int depth;
        final int downloads;
        final int extractors;
        final int perHost;
        try {
            depth = getArgument(args, 1, 1);
            downloads = getArgument(args, 2, 1);
            extractors = getArgument(args, 3, 1);
            perHost = getArgument(args, 4, 1);
        } catch (final NumberFormatException e) {
            System.err.println("Invalid number: " + e.getMessage());
            return;
        }
        try (Crawler crawler = new WebCrawler(new CachingDownloader(), downloads, extractors, perHost)) {
            final Result result = crawler.download(args[0], depth);
            System.out.println(result.getDownloaded().size() + " links downloaded");
            System.out.println(result.getErrors().size() + " errors occurred");
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Creates a new instance of the crawler.
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        if (downloaders < 1) {
            throw new IllegalArgumentException("Not enough downloaders");
        }
        if (extractors < 1) {
            throw new IllegalArgumentException("Not enough extractors");
        }
        if (perHost < 1) {
            throw new IllegalArgumentException("Not enough perHost");
        }
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    private static int getArgument(final String[] args, int index, int defaultValue) {
        return args.length > index ? Integer.parseInt(args[index]) : defaultValue;
    }

    /**
     * Closes this crawler, relinquishing all of its resources.
     */
    @Override
    public void close() {
        downloaders.shutdownNow();
        extractors.shutdownNow();
    }

    /**
     * Downloads the given URL and all URLs it refers to, up to the given depth.
     *
     * @param url   URL to start downloading from.
     * @param depth maximum depth of the site to download.
     * @return result of the download.
     */
    @Override
    public Result download(String url, int depth) {
        final Set<String> downloadedURLs = ConcurrentHashMap.newKeySet();
        final ConcurrentMap<String, IOException> exceptions = new ConcurrentHashMap<>();
        download(Set.of(url), depth, downloadedURLs, exceptions);
        return new Result(new ArrayList<>(downloadedURLs), exceptions);
    }

    private void download(Set<String> urls, int depth, Set<String> downloadedURLs, ConcurrentMap<String, IOException> exceptions) {
        ConcurrentMap<String, Semaphore> hostLimits = new ConcurrentHashMap<>();
        Set<String> toDownload = ConcurrentHashMap.newKeySet();
        CountDownLatch urlsLeft = new CountDownLatch(urls.size());
        for (String url : urls) {
            downloaders.submit(() -> {
                final String host;
                try {
                    host = URLUtils.getHost(url);
                } catch (final MalformedURLException e) {
                    exceptions.put(url, e);
                    urlsLeft.countDown();
                    return;
                }
                Semaphore hostSemaphore = hostLimits.computeIfAbsent(host, k -> new Semaphore(perHost));
                try {
                    hostSemaphore.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    final Document document = downloader.download(url);
                    downloadedURLs.add(url);
                    if (depth > 1) {
                        extractors.submit(() -> {
                            try {
                                toDownload.addAll(document.extractLinks());
                            } catch (final IOException e) {
                                exceptions.put(url, e);
                            } finally {
                                urlsLeft.countDown();
                            }
                        });
                    } else {
                        urlsLeft.countDown();
                    }
                } catch (final IOException e) {
                    exceptions.put(url, e);
                    urlsLeft.countDown();
                } finally {
                    hostSemaphore.release();
                }
            });
        }
        try {
            urlsLeft.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        toDownload.removeAll(exceptions.keySet());
        toDownload.removeAll(downloadedURLs);
        if (!toDownload.isEmpty()) {
            download(toDownload, depth - 1, downloadedURLs, exceptions);
        }
    }

    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final Downloader downloader;
    private final int perHost;
}