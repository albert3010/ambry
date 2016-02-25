package com.github.ambry.network;

import com.codahale.metrics.MetricRegistry;
import com.github.ambry.utils.MockTime;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A class that mocks the {@link Selector} and simply queues connection requests and send requests within itself and
 * returns them in the next calls to {@link #connected()} and {@link #completedSends()} calls.
 */
class MockSelector extends Selector {
  int index;
  private Set<String> connectionIds = new HashSet<String>();
  private List<String> connected = new ArrayList<String>();
  private List<String> disconnected = new ArrayList<String>();
  private List<NetworkSend> sends = new ArrayList<NetworkSend>();
  private List<NetworkReceive> receives = new ArrayList<NetworkReceive>();

  /**
   * Create a MockSelector
   * @throws IOException
   */
  public MockSelector()
      throws IOException {
    super(new NetworkMetrics(new MetricRegistry()), new MockTime(), null);
  }

  /**
   * Mocks the connect by simply keeping track of the connection requests to a (host, port)
   * @param address The address to connect to
   * @param sendBufferSize not used.
   * @param receiveBufferSize not used.
   * @param portType {@PortType} which represents the type of connection to establish
   * @return
   * @throws IOException
   */
  @Override
  public String connect(InetSocketAddress address, int sendBufferSize, int receiveBufferSize, PortType portType)
      throws IOException {
    String hostPortString = address.getHostString() + address.getPort() + index++;
    connected.add(hostPortString);
    connectionIds.add(hostPortString);
    return hostPortString;
  }

  /**
   * Mocks sending and poll. Creates a response for every send to be returned after the next poll.
   * @param timeoutMs Ignored.
   * @param sends The list of new sends to begin
   *
   */
  @Override
  public void poll(long timeoutMs, List<NetworkSend> sends) {
    this.sends = sends;
    if (sends != null) {
      for (NetworkSend send : sends) {
        receives.add(new MockReceive(send.getConnectionId()));
      }
    }
  }

  /**
   * Returns a list of connection ids created between the last two poll() calls (or since the instantiation
   * if only one poll() was done).
   * @return a list of connection ids.
   */
  @Override
  public List<String> connected() {
    List<String> toReturn = connected;
    connected = new ArrayList<String>();
    return toReturn;
  }

  /**
   * Returns a list of connection ids destroyed between the last two poll() calls.
   * @return a list of connection ids.
   */
  @Override
  public List<String> disconnected() {
    List<String> toReturn = disconnected;
    disconnected = new ArrayList<String>();
    return toReturn;
  }

  /**
   * Returns a list of {@link NetworkSend} sent as part of the last poll.
   * @return a lit of {@link NetworkSend} initiated previously.
   */
  @Override
  public List<NetworkSend> completedSends() {
    List<NetworkSend> toReturn = sends;
    sends = new ArrayList<NetworkSend>();
    return toReturn;
  }

  /**
   * Returns a list of {@link NetworkReceive} constructed in the last poll to simulate a response for every send.
   * @return a list of {@line NetworkReceive} for every initiated send.
   */
  @Override
  public List<NetworkReceive> completedReceives() {
    List<NetworkReceive> toReturn = receives;
    receives = new ArrayList<NetworkReceive>();
    return toReturn;
  }

  /**
   * Close the given connection.
   * @param conn connection id to close.
   */
  @Override
  public void close(String conn) {
    if (connectionIds.contains(conn)) {
      disconnected.add(conn);
    }
  }

  /**
   * Close the selector.
   */
  @Override
  public void close() {

  }
}

/**
 * A dummy implementation of the {@link Send} interface.
 */
class MockSend implements Send {
  private ByteBuffer buf;
  private int size;

  /**
   * Construct a MockSend
   */
  public MockSend() {
    buf = ByteBuffer.allocate(16);
    size = 16;
  }

  /**
   * Write the contents of the buffer to the channel.
   * @param channel The channel into which data needs to be written to
   * @return the number of bytes written.
   * @throws IOException if the write encounters an exception.
   */
  @Override
  public long writeTo(WritableByteChannel channel)
      throws IOException {
    long written = channel.write(buf);
    return written;
  }

  /**
   * Returns if all data has been written out.
   * @return true if all data has been written out, false otherwise.
   */
  @Override
  public boolean isSendComplete() {
    return buf.remaining() == 0;
  }

  /**
   * The size of the payload in the Send.
   * @return the size of the payload.
   */
  @Override
  public long sizeInBytes() {
    return size;
  }
}

/**
 * Mocks {@link NetworkReceive} by extending it.
 */
class MockReceive extends NetworkReceive {
  String connectionId;

  /**
   * Construct a MockReceive on the given connection id.
   * @param connectionId the connection id on which the receive is mocked.
   */
  public MockReceive(String connectionId) {
    super(connectionId, new BoundedByteBufferReceive(), new MockTime());
    this.connectionId = connectionId;
  }

  /**
   * Return the connection id associated with the MockReceive.
   * @return the connection id of the MockReceive.
   */
  @Override
  public String getConnectionId() {
    return connectionId;
  }
}

