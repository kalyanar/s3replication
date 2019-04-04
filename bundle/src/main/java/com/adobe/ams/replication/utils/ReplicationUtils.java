package com.adobe.ams.replication.utils;

import com.adobe.ams.replication.constants.ReplicationConstants;
import com.amazonaws.util.Md5Utils;
import org.apache.sling.api.resource.ResourceResolver;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ReplicationUtils {
  public static String getPathTobeReplicated(String path,String defaultSuffix,
          ReplicationConstants
          .FILE_TYPE
          file_type, ResourceResolver resourceResolver, boolean enableURLShortening){
    switch (file_type){
      case HTML:
        return getHtmlPath(path,defaultSuffix,resourceResolver,
                enableURLShortening);
      default:
        return path;
    }
  }
  public static String getHtmlPath(String url,String
          defaultSuffix,ResourceResolver resourceResolver,
                             boolean enableURLShortening){
    String path=url;
    if(!path.contains(".")){
      path=path.trim()+"."+defaultSuffix.trim();
    }
    if(enableURLShortening){
      path= resourceResolver.map(path);
    }
    return path;
  }
  public static ReplicationConstants.FILE_TYPE guessFileType(String path){
    if(path.endsWith(".js")){
      return ReplicationConstants.FILE_TYPE.JS;
    }else if(path.endsWith(".css")){
      return ReplicationConstants.FILE_TYPE.CSS;
    }else if(isHtmlPath(path)){
      return ReplicationConstants.FILE_TYPE.HTML;
    }else {
      return ReplicationConstants.FILE_TYPE.ASSETS;
    }

  }
  public static boolean isHtmlPath(String path){
    if(!path.startsWith("/content/dam")&&path.startsWith("/content")&&(path
            .endsWith("html")||path.indexOf(".")==-1)){
      return true;
    }
    return false;
  }

  public static String getMD5Checksum(InputStream is) throws IOException {
    String result = "";
    byte[] b=createChecksum(is);

    for (int i=0; i < b.length; i++) {
      result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
    }
    return result;
    // return Md5Utils.md5AsBase64(is);
  }
  public static String getMD5Checksum(byte[] bytes) throws IOException {

    return Md5Utils.md5AsBase64(bytes);
  }
  public static  byte[] createChecksum(InputStream is) throws IOException {
    InputStream fis =  is;

    byte[] buffer = new byte[1024];
    MessageDigest complete = null;
    try {
      complete = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {

    }
    int numRead;

    do {
      numRead = fis.read(buffer);
      if (numRead > 0) {
        complete.update(buffer, 0, numRead);
      }
    } while (numRead != -1);

    fis.close();
    return complete.digest();
  }
}
