
package uk.addie.xyzzy.error;

public class XyzzyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    XyzzyException(final String string) {
        super(string);
    }
}
