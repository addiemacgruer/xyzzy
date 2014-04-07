
package uk.addie.xyzzy;

import java.io.Serializable;

public class Random implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int               interval         = 0;
    private int               counter          = 0;

    public int random(final short range) {
        if (range <= 0) { /* set random seed */
            seed_random(-range);
            return 0;
        }
        /* generate random number */
        int result;
        if (interval != 0) { /* ...in special mode */
            result = counter++;
            if (counter == interval) {
                counter = 1;
            }
        } else { /* ...in standard mode */
            result = (short) System.currentTimeMillis();
            //            A = 0x015a4e35L * A + 1;
            //            result = (short) (A >> 16 & 0x7fff);
        }
        final int randomResult = (result % range) + 1;
        return randomResult;
    }

    public void seed_random(final int value) {
        if (value == 0) { /* true RN mode */
            interval = 0;
        } else if (value < 1000) { /* special seed value */
            counter = 0;
            interval = value;
        } else { /* standard seed value */
            interval = 0;
        }
    }
}
