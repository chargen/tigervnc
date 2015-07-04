/*
 * Copyright (c) 2015 Toni Spets <toni.spets@iki.fi>
 * 
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.tigervnc.vncviewer;

import com.tigervnc.network.Socket;
import com.tigervnc.rdr.FdInStream;
import com.tigervnc.rdr.FdOutStream;
import java.io.IOException;

/**
 *
 * @author Toni Spets <toni.spets@iki.fi>
 */
public class PipeSocket extends Socket {

  protected StreamDescriptor sd;

  public static PipeSocket create(String... command) throws IOException {
    ProcessBuilder pb = new ProcessBuilder(command);
    Process p = pb.start();
    StreamDescriptor sd = new StreamDescriptor(p.getInputStream(), p.getOutputStream());
    FdInStream inStream = new FdInStream(sd);
    FdOutStream outStream = new FdOutStream(sd);
    outStream.setBlocking(false);
    return new PipeSocket(inStream, outStream);
  }

  protected PipeSocket(FdInStream in, FdOutStream out) {
    super(in, out, true);
  }

  @Override
  public int getMyPort() {
    return 0;
  }

  @Override
  public String getPeerAddress() {
    return "pipe";
  }

  @Override
  public String getPeerName() {
    return "pipe";
  }

  @Override
  public int getPeerPort() {
    return 0;
  }

  @Override
  public String getPeerEndpoint() {
    return "pipe";
  }

  @Override
  public boolean sameMachine() {
    return false;
  }
  
}
