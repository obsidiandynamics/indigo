package com.obsidiandynamics.indigo.iot.remote;

import java.nio.*;
import java.util.concurrent.*;

import org.slf4j.*;

import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

final class EndpointAdapter<E extends WSEndpoint> implements EndpointListener<E> {
  private static final Logger LOG = LoggerFactory.getLogger(EndpointAdapter.class);
  
  private final RemoteNode node;
  private final RemoteNexus nexus;
  private final RemoteNexusHandler handler;
  
  EndpointAdapter(RemoteNode node, RemoteNexus nexus, RemoteNexusHandler handler) {
    this.node = node;
    this.nexus = nexus;
    this.handler = handler;
  }
  
  @Override 
  public void onConnect(E endpoint) {
    nexus.setEndpoint(endpoint);
    handler.onConnect(nexus);
  }

  @Override 
  public void onText(E endpoint, String message) {
    try {
      final TextEncodedFrame frame = node.getWire().decode(message);
      switch (frame.getType()) {
        case SUBSCRIBE:
          if (frame instanceof SubscribeResponseFrame) {
            final SubscribeResponseFrame subRes = (SubscribeResponseFrame) frame;
            final CompletableFuture<SubscribeResponseFrame> f = nexus.removeSubscribeRequest(subRes.getId());
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
          handler.onText(nexus, text.getTopic(), text.getPayload());
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
      final BinaryEncodedFrame frame = node.getWire().decode(message);
      if (frame.getType() == FrameType.RECEIVE) {
        final BinaryFrame bin = (BinaryFrame) frame;
        handler.onBinary(nexus, bin.getTopic(), bin.getPayload());
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
    handler.onDisconnect(nexus);
//    node.removeNexus(nexus);
    //TODO
    UndertowEndpoint ue = (UndertowEndpoint) endpoint;
    ue.getChannel().addCloseTask(c -> {
      node.removeNexus(nexus);
    });
  }

  @Override 
  public void onError(E endpoint, Throwable cause) {
    LOG.warn("Unexpected error", cause);
  }
}
