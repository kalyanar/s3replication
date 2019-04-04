package com.adobe.ams.replication.provider;

import com.day.cq.wcm.api.WCMMode;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MockHttpRequest  implements HttpServletRequest {
  public static final String FAKE_URL_BASE = "http://localhost:4502";
  private final String method;
  private final String path;
  private final Map<String, Object> attributes;
  private final Map<String, String[]> parameters;
  private final HttpSession session;

  private static final class MockHttpSession implements HttpSession {
    public long getCreationTime() {
      throw new UnsupportedOperationException();
    }

    public String getId() {
      throw new UnsupportedOperationException();
    }

    public long getLastAccessedTime() {
      throw new UnsupportedOperationException();
    }

    public ServletContext getServletContext() {
      throw new UnsupportedOperationException();
    }

    public void setMaxInactiveInterval(int i) {
      throw new UnsupportedOperationException();
    }

    public int getMaxInactiveInterval() {
      throw new UnsupportedOperationException();
    }

    public HttpSessionContext getSessionContext() {
      throw new UnsupportedOperationException();
    }

    public Object getAttribute(String s) {
      throw new UnsupportedOperationException();
    }

    public Object getValue(String s) {
      throw new UnsupportedOperationException();
    }

    public Enumeration getAttributeNames() {
      throw new UnsupportedOperationException();
    }

    public String[] getValueNames() {
      throw new UnsupportedOperationException();
    }

    public void setAttribute(String s, Object o) {
      throw new UnsupportedOperationException();
    }

    public void putValue(String s, Object o) {
      throw new UnsupportedOperationException();
    }

    public void removeAttribute(String s) {
      throw new UnsupportedOperationException();
    }

    public void removeValue(String s) {
      throw new UnsupportedOperationException();
    }

    public void invalidate() {
      throw new UnsupportedOperationException();
    }

    public boolean isNew() {
      throw new UnsupportedOperationException();
    }
  }

  public MockHttpRequest(String method, String path) {
    this.method = method;
    this.path = path;
    this.attributes = new HashMap<String, Object>();
    attributes.put(WCMMode.class.getName(),WCMMode.DISABLED);
    this.parameters = new HashMap<String, String[]>();
    this.session = new MockHttpSession();
  }

  public MockHttpRequest(String method, String path, Map<String, Object> params) {
    this.method = method;
    this.path = path;
    this.attributes = new HashMap<String, Object>();
    this.parameters = new HashMap<String, String[]>();
    this.session = new MockHttpSession();
    attributes.put(WCMMode.class.getName(),WCMMode.DISABLED);
    for(String key : params.keySet()) {
      Object value = params.get(key);

      // internally, Sling seems to expect all parameter values to be String[]
      if(params.get(key) instanceof String[]) {
        this.parameters.put(key, (String[])value);
      } else {
        this.parameters.put(key, new String[]{ value.toString() });
      }
    }
  }

  public String getAuthType() {
    return null;
  }

  public String getContextPath() {
    return "";
  }

  public Cookie[] getCookies() {
    Cookie[] cookies=new Cookie[1];
    Cookie cookie=new Cookie("wcmmode","preview");
    cookies[0]=cookie;
    return cookies;
  }

  public long getDateHeader(String name) {
    return -1;
  }

  public String getHeader(String name) {
    return null;
  }

  @SuppressWarnings("unchecked")
  public Enumeration getHeaderNames() {
    return null;
  }

  @SuppressWarnings("unchecked")
  public Enumeration getHeaders(String name) {
    return null;
  }

  public int getIntHeader(String name) {
    return -1;
  }

  public String getMethod() {
    return method;
  }

  public String getPathInfo() {
    return null;
  }

  public String getPathTranslated() {
    return null;
  }

  public String getQueryString() {
    return null;
  }

  public String getRemoteUser() {
    return null;
  }

  public String getRequestURI() {
    return path;
  }

  public StringBuffer getRequestURL() {
    return new StringBuffer(FAKE_URL_BASE + path);
  }

  public String getRequestedSessionId() {
    return null;
  }

  public String getServletPath() {
    return path;
  }

  public HttpSession getSession() {
    return session;
  }

  public HttpSession getSession(boolean create) {
    return session;
  }

  public Principal getUserPrincipal() {
    return null;
  }

  public boolean isRequestedSessionIdFromCookie() {
    return false;
  }

  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  public boolean isRequestedSessionIdFromUrl() {
    return false;
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    return false;
  }

  @Override
  public void login(String username, String password) throws ServletException {
  }

  @Override
  public void logout() throws ServletException {
  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    return null;
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException {
    return null;
  }

  public boolean isRequestedSessionIdValid() {
    return false;
  }

  public boolean isUserInRole(String role) {
    return false;
  }

  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(attributes.keySet());
  }

  public String getCharacterEncoding() {
    return "utf-8";
  }

  public int getContentLength() {
    return 0;
  }

  public String getContentType() {
    return null;
  }

  public ServletInputStream getInputStream() throws IOException {
    return null;
  }

  public String getLocalAddr() {
    return null;
  }

  public String getLocalName() {
    return null;
  }

  public int getLocalPort() {
    return 0;
  }

  @Override
  public ServletContext getServletContext() {
    return null;
  }

  @Override
  public AsyncContext startAsync() throws IllegalStateException {
    return null;
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, 
                                 ServletResponse servletResponse) throws 
          IllegalStateException {
    return null;
  }

  @Override
  public boolean isAsyncStarted() {
    return false;
  }

  @Override
  public boolean isAsyncSupported() {
    return false;
  }

  @Override
  public AsyncContext getAsyncContext() {
    return null;
  }

  @Override
  public DispatcherType getDispatcherType() {
    return null;
  }

  public Locale getLocale() {
    return Locale.getDefault();
  }

  @SuppressWarnings("unchecked")
  public Enumeration getLocales() {
    return Collections.enumeration(Collections.singleton(Locale.getDefault()));
  }

  public String getParameter(String name) {
    try {
      final Object value = parameters.get(name);

      if(value instanceof String[]) {
        return ((String[])value)[0];
      }

      return (String)value;
    } catch(ClassCastException e) {
      return null;
    }
  }

  public Map<String, String[]> getParameterMap() {
    return parameters;
  }

  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(parameters.keySet());
  }

  public String[] getParameterValues(String name) {
    throw new UnsupportedOperationException();
  }

  public String getProtocol() {
    return "HTTP/1.1";
  }

  public BufferedReader getReader() throws IOException {
    return null;
  }

  public String getRealPath(String path) {
    return null;
  }

  public String getRemoteAddr() {
    return null;
  }

  public String getRemoteHost() {
    return null;
  }

  public int getRemotePort() {
    return 0;
  }

  public RequestDispatcher getRequestDispatcher(String path) {
    return null;
  }

  public String getScheme() {
    return "http";
  }

  public String getServerName() {
    return null;
  }

  public int getServerPort() {
    return 0;
  }

  public boolean isSecure() {
    return false;
  }

  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  public void setAttribute(String name, Object o) {
    attributes.put(name, o);
  }

  public void setCharacterEncoding(String env)
          throws UnsupportedEncodingException {
  }
}
