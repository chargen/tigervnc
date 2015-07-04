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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.tigervnc.network.Socket;
import com.tigervnc.rdr.FdInStream;
import com.tigervnc.rdr.FdOutStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author Toni Spets <toni.spets@iki.fi>
 */
public class SSHExecSocket extends Socket {

  protected StreamDescriptor sd;

  public static SSHExecSocket create(String host, int port, String command) throws IOException {
    try {
      JSch jsch= new JSch();
      String homeDir = new String("");
      try {
        homeDir = System.getProperty("user.home");
      } catch(java.security.AccessControlException e) {
        System.out.println("Cannot access user.home system property");
      }
      // NOTE: jsch does not support all ciphers.  User may be
      //       prompted to accept host key authenticy even if
      //       the key is in the known_hosts file.
      File knownHosts = new File(homeDir+"/.ssh/known_hosts");
      if (knownHosts.exists() && knownHosts.canRead())
	      jsch.setKnownHosts(knownHosts.getAbsolutePath());
      ArrayList<File> privateKeys = new ArrayList<File>();
      privateKeys.add(new File(homeDir+"/.ssh/id_rsa"));
      privateKeys.add(new File(homeDir+"/.ssh/id_dsa"));
      for (Iterator<File> i = privateKeys.iterator(); i.hasNext();) {
        File privateKey = (File)i.next();
        if (privateKey.exists() && privateKey.canRead())
	        jsch.addIdentity(privateKey.getAbsolutePath());
      }
      // username and passphrase will be given via UserInfo interface.
      PasswdDialog dlg = new PasswdDialog(new String("SSH Authentication"), false, false);
      dlg.promptPassword(new String("SSH Authentication"));

      Session session=jsch.getSession(dlg.userEntry.getText(), host, port);
      session.setPassword(new String(dlg.passwdEntry.getPassword()));

      // see previous note
      java.util.Properties config = new java.util.Properties(); 
      config.put("StrictHostKeyChecking", "no");
      session.setConfig(config);

      session.connect();
      ChannelExec channel = (ChannelExec)session.openChannel("exec");
      channel.setCommand(command);
      channel.connect();

      StreamDescriptor sd = new StreamDescriptor(channel.getInputStream(), channel.getOutputStream());
      FdInStream inStream = new FdInStream(sd);
      FdOutStream outStream = new FdOutStream(sd);
      outStream.setBlocking(false);
      return new SSHExecSocket(inStream, outStream);
    } catch (JSchException e) {
      throw new IOException(e);
    }
  }

  protected SSHExecSocket(FdInStream in, FdOutStream out) {
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
