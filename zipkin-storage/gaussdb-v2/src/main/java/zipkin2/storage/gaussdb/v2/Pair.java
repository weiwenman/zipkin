package zipkin2.storage.gaussdb.v2;

import java.util.Objects;

record Pair(String left, String right) {

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Pair that)) return false;
    return Objects.equals(left, that.left) && Objects.equals(right, that.right);
  }

}
