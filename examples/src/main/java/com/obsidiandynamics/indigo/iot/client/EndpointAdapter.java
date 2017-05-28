package com.obsidiandynamics.indigo.iot.client;

import java.nio.*;
import java.util.concurrent.*;

import org.slf4j.*;

import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

final class EndpointAdapter<E extends WSEndpoint> implements EndpointListener<E> {
  private static final Logger LOG = LoggerFactory.getLogger(EndpointAdapter.class);
  
  private final SessionManager manager;
  private final Session session;
  private final SessionHandler handler;
  
  EndpointAdapter(SessionManager manager, Session session, SessionHandler handler) {
    this.manager = manager;
    this.session = session;
    this.handler = handler;
  }
  
  @Override 
  public void onConnect(E endpoint) {
    session.setEndpoint(endpoint);
    handler.onConnect(session);
  }

  @Override 
  public void onText(E endpoint, String message) {
    try {
      final TextEncodedFrame frame = manager.getWire().decode(message);
      switch (frame.getType()) {
        case SUBSCRIBE:
          if (frame instanceof SubscribeResponseFrame) {
            final SubscribeResponseFrame subRes = (SubscribeResponseFrame) frame;
            final CompletableFuture<SubscribeResponseFrame> f = session.removeSubscribeRequest(subRes.getId());
            if (f != null) {
              f.complete(subRes);
            } else {
              LOG.debug("Ignoring {}", subRes);
            }
          } else {
            LOG.error("Unsupported frame {}", frame);
          }
          break;
          
        case RECEIVE:
          final TextFrame text = (TextFrame) frame;
          handler.onText(session, text.getPayload());
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

  @Override 
  public void onBinary(E endpoint, ByteBuffer message) {
    try {
      final BinaryEncodedFrame frame = manager.getWire().decode(message);
      if (frame.getType() == FrameType.RECEIVE) {
        handler.onBinary(session, ((BinaryFrame) frame).getPayload());
      } else {
        LOG.error("Unsupported frame {}", frame);
      }
    } catch (Throwable e) {
      LOG.error(String.format("Error processing frame\n%s", BinaryUtils.dump(message)), e);
      return;
    }
  }

  @Override 
  public void onClose(E endpoint, int statusCode, String reason) {
    handler.onDisconnect(session);
  }

  @Override 
  public void onError(E endpoint, Throwable cause) {
    LOG.warn("Unexpected error", cause);
  }
}
