package in.dogue.synth;

/*
 * This is a simple speech synth subroutine, based on formant synthesis theory.
 * Speech is synthesized by passing source excitation signal through set of formant one-pole filters.
 * Excitation signal is a sawtooth or noise (depending on sound type), although you can try other signals.
 * Created by:           Stepanov Andrey, 2008 ( ICQ: 129179794, e-mail: andrewstepanov@mail.ru )
 */
public class TinySynth {
    static final int SAMPLE_FREQUENCY = 44100;
    static final int PLAY_TIME = 32;
    static final double MASTER_VOLUME = 0.0003;
    static final double PI = 3.141592653589793;
    static final double PI_2 = 2*PI;

    private static double sawtooth(double x) {
        return 0.5 - (x - Math.floor(x / PI_2) * PI_2) / PI_2;
    }

    private static Phoneme[] phonemes = new Phoneme[] {
            new Phoneme('o', 12,  15,   0,  10,  10,   0, new Shape(3,  6, false, false)),
            new Phoneme('i',  5,  56,   0,  10,  10,   0, new Shape(3,  3, false, false)),
            new Phoneme('j',  5,  56,   0,  10,  10,   0, new Shape(1,  3, false, false)),
            new Phoneme('u',  5,  14,   0,  10,  10,   0, new Shape(3,  3, false, false)),
            new Phoneme('a', 18,  30,   0,  10,  10,   0, new Shape(3, 15, false, false)),
            new Phoneme('e', 14,  50,   0,  10,  10,   0, new Shape(3, 15, false, false)),
            new Phoneme('E', 20,  40,   0,  10,  10,   0, new Shape(3, 12, false, false)),
            new Phoneme('w',  3,  14,   0,  10,  10,   0, new Shape(3,  1, false, false)),
            new Phoneme('v',  2,  20,   0,  20,  10,   0, new Shape(3,  3, false, false)),
            new Phoneme('T',  2,  20,   0,  40,   1,   0, new Shape(3,  5, false, false)),
            new Phoneme('z',  5,  28,  80,  10,   5,  10, new Shape(3,  3, false, false)),
            new Phoneme('Z',  4,  30,  60,  50,   1,   5, new Shape(3,  5, false, false)),
            new Phoneme('b',  4,   0,   0,  10,   0,   0, new Shape(1,  2, false, false)),
            new Phoneme('d',  4,  40,  80,  10,  10,  10, new Shape(1,  2, false, false)),
            new Phoneme('m',  4,  20,   0,  10,  10,   0, new Shape(3,  2, false, false)),
            new Phoneme('n',  4,  40,   0,  10,  10,   0, new Shape(3,  2, false, false)),
            new Phoneme('r',  3,  10,  20,  30,   8,   1, new Shape(3,  3, false, false)),
            new Phoneme('l',  8,  20,   0,  10,  10,   0, new Shape(3,  5, false, false)),
            new Phoneme('g',  2,  10,  26,  15,   5,   2, new Shape(2,  1, false, false)),
            new Phoneme('f',  8,  20,  34,  10,  10,  10, new Shape(3,  4,  true, false)),
            new Phoneme('h', 22,  26,  32,  30,  10,  30, new Shape(1, 10,  true, false)),
            new Phoneme('s', 80, 110,   0,  80,  40,   0, new Shape(3,  5,  true, false)),
            new Phoneme('S', 20,  30,   0, 100, 100,   0, new Shape(3, 10,  true, false)),
            new Phoneme('p',  4,  10,  20,   5,  10,  10, new Shape(1,  2,  true,  true)),
            new Phoneme('t',  4,  20,  40,  10,  20,   5, new Shape(1,  3,  true,  true)),
            new Phoneme('k', 20,  80,   0,  10,  10,   0, new Shape(1,  3,  true,  true))
    };

    private static Phoneme find(char c) {
        for (Phoneme p : phonemes) {
            if (p.c == c) {
                return p;
            }
        }
        return null;
    }

    private static double cutLevel(double x, double lvl) {
        if (x < -lvl) {
            return -lvl;
        } else if (x > lvl) {
            return lvl;
        } else {
            return x;
        }
    }

    public static short[] generate(String s) {
        double[] buf = new double[PLAY_TIME*SAMPLE_FREQUENCY*2];

        int k = synthSpeech(buf, 0, s);
        short[] finalBuf = new short[k/2];

        for (int i = 0; i < k/2; i += 1) {
            double f0 = buf[i*2];
            double f1 = buf[i*2 + 1];
            finalBuf[i] = (short)(32700.0 * cutLevel((f0 + f1) / 2 * MASTER_VOLUME, 1));
        }
        return finalBuf;
    }


    private static int synthSpeech(double[] buf, int k, String text) {
        for (int i = 0; i < text.length(); i++){
            char l = text.charAt(i);
            double v = 0;
            Phoneme p = find(l);
            if (p == null) {
                p = phonemes[0];
            }
            if (l != ' ') {
                v = p.shape.amp;
            }

            int sl = p.shape.len * (SAMPLE_FREQUENCY / 15);
            for (int f = 0;  f < 3; f += 1) {
                int ff = p.f[f];
                double freq = (double)ff * (50.0/SAMPLE_FREQUENCY);
                if (ff == 0) {
                    continue;
                }
                double buf1Res = 0;
                double buf2Res = 0;
                double q = 1.0 - p.w[f] * (PI * 10.0 / SAMPLE_FREQUENCY);
                int j = 0;
                double xp = 0;
                for (int s = 0; s < sl; s += 1) {
                    double n = Math.random() - 0.5;
                    double x = n;
                    if (!p.shape.osc) {
                        x = sawtooth(s * (120.0 * PI_2 / SAMPLE_FREQUENCY));
                        xp = 0;
                    }
                    x = x + 2.0 * Math.cos(PI_2 * freq) * buf1Res * q - buf2Res * q * q;
                    buf2Res = buf1Res;
                    buf1Res = x;
                    x = 0.75 * xp + x * v;
                    xp = x;
                    x *= cutLevel(Math.sin((PI * s) / sl) * 5, 1);
                    buf[k+j] += x;
                    j += 1;
                    buf[k+j] += x;
                    j += 1;
                }
            }

            k += ((3*sl/4)<<1);
            if (p.shape.plosive) {
                k += (sl & 0xfffffe);
            }

        }
        return k;
    }
}

class Shape {
    public int len;
    public int amp;
    public boolean osc;
    public boolean plosive;
    public Shape(int len, int amp, boolean osc, boolean plosive) {
        this.len = len;
        this.amp = amp;
        this.osc = osc;
        this.plosive = plosive;
    }
}

class Phoneme {
    public char c;
    public int[] f;
    public int[] w;
    public Shape shape;
    public Phoneme(char c, int f0, int f1, int f2, int w0, int w1, int w2, Shape shape) {
        this.c = c;
        this.f = new int[] { f0, f1, f2 };
        this.w = new int[] { w0, w1, w2 };
        this.shape = shape;
    }
}



