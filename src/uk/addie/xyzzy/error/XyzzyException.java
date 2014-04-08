
package uk.addie.xyzzy.error;

public class XyzzyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public XyzzyException(String string) {
        super(string);
    }
}
