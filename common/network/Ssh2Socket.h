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

// -=- TcpSocket.h - class for SSH2 exec sockets.

#ifndef __NETWORK_SSH2_SOCKET_H__
#define __NETWORK_SSH2_SOCKET_H__

#include <network/TcpSocket.h>
#include <libssh2.h>

namespace network {
  class Ssh2Socket : public network::TcpSocket {
    public:
      Ssh2Socket(const char *name, int port);
      virtual const char *LastError();
      virtual ~Ssh2Socket();
    private:
      LIBSSH2_SESSION *session;
      LIBSSH2_CHANNEL *channel;
  };
}

#endif
