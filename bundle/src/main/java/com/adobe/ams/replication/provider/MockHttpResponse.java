package com.adobe.ams.replication.provider;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Locale;

public class MockHttpResponse implements HttpServletResponse {
  private int status = 200;
  private String message;
  private String encoding = "UTF-8";
  private String contentType;
  private final MockServletOutputStream outputStream;
  private final PrintWriter writer;

  public MockHttpResponse(OutputStream os) throws UnsupportedEncodingException {
    outputStream = new MockServletOutputStream(os);
    writer = new PrintWriter(new OutputStreamWriter(outputStream, encoding));
  }

  public void addCookie(Cookie cookie) {
  }

  public void addDateHeader(String name, long date) {
  }

  public void addHeader(String name, String value) {
  }

  public void addIntHeader(String name, int value) {
  }

  public boolean containsHeader(String name) {
    return false;
  }

  public String encodeRedirectUrl(String url) {
    return null;
  }

  public String encodeRedirectURL(String url) {
    return null;
  }

  public String encodeUrl(String url) {
    return null;
  }

  public String encodeURL(String url) {
    return null;
  }

  public void sendError(int sc, String msg) throws IOException {
    status = sc;
    message = msg;
  }

  public void sendError(int sc) throws IOException {
    status = sc;
  }

  public void sendRedirect(String location) throws IOException {
  }

  public void setDateHeader(String name, long date) {
  }

  public void setHeader(String name, String value) {
  }

  public void setIntHeader(String name, int value) {
  }

  public void setStatus(int sc, String sm) {
    status = sc;
    message = sm;
  }

  public void setStatus(int sc) {
    status = sc;
  }

  public void flushBuffer() throws IOException {
  }

  public int getBufferSize() {
    return 0;
  }

  public String getCharacterEncoding() {
    return encoding;
  }

  public String getContentType() {
    return contentType;
  }

  public Locale getLocale() {
    return null;
  }

  public ServletOutputStream getOutputStream() throws IOException {
    return outputStream;
  }

  public PrintWriter getWriter() throws IOException {
    return writer;
  }

  public boolean isCommitted() {
    return false;
  }

  public void reset() {
  }

  public void resetBuffer() {
  }

  public void setBufferSize(int size) {
  }

  public void setCharacterEncoding(String charset) {
    encoding = charset;
  }

  public void setContentLength(int len) {
  }

  public void setContentType(String type) {
    contentType = type;
  }

  public void setLocale(Locale loc) {
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public int getStatus() {
    return status;
  }

  @Override
  public String getHeader(String name) {
    return null;
  }

  @Override
  public Collection<String> getHeaders(String name) {
    return null;
  }

  @Override
  public Collection<String> getHeaderNames() {
    return null;
  }

  public String getContent() {
    writer.flush();
    return outputStream.toString();
  }
}
