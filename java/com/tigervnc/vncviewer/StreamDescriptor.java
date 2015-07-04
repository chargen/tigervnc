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

import com.tigervnc.network.FileDescriptor;
import com.tigervnc.rdr.Exception;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;


/**
 *
 * @author Toni Spets <toni.spets@iki.fi>
 */
public class StreamDescriptor implements FileDescriptor {

  protected Selector selector;
  protected StreamInPipe inPipe;
  protected StreamOutPipe outPipe;
  protected Pipe.SourceChannel inChannel;
  protected Pipe.SinkChannel outChannel;

  public StreamDescriptor(InputStream in, OutputStream out) throws Exception {
    try {
      inPipe = new StreamInPipe(in);
      inChannel = inPipe.getSourceChannel();
      inChannel.configureBlocking(false);
      inPipe.start();

      outPipe = new StreamOutPipe(out);
      outChannel = outPipe.getSinkChannel();
      outChannel.configureBlocking(false);
      outPipe.start();

      selector = Selector.open();
    } catch (IOException e) {
      e.printStackTrace();
      throw new Exception(e.getMessage());
    }
  }

  @Override
  public int read(byte[] buf, int bufPtr, int length) throws Exception {
    try {
      // here's a problem: non-blocking read can return 0 even if select says
      // there's data but there isn't, surprise!
      // it causes an odd issue that the interface should never return 0 unless
      // there's actually no data to read, here's to hoping some data will
      // arrive sooner or later if select said there is

      int ret;

      do {
        ret = inChannel.read(ByteBuffer.wrap(buf, bufPtr, length));
      } while(ret == 0);

      if (ret == -1)
        return 0;

      return ret;
    } catch (IOException e) {
      e.printStackTrace();
      throw new Exception(e.getMessage());
    }
  }

  @Override
  public int write(byte[] buf, int bufPtr, int length) throws Exception {
    try {
      return outChannel.write(ByteBuffer.wrap(buf, bufPtr, length));
    } catch (IOException e) {
      e.printStackTrace();
      throw new Exception(e.getMessage());
    }
  }

  @Override
  public int select(int interestOps, Integer timeout) throws Exception {
    try {
      if ((interestOps & SelectionKey.OP_READ) > 0) {
        inChannel.register(selector, SelectionKey.OP_READ);
      } else {
        inChannel.register(selector, 0);
      }

      if ((interestOps & SelectionKey.OP_WRITE) > 0) {
        outChannel.register(selector, SelectionKey.OP_WRITE);
      } else {
        outChannel.register(selector, 0);
      }

      int readyKeys;
      if (timeout == null) {
        readyKeys = selector.select(0);
      }
      else if (timeout.longValue() == 0)
        readyKeys = selector.selectNow();
      else {
        readyKeys = selector.select(timeout.longValue());
      }

      int readyOps = 0;
      if (readyKeys > 0) {
        for (SelectionKey sk : selector.selectedKeys()) {
          readyOps |= sk.readyOps();
        }
        selector.selectedKeys().clear();
      }

      return readyOps;
    } catch (IOException e) {
      e.printStackTrace();
      throw new Exception(e.getMessage());
    }
  }

  @Override
  public void close() throws IOException {
    inPipe.finalize();
    selector.close();
  }
}
