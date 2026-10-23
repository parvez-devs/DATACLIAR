package com.axiom.datacliar;
import org.junit.Test;
import java.nio.file.*;
import static org.junit.Assert.*;
public class CacheCleanerTest {
    @Test public void deletesNestedCacheButKeepsRootAndSiblingData() throws Exception {
        Path base=Files.createTempDirectory("datacliar-cache-test");
        try{
            Path root=Files.createDirectories(base.resolve("cache/nested"));
            Files.write(root.resolve("temporary"),new byte[]{1});
            Path data=Files.write(base.resolve("preferences"),new byte[]{2});
            assertEquals(0,CacheCleaner.clear(base.resolve("cache").toFile(),null));
            assertTrue(Files.isDirectory(base.resolve("cache")));assertFalse(Files.exists(root));assertTrue(Files.exists(data));
        }finally{try(java.util.stream.Stream<Path> paths=Files.walk(base)){paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception e){throw new RuntimeException(e);}});}}
    }
    @Test public void refusesLinksOutsideCache() throws Exception {
        Path base=Files.createTempDirectory("datacliar-link-test");
        try{
            Path cache=Files.createDirectory(base.resolve("cache"));Path data=Files.write(base.resolve("private-data"),new byte[]{3});
            Files.createSymbolicLink(cache.resolve("link"),data);
            assertEquals(1,CacheCleaner.clear(cache.toFile()));assertTrue(Files.exists(data));
        }finally{try(java.util.stream.Stream<Path> paths=Files.walk(base)){paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception e){throw new RuntimeException(e);}});}}
    }
}
