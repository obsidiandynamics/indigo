package com.obsidiandynamics.indigo.iot.frame;

import java.nio.*;
import java.nio.charset.*;

import com.google.gson.*;
import com.google.gson.typeadapters.*;
import com.obsidiandynamics.indigo.util.*;

public final class Wire {
  private static final Charset UTF8 = Charset.forName("UTF-8");
  
  private static final int MAX_UNSIGNED_SHORT = (1 << 16) - 1;
  
  private final Gson gson;
  
  public Wire(boolean prettyPrinting) {
    final GsonBuilder builder = new GsonBuilder()
        .registerTypeAdapterFactory(RuntimeTypeAdapterFactory
                                    .of(IdFrame.class, "type")
                                    .registerSubtype(SubscribeFrame.class, "Subscribe")
                                    .registerSubtype(SubscribeResponseFrame.class, "SubscribeResponse"));
    if (prettyPrinting) builder.setPrettyPrinting();
    gson = builder.create();
  }

  public String encode(TextEncodedFrame frame) {
    final StringBuilder sb = new StringBuilder();
    sb.append(frame.getType().getCharCode()).append(' ');
    encodeFrameBody(frame, sb);
    return sb.toString();
  }
  
  private void encodeFrameBody(Frame frame, StringBuilder sb) {
    switch (frame.getType()) {
      case SUBSCRIBE:
        sb.append(gson.toJson(frame, IdFrame.class));
        return;
        
      case RECEIVE:
        sb.append(((TextFrame) frame).getPayload());
        return;
        
      case PUBLISH:
        final PublishTextFrame pub = (PublishTextFrame) frame;
        sb.append(pub.getTopic()).append(' ').append(pub.getPayload());
        return;
        
      default:
        throw new IllegalArgumentException("Unsupported frame " + frame);
    }
  }

  public ByteBuffer encode(BinaryEncodedFrame frame) {
    final FrameType type = frame.getType();
    switch (type) {
      case RECEIVE: {
        final BinaryFrame bin = (BinaryFrame) frame;
        final ByteBuffer payload = bin.getPayload();
        final int payloadRemaining = payload.remaining();
        final ByteBuffer buf = ByteBuffer.allocate(1 + payloadRemaining);
        buf.put(type.getByteCode());
        final int payloadPos = payload.position();
        buf.put(payload);
        payload.position(payloadPos);
        buf.flip();
        return verifiedBuffer(buf);
      }
        
      case PUBLISH: {
        final PublishBinaryFrame pub = (PublishBinaryFrame) frame;
        final byte[] topicBytes = pub.getTopic().getBytes(UTF8);
        if (topicBytes.length > MAX_UNSIGNED_SHORT) {
          throw new IllegalArgumentException("Topic length cannot exceed " + MAX_UNSIGNED_SHORT + " bytes");
        }
        final ByteBuffer payload = pub.getPayload();
        final int payloadRemaining = payload.remaining();
        final ByteBuffer buf = ByteBuffer.allocate(3 + topicBytes.length + payloadRemaining);
        buf.put(type.getByteCode());
        buf.putShort((short) topicBytes.length);
        buf.put(topicBytes);
        final int payloadPos = payload.position();
        buf.put(payload);
        payload.position(payloadPos);
        buf.flip();
        return verifiedBuffer(buf);
      }
      
      default:
        throw new IllegalArgumentException("Unsupported frame " + frame);
    }
  }
  
  private static ByteBuffer verifiedBuffer(ByteBuffer buf) {
    if (buf.remaining() > MAX_UNSIGNED_SHORT) {
      throw new IllegalArgumentException("Frame length cannot exceed " + MAX_UNSIGNED_SHORT + " bytes");
    }
    return buf;
  }
  
  public TextEncodedFrame decode(String str) {
    final FrameType type = FrameType.fromCharCode(str.charAt(0));
    return decodeFrameBody(type, str);
  }
  
  private TextEncodedFrame decodeFrameBody(FrameType type, String str) {
    if (str.length() <= 2) return throwError(type, str);
    switch (type) {
      case SUBSCRIBE:
        return (TextEncodedFrame) gson.fromJson(str.substring(2), IdFrame.class);
        
      case RECEIVE:
        return new TextFrame(str.substring(2));
        
      case PUBLISH:
        final int splitIdx = str.indexOf(' ', 2);
        if (splitIdx == -1) return throwError(type, str);
        final String topic = str.substring(2, splitIdx);
        final String payload = str.substring(splitIdx + 1);
        return new PublishTextFrame(topic, payload);
      
      default:
        throw new IllegalArgumentException("Unsupported frame content '" + str + "'");
    }
  }
  
  private static TextEncodedFrame throwError(FrameType type, String str) {
    throw new IllegalArgumentException("Invalid '" + type.getCharCode() + "' frame with content '" + str + "'");
  }
  
  public BinaryEncodedFrame decode(ByteBuffer buf) {
    final int pos = buf.position();
    final byte byteCode = buf.get();
    final FrameType type = FrameType.fromByteCode(byteCode);
    switch (type) {
      case RECEIVE:
        return new BinaryFrame(buf);
        
      case PUBLISH:
        final int topicLength = Short.toUnsignedInt(buf.getShort());
        if (topicLength > MAX_UNSIGNED_SHORT) {
          throw new IllegalArgumentException("Topic length cannot exceed " + MAX_UNSIGNED_SHORT + " bytes");
        }
        final byte[] topicBytes = new byte[topicLength];
        buf.get(topicBytes);
        final String topic = new String(topicBytes, UTF8);
        return new PublishBinaryFrame(topic, buf);
        
      default:
        buf.position(pos);
        final byte[] frameBytes = new byte[buf.remaining()];
        buf.get(frameBytes);
        throw new IllegalArgumentException("Unsupported frame content: " + BinaryUtils.dump(frameBytes));
    }
  }
}