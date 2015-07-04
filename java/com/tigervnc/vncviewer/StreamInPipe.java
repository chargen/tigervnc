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

import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.WritableByteChannel;
import java.io.InputStream;
import java.io.IOException;

/**
 * Class which encapsulates System.in as a selectable channel.
 * Instantiate this class, call start() on it to run the background
 * draining thread, then call getStdinChannel() to get a SelectableChannel
 * object which can be used with a Selector object.
 *
 * @author Ron Hitchens (ron@ronsoft.com)
 * created: Jan 2003
 */
public class StreamInPipe
{
  protected Pipe pipe;
  protected CopyThread copyThread;

  public StreamInPipe (InputStream in) throws IOException {
    pipe = Pipe.open();

    copyThread = new CopyThread (in, pipe.sink());
  }

  public void start() {
    copyThread.start();
  }

  public Pipe.SourceChannel getSourceChannel() throws IOException {
    return pipe.source();
  }

  protected void finalize() {
    copyThread.shutdown();
  }

  public static class CopyThread extends Thread
  {
    boolean keepRunning = true;
    byte [] bytes = new byte [4096];
    ByteBuffer buffer = ByteBuffer.wrap (bytes);
    InputStream in;
    WritableByteChannel out;

    public CopyThread (InputStream in, WritableByteChannel out) {
      this.in = in;
      this.out = out;
      this.setDaemon (true);
    }

    public void shutdown() {
      keepRunning = false;
      this.interrupt();
    }

    public void run() {
      try {
        while (keepRunning) {
          int count = in.read (bytes);

          if (count < 0) {
            break;
          }

          buffer.clear().limit (count);

          out.write (buffer);
        }

        System.out.println("CLOSING OUT");

        out.close();
      } catch (IOException e) {
          e.printStackTrace();
      }
    }
  }
}