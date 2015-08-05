package uk.addie.xyzzy.error;

public class XyzzyException extends RuntimeException {
  XyzzyException(final String string) {
    super(string);
  }

  private static final long serialVersionUID = 1L;
}
