package com.adobe.ams.replication.provider.impl;

import com.adobe.ams.replication.provider.EmbeddedReferenceProvider;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Component
public class HTMLEmbeddedReferenceProvider implements EmbeddedReferenceProvider {
  @Override
  public Set<String> provideReferences(String path, byte[] response) {
    String html=new String(response);
    Parser parser = Parser.htmlParser();
    parser.settings(new ParseSettings(true, true));
    Document doc = Jsoup.parse(html,"",parser);
    Set<String> set=findReferences(doc,"script[src]",".js");
    set.addAll(findReferences(doc,"link[href]",".css"));
    set.addAll(findReferences(doc,"img[src]",".jpg","/content/dam"));
    set.addAll(findReferences(doc,"img[src]",".jpeg","/content/dam"));
    set.addAll(findReferences(doc,"img[src]",".png","/content/dam"));
    set.addAll(findReferencesInDiv(doc,"div[style]","/content/dam"));
    return set;
  }

  @Override
  public String getProviderName() {
    return "html";
  }

  private Set<String> findReferences(Document document, String filter,String
          extension){
    Elements elements = document.select(filter);
    Set<String> filesSet=new HashSet<>();
    for(Element element:elements) {
      Attributes attributes = element.attributes();
      for (Attribute attribute : attributes) {
        String path=attribute.getValue();
        if(!path.startsWith("http")&&!path.startsWith("//")&&path.contains
                (extension)){
          filesSet.add( attribute.getValue());
        }
      }
    }
    return filesSet;
  }
  private Set<String> findReferencesInDiv(Document document, String filter,String notStartsWith){
    Pattern p = Pattern.compile("url[ ]*[(]{1}('|\")*[^\\s]+[)]");
    Elements elements = document.select(filter);
    Set<String> filesSet=new HashSet<>();
    for(Element element:elements) {
      Attributes attributes = element.attributes();
      for (Attribute attribute : attributes) {
        String path=attribute.getValue();
        if(!path.startsWith("http")&&!path.startsWith("//")){
          if(!path.startsWith(notStartsWith)){
            Matcher m = p.matcher(path);
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
                    Path base = Paths.get(path);
                    url = base.getParent().resolve(url).normalize().toString();
                  }
                  filesSet.add(url);
                }
              }
            }
          }
        }

      }
    }
    return filesSet;
  }
  private Set<String> findReferences(Document document, String filter,String
          extension,String notStartsWith){
    Elements elements = document.select(filter);
    Set<String> filesSet=new HashSet<>();
    for(Element element:elements) {
      Attributes attributes = element.attributes();
      for (Attribute attribute : attributes) {
        String path=attribute.getValue();
        if(path!=null&&!path.startsWith("http")&&!path.startsWith("//")&&path
                .contains
                        (extension)){
          if(!attribute.getValue().startsWith(notStartsWith)){
            filesSet.add( attribute.getValue());
          }
        }

      }
    }
    return filesSet;
  }
}
