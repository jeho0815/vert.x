/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.core.datagram.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.impl.ContextImpl;

import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.core.net.impl.SocketAddressImpl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
public class DatagramSocketImpl extends ConnectionBase implements DatagramSocket {

  private Handler<io.vertx.core.datagram.DatagramPacket> packetHandler;

  public DatagramSocketImpl(VertxInternal vertx,
                            DatagramSocketOptions options) {
    super(vertx, createChannel(options.isIpV6() ? io.vertx.core.datagram.impl.InternetProtocolFamily.IPv6 : io.vertx.core.datagram.impl.InternetProtocolFamily.IPv4,
          DatagramSocketOptions.copiedOptions(options)), vertx.getOrCreateContext());
    ContextImpl creatingContext = vertx.getContext();
    if (creatingContext != null && creatingContext.isMultithreaded()) {
      throw new IllegalStateException("Cannot use DatagramSocket in a multi-threaded worker verticle");
    }
    channel().config().setOption(ChannelOption.DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION, true);
    context.getEventLoop().register(channel);
    channel.pipeline().addLast("handler", new DatagramServerHandler(this.vertx, this));
    channel().config().setMaxMessagesPerRead(1);
  }

  @Override
  public DatagramSocket listenMulticastGroup(String multicastAddress, Handler<AsyncResult<DatagramSocket>> handler) {
    try {
      addListener(channel().joinGroup(InetAddress.getByName(multicastAddress)), handler);
    } catch (UnknownHostException e) {
      notifyException(handler, e);
    }
    return this;
  }

  @Override
  public DatagramSocket listenMulticastGroup(String multicastAddress, String networkInterface, String source, Handler<AsyncResult<DatagramSocket>> handler) {
    try {
      InetAddress sourceAddress;
      if (source == null) {
        sourceAddress = null;
      } else {
        sourceAddress = InetAddress.getByName(source);
      }
      addListener(channel().joinGroup(InetAddress.getByName(multicastAddress),
              NetworkInterface.getByName(networkInterface), sourceAddress), handler);
    } catch (Exception e) {
      notifyException(handler, e);
    }
    return this;
  }

  @Override
  public DatagramSocket unlistenMulticastGroup(String multicastAddress, Handler<AsyncResult<DatagramSocket>> handler) {
    try {
      addListener(channel().leaveGroup(InetAddress.getByName(multicastAddress)), handler);
    } catch (UnknownHostException e) {
      notifyException(handler, e);
    }
    return this;
  }

  @Override
  public DatagramSocket unlistenMulticastGroup(String multicastAddress, String networkInterface, String source, Handler<AsyncResult<DatagramSocket>> handler) {
    try {
      InetAddress sourceAddress;
      if (source == null) {
        sourceAddress = null;
      } else {
        sourceAddress = InetAddress.getByName(source);
      }
      addListener(channel().leaveGroup(InetAddress.getByName(multicastAddress),
              NetworkInterface.getByName(networkInterface), sourceAddress), handler);
    } catch (Exception e) {
      notifyException(handler, e);
    }
    return this;
  }

  @Override
  public DatagramSocket blockMulticastGroup(String multicastAddress, String networkInterface, String sourceToBlock, Handler<AsyncResult<DatagramSocket>> handler) {
    try {
      InetAddress sourceAddress;
      if (sourceToBlock == null) {
        sourceAddress = null;
      } else {
        sourceAddress = InetAddress.getByName(sourceToBlock);
      }
      addListener(channel().block(InetAddress.getByName(multicastAddress),
              NetworkInterface.getByName(networkInterface), sourceAddress), handler);
    } catch (Exception e) {
      notifyException(handler, e);
    }
    return  this;
  }

  @Override
  public DatagramSocket blockMulticastGroup(String multicastAddress, String sourceToBlock, Handler<AsyncResult<DatagramSocket>> handler) {
    try {
      addListener(channel().block(InetAddress.getByName(multicastAddress), InetAddress.getByName(sourceToBlock)), handler);
    } catch (UnknownHostException e) {
      notifyException(handler, e);
    }
    return this;
  }

  @Override
  public DatagramSocket listen(int port, String address, Handler<AsyncResult<DatagramSocket>> handler) {
    return listen(new SocketAddressImpl(port, address), handler);
  }

  @Override
  public DatagramSocket packetHandler(Handler<io.vertx.core.datagram.DatagramPacket> handler) {
    this.packetHandler = handler;
    return this;
  }

