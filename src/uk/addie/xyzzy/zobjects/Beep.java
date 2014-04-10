
package uk.addie.xyzzy.zobjects;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class Beep {
    // originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
    // and modified by Steve Pomeroy <steve@staticfree.info>
    private final int        sampleRate = 8000;
    private final int        numSamples;
    private final double     sample[];
    private final double     freqOfTone;                      // hz
    private final byte       generatedSnd[];
    public final static Beep beep1      = new Beep(0.1, 1760);
    public final static Beep beep2      = new Beep(0.1, 440);

    public Beep(double duration, int frequency) {
        this.numSamples = (int) (duration * sampleRate);
        sample = new double[numSamples];
        this.freqOfTone = frequency;
        this.generatedSnd = new byte[2 * numSamples];
        genTone();
    }

    void genTone() {
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone));
        }
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    public void playSound() {
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }
}
