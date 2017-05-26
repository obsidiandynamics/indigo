package com.obsidiandynamics.indigo.iot.frame;

import com.google.gson.*;
import com.google.gson.typeadapters.*;

public final class Wire {
  private final Gson gson;
  
  public Wire(boolean prettyPrinting) {
    final GsonBuilder builder = new GsonBuilder()
        .registerTypeAdapterFactory(RuntimeTypeAdapterFactory
                                    .of(Frame.class, "type")
                                    .registerSubtype(SubscribeFrame.class, "Subscribe")
                                    .registerSubtype(SubscribeResponseFrame.class, "SubscribeResponse"));
    if (prettyPrinting) builder.setPrettyPrinting();
    gson = builder.create();
  }

  public String encode(Frame frame) {
    final StringBuilder sb = new StringBuilder();
    sb.append(frame.getType().getCharCode()).append(' ');
    encodeFrameBody(frame, sb);
    return sb.toString();
  }
  
  private void encodeFrameBody(Frame frame, StringBuilder sb) {
    switch (frame.getType()) {
      case SUBSCRIBE:
        sb.append(gson.toJson(frame, Frame.class));
        return;
        
      case RECEIVE:
        sb.append(((TextFrame) frame).getPayload());
        return;
        
      case PUBLISH:
        final PublishFrame pub = (PublishFrame) frame;
        sb.append(pub.getTopic()).append(' ').append(pub.getPayload());
        return;
        
      default:
        return;
    }
  }
  
  public Frame decode(String str) {
    final FrameType type = FrameType.fromCharCode(str.charAt(0));
    return decodeFrameBody(type, str);
  }
  
  private Frame decodeFrameBody(FrameType type, String str) {
    if (str.length() <= 2) return throwError(type, str);
    switch (type) {
      case SUBSCRIBE:
        return gson.fromJson(str.substring(2), Frame.class);
        
      case RECEIVE:
        return new TextFrame(str.substring(2));
        
      case PUBLISH:
        final int splitIdx = str.indexOf(' ', 2);
        if (splitIdx == -1) return throwError(type, str);
        final String topic = str.substring(2, splitIdx);
        final String payload = str.substring(splitIdx + 1);
        return new PublishFrame(topic, payload);
      
      default:
        throw new IllegalArgumentException("Unsupported frame content '" + str + "'");
    }
  }
  
  private static Frame throwError(FrameType type, String str) {
    throw new IllegalArgumentException("Invalid '" + type.getCharCode() + "' frame with content '" + str + "'");
  }
}
