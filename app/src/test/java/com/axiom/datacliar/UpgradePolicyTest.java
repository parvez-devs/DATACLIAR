package com.axiom.datacliar;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class UpgradePolicyTest {
    @Test public void updateUrlsStayInsideTheProjectReleaseChannel(){
        assertTrue(UpdatePolicy.assetUrl(UpdatePolicy.REPO+"/releases/download/v1.4.0/app.apk"));
        for(String url:new String[]{"http://github.com/parvez-devs/DATACLIAR/releases/download/v1/app.apk","https://github.com.evil.example/parvez-devs/DATACLIAR/releases/download/v1/app.apk","https://user@github.com/parvez-devs/DATACLIAR/releases/download/v1/app.apk","https://github.com/other/repo/releases/download/v1/app.apk",UpdatePolicy.REPO+"/releases/download/v1/../app.apk",UpdatePolicy.REPO+"/releases/download/v1/%2e%2e/app.apk"})assertFalse(url,UpdatePolicy.assetUrl(url));
    }
    @Test public void updateMetadataRequiresVersionHashAndPinnedSigner(){
        String url=UpdatePolicy.REPO+"/releases/download/v1.4.0/app.apk";
        UpdatePolicy.validate(5,url,UpdatePolicy.SIGNER,UpdatePolicy.SIGNER);
        assertThrows(IllegalArgumentException.class,()->UpdatePolicy.validate(0,url,UpdatePolicy.SIGNER,UpdatePolicy.SIGNER));
        assertThrows(IllegalArgumentException.class,()->UpdatePolicy.validate(5,url,"invalid",UpdatePolicy.SIGNER));
        assertThrows(IllegalArgumentException.class,()->UpdatePolicy.validate(5,url,UpdatePolicy.SIGNER,"0".repeat(64)));
    }
    @Test public void batchesDeduplicateAndNeverAllowBulkDataDeletion(){
        List<BatchTarget> targets=BatchTarget.unique(Arrays.asList(new BatchTarget("com.app","App"),new BatchTarget("com.app","App"),new BatchTarget("com.other","Other")));
        assertEquals(2,targets.size());assertEquals("com.app",targets.get(0).pkg);
        assertTrue(BatchTarget.batchAction("freeze"));assertTrue(BatchTarget.batchAction("unfreeze"));assertTrue(BatchTarget.batchAction("stop"));assertFalse(BatchTarget.batchAction("data"));assertFalse(BatchTarget.batchAction("cache"));
        assertThrows(IllegalArgumentException.class,()->BatchTarget.unique(Collections.nCopies(201,new BatchTarget("com.app","App"))));
    }
}
