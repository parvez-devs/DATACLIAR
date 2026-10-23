package com.axiom.datacliar;

import java.net.URI;

/** Restricts update metadata to this project's HTTPS distribution channel. */
public final class UpdatePolicy {
    private UpdatePolicy(){}
    public static final String REPO="https://github.com/parvez-devs/DATACLIAR";
    public static final String SIGNER="eb04dedcfe9ba3ab0aa62cf386690f4152b534bc659882cbad4126577f8a4fba";
    public static boolean assetUrl(String raw){
        try{URI uri=new URI(raw);return "https".equals(uri.getScheme())&&"github.com".equals(uri.getHost())&&uri.getPort()==-1&&uri.getUserInfo()==null&&uri.getQuery()==null&&uri.getFragment()==null&&uri.getPath().startsWith("/parvez-devs/DATACLIAR/releases/download/")&&!uri.getPath().contains("..")&&!raw.contains("%");}catch(Exception e){return false;}
    }
    public static boolean hash(String value){return value!=null&&value.matches("[0-9a-fA-F]{64}");}
    public static void validate(long version,String url,String hash,String signer){
        if(version<=0||!assetUrl(url)||!hash(hash)||!SIGNER.equalsIgnoreCase(signer))throw new IllegalArgumentException("Untrusted or invalid update metadata");
    }
}
