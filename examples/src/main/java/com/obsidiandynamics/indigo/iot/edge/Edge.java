package com.obsidiandynamics.indigo.iot.edge;

import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

import org.slf4j.*;

import com.obsidiandynamics.indigo.iot.client.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;

public final class Edge implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);
  
  private final WSServer<?> server;
  
  private final Wire wire;
  
  private final TopicBridge bridge;
  
  private final List<EdgeNexus> nexuses = new CopyOnWriteArrayList<>();

  public <E extends WSEndpoint> Edge(WSServerFactory<E> serverFactory, 
                                     WSServerConfig config, 
                                     Wire wire, 
                                     TopicBridge bridge) throws Exception {
    this.wire = wire;
    this.bridge = bridge;
    server = serverFactory.create(config, new EndpointListener<E>() {
      @Override public void onConnect(E endpoint) {
        handleConnect(endpoint);
      }

      @Override public void onText(E endpoint, String message) {
        final EdgeNexus nexus = endpoint.getContext();
        try {
          final Frame frame = wire.decode(message);
          switch (frame.getType()) {
            case SUBSCRIBE:
              if (frame instanceof SubscribeFrame) {
                final SubscribeFrame sub = (SubscribeFrame) frame;
                handleSubscribe(nexus, sub);
              } else {
                LOG.error("Unsupported frame {}", frame);
              }
              break;
              
            case PUBLISH:
              final PublishTextFrame pub = (PublishTextFrame) frame;
              handlePublish(nexus, pub);
              break;
              
            default:
              LOG.error("Unsupported frame {}", frame);
              return;
          }
        } catch (Throwable e) {
          LOG.error(String.format("Error processing frame %s", message), e);
          return;
        }
      }

      @Override public void onBinary(E endpoint, ByteBuffer message) {
        // TODO Auto-generated method stub
        LOG.warn("Unimplemented");
      }

      @Override public void onClose(E endpoint, int statusCode, String reason) {
        final EdgeNexus nexus = endpoint.getContext();
        handleDisconnect(nexus);
      }

      @Override public void onError(E endpoint, Throwable cause) {
        LOG.warn("Unexpected error", cause);
      }
    });
  }
  
  private void handleSubscribe(EdgeNexus nexus, SubscribeFrame sub) {
    final CompletableFuture<SubscribeResponseFrame> f = bridge.onSubscribe(nexus, sub);
    f.whenComplete((subRes, cause) -> {
      if (cause == null) {
        nexus.send(subRes);
      } else {
        LOG.warn("Error handling subscription {}", sub);
        LOG.warn("", cause);
      }
    });
  }
  
  private void handlePublish(EdgeNexus nexus, PublishTextFrame pub) {
    bridge.onPublish(nexus, pub);
  }
  
  private void handleConnect(WSEndpoint endpoint) {
    final EdgeNexus nexus = new EdgeNexus(this, endpoint);
    nexuses.add(nexus);
    endpoint.setContext(nexus);
    bridge.onConnect(nexus);
  }
  
  private void handleDisconnect(EdgeNexus nexus) {
    bridge.onDisconnect(nexus);
    nexuses.remove(nexus);
  }
  
  public List<EdgeNexus> getNexuses() {
    return Collections.unmodifiableList(nexuses);
  }
  
  Wire getWire() {
    return wire;
  }

  @Override
  public void close() throws Exception {
    server.close();
  }
}
