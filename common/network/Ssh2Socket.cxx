/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * 
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

#include <network/Ssh2Socket.h>
#include <rfb/util.h>
#include <rfb/LogWriter.h>
#include <rfb/Configuration.h>
#include <rfb/Security.h>

#include <rdr/FdInStream.h>
#include <rdr/FdOutStream.h>

using namespace network;
using namespace rfb;

static rfb::LogWriter vlog("Ssh2Socket");

namespace network {
  class Ssh2InStream : public rdr::FdInStream
  {
    public:
      Ssh2InStream(int fd, LIBSSH2_CHANNEL *channel) : FdInStream(fd) {
        this->channel = channel;
      }

    private:
      LIBSSH2_CHANNEL *channel;

      int readWithTimeoutOrCallback(void* buf, int len, bool wait)  {
        if (wait) {
          int p = 0;
          while (p < len) {
            int ret = libssh2_channel_read(channel, (char *)buf + p, len - p);
            if (ret == LIBSSH2_ERROR_EAGAIN) {
              if (p > 0)
                return p;
              continue;
            }

            p += ret;
          }
          return p;
        } else {
          int ret = libssh2_channel_read(channel, (char *)buf, len);
          if (ret == LIBSSH2_ERROR_EAGAIN)
            return 0;

          return ret;
        }
      }
  };

  class Ssh2OutStream : public rdr::FdOutStream
  {
    public:
      Ssh2OutStream(int fd, LIBSSH2_CHANNEL *channel) : FdOutStream(fd) {
        this->channel = channel;
      }

    private:
      LIBSSH2_CHANNEL *channel;

      int writeWithTimeout(const void* data, int length, int timeoutms) {
        int p = 0;

        while (p < length) {
          int ret = libssh2_channel_write(channel, (char *)data + p, length - p);
          if (ret == LIBSSH2_ERROR_EAGAIN)
            return 0;

          p += ret;
        }

        return p;
      }
  };
}

Ssh2Socket::Ssh2Socket(const char *name, int port) : TcpSocket::TcpSocket(name, port) {
  session = libssh2_session_init();
  if (!session)
    throw SocketException ("libssh2 session init failed", 0);

  if (libssh2_session_handshake(session, getFd()) < 0)
    throw SocketException ("libssh2 handshake failed with remote host", 0);

  CharArray username;
  CharArray password;

  (CSecurity::upg)->getUserPasswd(&username.buf, &password.buf);

  if (libssh2_userauth_password(session, username.buf, password.buf) < 0) {
    while (true) {
      (CSecurity::upg)->getUserPasswd(0, &password.buf);
      if (libssh2_userauth_password(session, username.buf, password.buf) == 0)
        break;
    }
  }

  vlog.info("SSH login successful as %s", username.buf);

  channel = libssh2_channel_open_session(session);
  if (!channel)
    throw SocketException (LastError(), 0);

  if (libssh2_channel_exec(channel, "./vncpipe.pl") < 0)
    throw SocketException (LastError(), 0);

  vlog.info("SSH exec started, switching to non-blocking");

  libssh2_session_set_blocking(session, 0);

  instream = new Ssh2InStream(getFd(), channel);
  outstream = new Ssh2OutStream(getFd(), channel);
}

const char *Ssh2Socket::LastError()
{
  if (!session)
    return NULL;

  char *ret = NULL;
  libssh2_session_last_error(session, &ret, NULL, 0);

  return ret;
}

Ssh2Socket::~Ssh2Socket() {
  if (channel) {
    libssh2_channel_free(channel);
  }

  if (session) {
    libssh2_session_disconnect(session, "Connection closed.");
    libssh2_session_free(session);
  }
}
