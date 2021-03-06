//
//  mpsocket.h
//  CMuscle
//
//  Created by Joris Borgdorff on 04-06-13.
//  Copyright (c) 2013 Joris Borgdorff. All rights reserved.
//

#ifndef __CMuscle__mpsocket__
#define __CMuscle__mpsocket__
#include "DecoupledSelectSocket.h"
#include "muscle2/util/msocket.h"
#include "muscle2/util/thread.h"
#include "muscle2/util/rwmutex.h"
#include "muscle2/util/mutex.h"

#include <string>
#include <unistd.h>
#include <pthread.h>
#include <sys/socket.h>
#include <stack>

namespace muscle {
	using namespace net;
	using namespace util;

    class MPServerSocket;
    class MPClientSocket;
    class mpsocket;
    
    //// Threads for asynchronous connections ////
    
    class mpsocket_thread : public thread
    {
    private:
        const size_t sz;
        const MPClientSocket *sock;
        const bool send;
    public:
        mpsocket_thread(bool send, void *data, size_t sz, const MPClientSocket *sock) : send(send), data((char *)data), sz(sz), sock(sock) { start(); }
        
        virtual void *run();
		virtual void afterRun();
		virtual void deleteResult(void *res) { delete (ssize_t*)res; }
		char * const data;
    };
    
    class mpsocket_connect_thread : public thread
    {
    private:
        const endpoint ep;
        const socket_opts opts;
        const mpsocket *sock;
        const bool asServer;
    public:
        mpsocket_connect_thread(endpoint ep, const socket_opts& opts, const mpsocket *sock, bool asServer) : ep(ep), opts(opts), sock(sock), asServer(asServer) { start(); }
        virtual void *run();
		virtual void deleteResult(void *res);
		virtual void afterRun();
    };
        
    ///// Actual socket implementation ////
    
    class mpsocket : virtual public DecoupledSelectSocket
    {
    public:
        static mutex path_mutex;
		virtual bool selectWriteFdIsReadable() const { return true; }
    protected:
        mpsocket() {}
        virtual ~mpsocket() {}
    }; // end class socket

    class MPClientSocket : public ClientSocket, public mpsocket
    {
    public:
        int pathid;

        MPClientSocket(const ServerSocket& parent, int pathid, const socket_opts& opts);
        MPClientSocket(endpoint& ep, async_service *service, const socket_opts& opts);
        virtual ~MPClientSocket();
        
        // Data Transmission
        virtual ssize_t send (const void* s, size_t size);
        virtual ssize_t recv (void* s, size_t size);
        
        virtual int hasError();
        void setWin(ssize_t size);
		virtual void async_cancel();
	private:
        // Disallowed - is problematic for destructor
        MPClientSocket(const MPClientSocket& other) {}
        
        ssize_t runInThread(bool send, void *s, size_t sz, mpsocket_thread *&last_thread);
        bool isConnecting();

        mpsocket_thread *sendThread, *recvThread;
        mpsocket_connect_thread *connectThread;
        void *last_send, *last_recv;
    };
    
    class MPServerSocket : public ServerSocket, public mpsocket
    {
    public:
        MPServerSocket(endpoint& ep, async_service *service, const socket_opts& opts);
        virtual ~MPServerSocket() { async_cancel(); delete listener; }
		
        virtual ClientSocket *accept(const socket_opts& opts);
        virtual void async_cancel();
	private:
        mpsocket_connect_thread *listener;
		socket_opts server_opts;
    };

    class MPSocketFactory : public SocketFactory
    {
    private:
        static int num_mpsocket_factories;
    public:
        MPSocketFactory(async_service *service);
        virtual ~MPSocketFactory();
        virtual ClientSocket *connect(endpoint& ep, const socket_opts& opts);
        virtual ServerSocket *listen(endpoint& ep, const socket_opts& opts);
    };
} // end namespace muscle

#endif /* defined(__CMuscle__mpsocket__) */
