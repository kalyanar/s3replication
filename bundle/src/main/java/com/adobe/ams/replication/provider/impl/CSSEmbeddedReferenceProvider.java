package com.adobe.ams.replication.provider.impl;

import com.adobe.ams.replication.provider.EmbeddedReferenceProvider;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleSheet;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
@Component
public class CSSEmbeddedReferenceProvider implements EmbeddedReferenceProvider {
  @Override
  public Set<String> provideReferences(String path, byte[] response) {
    return referencesInCSS(path,response);
  }

  @Override
  public String getProviderName() {
    return "css";
  }
  private Set<String> referencesInCSS(String cssfileName,byte[] content){
    Pattern p = Pattern.compile("url[ ]*[(]{1}('|\")*[^\\s]+[)]");
    String css=new String(content);
    Set<String> urls=new HashSet<>();
    InputSource source = new InputSource(new StringReader(css));
    CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
    try {

      CSSStyleSheet sheet = parser.parseStyleSheet(source, null, null);
      CSSRuleList rules = sheet.getCssRules();
      for (int i = 0; i < rules.getLength(); i++) {
        final CSSRule rule = rules.item(i);

        String cssText=rule.getCssText();
        Matcher m = p.matcher(cssText);
        while(m.find()){

          for(int j=0;j<m.groupCount();j++) {
            String url = m.group(j);
            if (url != null) {
              url = url.replace("url", "");
              url = url.replace("(", "");
              url = url.replace("'", "");
              url = url.replace("\"", "");
              url = url.replace(")", "");
              if (!url.startsWith("/") && !url.startsWith("http")) {
                Path base = Paths.get(cssfileName);
                url = base.getParent().resolve(url).normalize().toString();
              }
              if(url.length()<100){
                urls.add(url);
              }

            }
          }
        }
      }

    } catch (IOException e) {

    }
    return urls;
  }
}