  @Override
  public DatagramSocket exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  private DatagramSocket listen(SocketAddress local, Handler<AsyncResult<DatagramSocket>> handler) {
    InetSocketAddress is = new InetSocketAddress(local.hostAddress(), local.hostPort());
    ChannelFuture future = channel().bind(is);
    addListener(future, handler);
    return this;
  }

  @SuppressWarnings("unchecked")
  final void addListener(ChannelFuture future, Handler<AsyncResult<DatagramSocket>> handler) {
    if (handler != null) {
      future.addListener(new DatagramChannelFutureListener<>(this, handler, vertx, context));
    }
  }

  @SuppressWarnings("unchecked")
  public DatagramSocket pause() {
    doPause();
    return this;
  }

  @SuppressWarnings("unchecked")
  public DatagramSocket resume() {
    doResume();
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public DatagramSocket send(Buffer packet, int port, String host, Handler<AsyncResult<DatagramSocket>> handler) {
    ChannelFuture future = channel().writeAndFlush(new DatagramPacket(packet.getByteBuf(), new InetSocketAddress(host, port)));
    addListener(future, handler);
    return this;
  }

  @Override
  public DatagramSocket send(String str, int port, String host, Handler<AsyncResult<DatagramSocket>> handler) {
    return send(Buffer.buffer(str), port, host, handler);
  }

  @Override
  public DatagramSocket send(String str, String enc, int port, String host, Handler<AsyncResult<DatagramSocket>> handler) {
    return send(Buffer.buffer(str, enc), port, host, handler);
  }

  @Override
  public void close(final Handler<AsyncResult<Void>> handler) {
    // make sure everything is flushed out on close
    endReadAndFlush();
    ChannelFuture future = channel.close();
    if (handler != null) {
      future.addListener(new DatagramChannelFutureListener<>(null, handler, vertx, context));
    }
  }

  protected DatagramChannel channel() {
    return (DatagramChannel) channel;
  }

  private static NioDatagramChannel createChannel(io.vertx.core.datagram.impl.InternetProtocolFamily family,
                                                  DatagramSocketOptions options) {
    NioDatagramChannel channel;
    if (family == null) {
      channel = new NioDatagramChannel();
    } else {
      switch (family) {
        case IPv4:
          channel = new NioDatagramChannel(InternetProtocolFamily.IPv4);
          break;
        case IPv6:
          channel = new NioDatagramChannel(InternetProtocolFamily.IPv6);
          break;
        default:
          channel = new NioDatagramChannel();
      }
    }
    if (options.getSendBufferSize() != -1) {
      channel.config().setSendBufferSize(options.getSendBufferSize());
    }
    if (options.getReceiveBufferSize() != -1) {
      channel.config().setReceiveBufferSize(options.getReceiveBufferSize());
    }
    channel.config().setReuseAddress(options.isReuseAddress());
    if (options.getTrafficClass() != -1) {
      channel.config().setTrafficClass(options.getTrafficClass());
    }
    channel.config().setBroadcast(options.isBroadcast());
    channel.config().setLoopbackModeDisabled(options.isLoopbackModeDisabled());
    if (options.getMulticastTimeToLive() != -1) {
      channel.config().setTimeToLive(options.getMulticastTimeToLive());
    }
    if (options.getMulticastNetworkInterface() != null) {
      try {
        channel.config().setNetworkInterface(NetworkInterface.getByName(options.getMulticastNetworkInterface()));
      } catch (SocketException e) {
        throw new IllegalArgumentException("Could not find network interface with name " + options.getMulticastNetworkInterface());
      }
    }
    return channel;
  }


  private void notifyException(final Handler<AsyncResult<DatagramSocket>> handler, final Throwable cause) {
    context.execute(() -> handler.handle(Future.completedFuture(cause)), true);
  }

  @Override
  protected void finalize() throws Throwable {
    // Make sure this gets cleaned up if there are no more references to it
    // so as not to leave connections and resources dangling until the system is shutdown
    // which could make the JVM run out of file handles.
    close();
    super.finalize();
  }

  protected void handleClosed() {
    checkContext();
    super.handleClosed();
  }

  void handlePacket(io.vertx.core.datagram.DatagramPacket packet) {
    if (packetHandler != null) {
      packetHandler.handle(packet);
    }
  }

  @Override
  protected void handleInterestedOpsChanged() {
  }
}