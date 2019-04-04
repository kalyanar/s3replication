package com.adobe.ams.replication.provider;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MockServletOutputStream extends ServletOutputStream {
  private final OutputStream os;
  public MockServletOutputStream(OutputStream os){
    this.os = os;
  }

  @Override
  public String toString() {
    return os.toString();
  }

  @Override
  public void write(int b) throws IOException {
    os.write(b);
  }
}
