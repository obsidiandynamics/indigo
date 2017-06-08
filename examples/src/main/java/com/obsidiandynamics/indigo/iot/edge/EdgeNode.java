package com.obsidiandynamics.indigo.iot.edge;

import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

import org.slf4j.*;

import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public final class EdgeNode implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteNode.class);
  
  private final EdgeNexus localNexus = new EdgeNexus(this, LocalPeer.instance());
  
  private final WSServer<?> server;
  
  private final Wire wire;
  
  private final TopicBridge bridge;
  
  private final List<EdgeNexus> nexuses = new CopyOnWriteArrayList<>();
  
  private final List<TopicListener> topicListeners = new ArrayList<>();

  public <E extends WSEndpoint> EdgeNode(WSServerFactory<E> serverFactory, 
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
            case BIND:
              if (frame instanceof BindFrame) {
                final BindFrame bind = (BindFrame) frame;
                handleBind(nexus, bind);
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
        final EdgeNexus nexus = endpoint.getContext();
        try {
          final BinaryEncodedFrame frame = wire.decode(message);
          if (frame.getType() == FrameType.PUBLISH) {
            final PublishBinaryFrame pub = (PublishBinaryFrame) frame;
            handlePublish(nexus, pub);
          } else {
            LOG.error("Unsupported frame {}", frame);
          }
        } catch (Throwable e) {
          LOG.error(String.format("Error processing frame\n%s", BinaryUtils.dump(message)), e);
          return;
        }
      }

      @Override public void onDisconnect(E endpoint, int statusCode, String reason) {
        final EdgeNexus nexus = endpoint.getContext();
        handleDisconnect(nexus);
      }

      @Override public void onClose(E endpoint) {
        final EdgeNexus nexus = endpoint.getContext();
        nexuses.remove(nexus);
      }

      @Override public void onError(E endpoint, Throwable cause) {
        LOG.warn(String.format("Unexpected error on endpoint %s", endpoint), cause);
      }
    });
  }
  
  private void handleBind(EdgeNexus nexus, BindFrame bind) {
    if (bind.getSessionId() != null) {
      final Session session = nexus.getSession();
      if (session == null) {
        LOG.error("No session set for {}", nexus);
        return;
      }
      
      if (session.getSessionId() == null) {
        session.setSessionId(bind.getSessionId());
      } else if (! session.getSessionId().equals(bind.getSessionId())) {
        LOG.warn("Connection {} has attempted to change its session ID from {} to {}", 
                 nexus, session.getSessionId(), bind.getSessionId());
        nexus.send(new BindResponseFrame(bind.getMessageId(), "Cannot reassign session ID"));
        return;
      }
    }
    
    final CompletableFuture<BindResponseFrame> f = bridge.onBind(nexus, bind);
    f.whenComplete((bindRes, cause) -> {
      if (cause == null) {
        nexus.send(bindRes);
        fireBindEvent(nexus, bind, bindRes);
      } else {
        LOG.warn("Error handling bind {}", bind);
        LOG.warn("", cause);
      }
    });
  }
  
  private void handlePublish(EdgeNexus nexus, PublishTextFrame pub) {
    bridge.onPublish(nexus, pub);
    firePublishEvent(nexus, pub);
  }
  
  private void handlePublish(EdgeNexus nexus, PublishBinaryFrame pub) {
    bridge.onPublish(nexus, pub);
    firePublishEvent(nexus, pub);
  }
  
  private void handleConnect(WSEndpoint endpoint) {
    final EdgeNexus nexus = new EdgeNexus(this, new WSEndpointPeer(endpoint));
    nexuses.add(nexus);
    endpoint.setContext(nexus);
    nexus.setSession(new Session());
    bridge.onConnect(nexus);
    fireConnectEvent(nexus);
  }
  
  private void fireConnectEvent(EdgeNexus nexus) {
    for (TopicListener l : topicListeners) {
      l.onConnect(nexus);
    }
  }
  
  private void fireDisconnectEvent(EdgeNexus nexus) {
    for (TopicListener l : topicListeners) {
      l.onDisconnect(nexus);
    }
  }
  
  private void fireBindEvent(EdgeNexus nexus, BindFrame bind, BindResponseFrame bindRes) {
    for (TopicListener l : topicListeners) {
      l.onBind(nexus, bind, bindRes);
    }
  }
  
  private void firePublishEvent(EdgeNexus nexus, PublishTextFrame pub) {
    for (TopicListener l : topicListeners) {
      l.onPublish(nexus, pub);
    }
  }
  
  private void firePublishEvent(EdgeNexus nexus, PublishBinaryFrame pub) {
    for (TopicListener l : topicListeners) {
      l.onPublish(nexus, pub);
    }
  }
  
  private void handleDisconnect(EdgeNexus nexus) {
    bridge.onDisconnect(nexus);
    fireDisconnectEvent(nexus);
  }
  
  public void addTopicListener(TopicListener l) {
    topicListeners.add(l);
  }
  
  public void removeTopicListener(TopicListener l) {
    topicListeners.remove(l);
  }
  
  /**
   *  Obtains the currently connected non-local nexuses.
   *  
   *  @return List of nexuses.
   */
  public List<EdgeNexus> getNexuses() {
    return Collections.unmodifiableList(nexuses);
  }
  
  public void publish(String topic, String payload) {
    final PublishTextFrame pub = new PublishTextFrame(topic, payload);
    bridge.onPublish(localNexus, pub);
    firePublishEvent(localNexus, pub);
  }
  
  public void publish(String topic, ByteBuffer payload) {
    final PublishBinaryFrame pub = new PublishBinaryFrame(topic, payload);
    bridge.onPublish(localNexus, pub);
    firePublishEvent(localNexus, pub);
  }
  
  Wire getWire() {
    return wire;
  }

  @Override
  public void close() throws Exception {
    server.close();
    bridge.close();
  }
  
  public static final class EdgeNodeBuilder {
    private WSServerFactory<?> serverFactory;
    private WSServerConfig serverConfig = new WSServerConfig();
    private Wire wire = new Wire(false);
    private TopicBridge topicBridge;
    
    private void init() throws Exception {
      if (serverFactory == null) {
        serverFactory = (WSServerFactory<?>) Class.forName("com.obsidiandynamics.indigo.ws.undertow.UndertowServer$Factory").newInstance();
      }
      
      if (topicBridge == null) {
        topicBridge = new RoutingTopicBridge();
      }
    }
    
    public EdgeNodeBuilder withServerFactory(WSServerFactory<?> serverFactory) {
      this.serverFactory = serverFactory;
      return this;
    }
    
    public EdgeNodeBuilder withServerConfig(WSServerConfig serverConfig) {
      this.serverConfig = serverConfig;
      return this;
    }
    
    public EdgeNodeBuilder withWire(Wire wire) {
      this.wire = wire;
      return this;
    }

    public EdgeNodeBuilder withTopicBridge(TopicBridge topicBridge) {
      this.topicBridge = topicBridge;
      return this;
    }

    public EdgeNode build() throws Exception {
      init();
      return new EdgeNode(serverFactory, serverConfig, wire, topicBridge);
    }
  }
  
  public static EdgeNodeBuilder builder() {
    return new EdgeNodeBuilder();
  }
}
