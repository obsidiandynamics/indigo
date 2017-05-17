package com.obsidiandynamics.indigo.messagebus;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;

public final class GensonCodecTest implements TestSupport {
  private static final class Node {
    final String text;
    final Object[] children;
    
    @SuppressWarnings("unused")
    @Deprecated Node() {
      this(null);
    }
    
    Node(String text) {
      this(text, new Node[0]);
    }
    
    Node(String text, Node[] children) {
      this.text = text;
      this.children = children;
    }

    @Override
    public String toString() {
      return "Node [text=" + text + ", children=" + Arrays.toString(children) + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(children);
      result = prime * result + ((text == null) ? 0 : text.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Node other = (Node) obj;
      if (!Arrays.equals(children, other.children))
        return false;
      if (text == null) {
        if (other.text != null)
          return false;
      } else if (!text.equals(other.text))
        return false;
      return true;
    }
  }
  
  @Test
  public void testObj() {
    final MessageCodec codec = new GensonCodec();
    final Node root = new Node("root", new Node[] {new Node("branch")});
    
    final String encoded = codec.encode(root);
    log("encoded=%s\n", encoded);
    
    final Object r = codec.decode(encoded);
    log("r=%s\n", r);
    
    assertNotNull(r);
    assertEquals(Node.class, r.getClass());
    assertEquals(root, r);
  }
  
  @Test
  public void testNull() {
    final MessageCodec codec = new GensonCodec();
    
    final String encoded = codec.encode(null);
    log("encoded=%s\n", encoded);
    
    final Object r = codec.decode(encoded);
    log("r=%s\n", r);
    
    assertNull(r);
  }
}
