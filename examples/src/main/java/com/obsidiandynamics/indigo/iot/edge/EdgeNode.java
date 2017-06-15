package com.obsidiandynamics.indigo.iot.edge;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.iot.edge.auth.*;
import com.obsidiandynamics.indigo.iot.edge.auth.AuthChain.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.frame.Error;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;
import org.slf4j.*;

import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

public final class EdgeNode implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteNode.class);
  
  private final EdgeNexus localNexus = new EdgeNexus(this, LocalPeer.instance());
  
  private final WSServer<?> server;
  
  private final Wire wire;
  
  private final TopicBridge bridge;
  
  private final AuthChain pubAuthChain;
  
  private final AuthChain subAuthChain;
  
  private final List<EdgeNexus> nexuses = new CopyOnWriteArrayList<>();
  
  private final List<TopicListener> topicListeners = new ArrayList<>();
  
  private boolean loggingEnabled = true;

  public <E extends WSEndpoint> EdgeNode(WSServerFactory<E> serverFactory,
                                         WSServerConfig config,
                                         Wire wire,
                                         TopicBridge bridge,
                                         AuthChain pubAuthChain,
                                         AuthChain subAuthChain) throws Exception {
    pubAuthChain.validate();
    subAuthChain.validate();
    this.wire = wire;
    this.bridge = bridge;
    this.pubAuthChain = pubAuthChain;
    this.subAuthChain = subAuthChain;
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
                if (loggingEnabled) LOG.warn("{}: unsupported frame {}", nexus, frame);
              }
              break;
              
            case PUBLISH:
              final PublishTextFrame pub = (PublishTextFrame) frame;
              handlePublish(nexus, pub);
              break;
              
            default:
              if (loggingEnabled) LOG.warn("{}: unsupported frame {}", nexus, frame);
              return;
          }
        } catch (Throwable e) {
          if (loggingEnabled) LOG.warn(String.format("%s: error processing frame\n%s", nexus,message), e);
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
            if (loggingEnabled) LOG.warn("{}: unsupported frame {}", nexus, frame);
          }
        } catch (Throwable e) {
          if (loggingEnabled) LOG.warn(String.format("%s: error processing frame\n%s", nexus, BinaryUtils.dump(message)), e);
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
        if (loggingEnabled) LOG.warn(String.format("Unexpected error on endpoint %s", endpoint), cause);
      }
    });
  }
  
  private void handleBind(EdgeNexus nexus, BindFrame bind) {
    if (loggingEnabled && LOG.isDebugEnabled()) LOG.debug("{}: bind {}", nexus, bind);

    final String validationError = bind.getValidationError();
    if (validationError != null) {
      if (loggingEnabled) LOG.warn("{}: validation error: {}", nexus, validationError);
      nexus.send(new BindResponseFrame(bind.getMessageId(), new GeneralError(validationError)));
      return;
    }
    
    final Session session = nexus.getSession();
    if (session == null) {
      if (loggingEnabled) LOG.error("{}: no session", nexus);
      return;
    }
    
    if (bind.getAuth() != null) {
      session.setAuth(bind.getAuth());
    }
    
    final String newSessionId;
    if (bind.getSessionId() != null) {
      if (session.getSessionId() == null) {
        newSessionId = bind.getSessionId();
        session.setSessionId(newSessionId);
      } else if (! session.getSessionId().equals(bind.getSessionId())) {
        if (loggingEnabled) LOG.warn("{}: attempted to change its session ID from {} to {}", 
                                     nexus, session.getSessionId(), bind.getSessionId());
        nexus.send(new BindResponseFrame(bind.getMessageId(), new GeneralError("Cannot reassign session ID")));
        return;
      } else {
        newSessionId = null;
      }
    } else {
      newSessionId = null;
    }
    
    final Set<String> toSubscribe = new HashSet<>();
    final Set<String> existing = session.getSubscription().getTopics();
    for (String topic : bind.getSubscribe()) {
      if (! existing.contains(topic)) {
        toSubscribe.add(topic);
      } else {
        if (loggingEnabled && LOG.isDebugEnabled()) LOG.debug("{}: ignoring duplicate subscription to {} for {}", nexus, topic);    
      }
    }
    if (newSessionId != null) {
      toSubscribe.add(Flywheel.getRxTopicPrefix(newSessionId));
      toSubscribe.add(Flywheel.getRxTopicPrefix(newSessionId) + "/#");
    }

    final Set<String> toUnsubscribe = new HashSet<>();
    for (String topic : bind.getUnsubscribe()) {
      if (existing.contains(topic)) {
        toUnsubscribe.add(topic);
      } else {
        if (loggingEnabled && LOG.isDebugEnabled()) LOG.debug("{}: ignoring duplicate unsubscription from {} for {}", nexus, topic);
      }
    }
    
    authenticateSubTopics(nexus, bind.getMessageId(), toSubscribe, () -> {
      final CompletableFuture<Void> f = bridge.onBind(nexus, toSubscribe, toUnsubscribe);
      f.whenComplete((void_, cause) -> {
        if (cause == null) {
          final BindResponseFrame bindRes = new BindResponseFrame(bind.getMessageId());
          nexus.send(bindRes);
          fireBindEvent(nexus, bind, bindRes);
        } else {
          if (loggingEnabled) LOG.warn("{}: error handling bind {}", nexus, bind);
          if (loggingEnabled) LOG.warn("", cause);
          fireBindEvent(nexus, bind, new BindResponseFrame(bind.getMessageId(), new GeneralError("Internal error")));
        }
      });      
    });
  }
  
  private void authenticateSubTopics(EdgeNexus nexus, UUID messageId, Set<String> topics, Runnable onSuccess) {
    final CombinedMatches combined = subAuthChain.get(topics);
    combined.invokeAll(nexus, errors -> {
      if (errors.isEmpty()) {
        onSuccess.run();
      } else {
        if (loggingEnabled) LOG.warn("{}: subscriber authentication failed with errors {}, auth: {}", 
                                     nexus, errors, nexus.getSession().getAuth());
        nexus.send(new BindResponseFrame(messageId, errors));
      }
    });
  }
  
  private void handlePublish(EdgeNexus nexus, PublishTextFrame pub) {
    authenticatePubTopic(nexus, pub.getTopic(), () -> {
      bridge.onPublish(nexus, pub);
      firePublishEvent(nexus, pub);
    });
  }
  
  private void handlePublish(EdgeNexus nexus, PublishBinaryFrame pub) {
    authenticatePubTopic(nexus, pub.getTopic(), () -> {
      bridge.onPublish(nexus, pub);
      firePublishEvent(nexus, pub);
    });
  }
  
  private void authenticatePubTopic(EdgeNexus nexus, String topic, Runnable onSuccess) {
    final CombinedMatches combined = pubAuthChain.get(Collections.singleton(topic));
    combined.invokeAll(nexus, errors -> {
      if (errors.isEmpty()) {
        onSuccess.run();
      } else {
        if (loggingEnabled) LOG.warn("{}: publisher authentication failed with errors {}, auth: {}", 
                                     nexus, errors, nexus.getSession().getAuth());
        sendErrors(nexus, errors);
      }
    });
  }

  private void sendErrors(EdgeNexus nexus, Collection<? extends Error> errors) {
    final String sessionId = nexus.getSession().getSessionId();
    final String errorTopic = Flywheel.getRxTopicPrefix(sessionId != null ? sessionId : "anon") + "/errors";
    nexus.send(new TextFrame(errorTopic, wire.encodeJson(new Errors(errors))));
  }
  
  private void handleConnect(WSEndpoint endpoint) {
    final EdgeNexus nexus = new EdgeNexus(this, new WSEndpointPeer(endpoint));
    nexuses.add(nexus);
    endpoint.setContext(nexus);
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
  
  public boolean isLoggingEnabled() {
    return loggingEnabled;
  }

  public void setLoggingEnabled(boolean loggingEnabled) {
    this.loggingEnabled = loggingEnabled;
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
    private AuthChain pubAuthChain = AuthChain.createPubDefault();
    private AuthChain subAuthChain = AuthChain.createSubDefault();
    
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
    
    public EdgeNodeBuilder withPubAuthChain(AuthChain pubAuthChain) {
      this.pubAuthChain = pubAuthChain;
      return this;
    }
    
    public EdgeNodeBuilder withSubAuthChain(AuthChain subAuthChain) {
      this.subAuthChain = subAuthChain;
      return this;
    }

    public EdgeNode build() throws Exception {
      init();
      return new EdgeNode(serverFactory, serverConfig, wire, topicBridge, pubAuthChain, subAuthChain);
    }
  }
  
  public static EdgeNodeBuilder builder() {
    return new EdgeNodeBuilder();
  }
}
