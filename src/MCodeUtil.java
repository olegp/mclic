/*
 * MCodeUtil.java
 *
 * Created on 10 June 2006, 21:44
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author Alex Lam Sze Lok
 */
public final class MCodeUtil {
    /** Creates a new instance of MCodeUtil */
    private MCodeUtil() {}

    public static void fromRGB(int[] input) {
        for (int i = 0; i < input.length; i++) {
            input[i] = (((input[i] & 0x00FF0000) >> 16) + ((input[i] & 0x0000FF00) >> 8)) >> 1;
        }
    }
    
    public static void toRGB(int[] input) {
        for (int i = 0; i < input.length; i++) {
            input[i] |= (input[i] << 8) | (input[i] << 16) | 0xFF000000;
        }
    }

    public static final int WIDTH = 160;
    public static final int HEIGHT = 120;
    private static final int X_LIMIT = WIDTH - 8;
    private static final int Y_LIMIT = HEIGHT - 8;
    private static final int ROW0_DY_START = -7 * WIDTH;
    private static final int ROW1_DY_START = (-7 + 9) * WIDTH;
    private static final int KERNEL_SHIFT = 15; // Kernel's size - 1

    public static void cornerFilter(int[] input) {
        for (int index = 0, y = 7, y0 = y * WIDTH, y1 = (y+1)*WIDTH; y < Y_LIMIT; y++, y0 += WIDTH, y1 += WIDTH, index += KERNEL_SHIFT) {
            for (int x = 7; x < X_LIMIT; x++, index++) {
                int pSum = 0, nSum = 0;
                for (int dx = -7, xpdx = x+dx; dx < 9; dx++, xpdx++) {
                    nSum += input[y0+xpdx] + input[y1+xpdx];
                }
                for (int dy = -7, row0px = y0+ROW0_DY_START+x, row1px = y0+ROW1_DY_START+x; dy < 0; dy++, row0px += WIDTH, row1px += WIDTH) {
                    for (int dx = -7, dxp9 = 2; dx < 0; dx++, dxp9++) {
                        pSum += input[row0px+dx] + input[row0px+dxp9] + input[row1px+dx] + input[row1px+dxp9];
                    }
                    nSum += input[row0px] + input[row0px+1] + input[row1px] + input[row1px+1];
                }
                int sum = (pSum - (nSum << 2)) >> 7;
                int mask = sum & 0xFF000000;
                int overflow = sum & 0x100;
                overflow = (overflow >> 1) | (overflow >> 2);
                overflow |= overflow >> 2;
                overflow |= overflow >> 4;
                mask = (~mask) >>> 24;
                input[index] = (sum | overflow) & mask;
            }
        }
    }

    private static class Link {
        public Link prev = null;
        public final int index, intensity;

        public Link(int index, int intensity) {
            this.index = index;
            this.intensity = intensity;
        }
    }

    private static class MaxFinder {
        public final int NUMBER_OF_MAX;
        private Link last = null;
        private int itemsCount = 0;

        public MaxFinder(int NUMBER_OF_MAX) {
            this.NUMBER_OF_MAX = NUMBER_OF_MAX;
        }

        public void updateLinks(final int index, final int intensity) {
            Link link = last;
            Link nextLink = null;
            while (link != null && intensity > link.intensity) {
                nextLink = link;
                link = link.prev;
            }
            Link newLink = new Link(index, intensity);
            newLink.prev = link;
            if (link == last) {
                last = newLink;
            } else {
                nextLink.prev = newLink;
            }
            if (itemsCount < NUMBER_OF_MAX) {
                itemsCount++;
            } else {
                last = last.prev;
            }
        }

        public Link[] getIndices() {
            Link[] result = new Link[itemsCount];
            Link link = last;
            for (int index = itemsCount; --index >= 0; result[index] = link, link = link.prev);
            return result;
        }
    }

    private static int[] _findCorners(Link[] pivots, Link[] points0, Link[] points1, final int CRITICAL_INTENSITY) {
        int bestp = 0, best0 = 0, best1 = 0;
        int minMeasure = Integer.MAX_VALUE;
        for (int ip = 0; ip < pivots.length; ip++) {
            if (pivots[ip].intensity < CRITICAL_INTENSITY) {
                continue;
            }
            int px = pivots[ip].index % WIDTH;
            int py = pivots[ip].index / WIDTH;
            for (int i0 = 0; i0 < points0.length; i0++) {
                if (points0[i0].intensity < CRITICAL_INTENSITY) {
                    continue;
                }
                int p0x = points0[i0].index % WIDTH;
                int p0y = points0[i0].index / WIDTH;
                for (int i1 = 0; i1 < points1.length; i1++) {
                    if (points1[i1].intensity < CRITICAL_INTENSITY) {
                        continue;
                    }
                    int p1x = points1[i1].index % WIDTH;
                    int p1y = points1[i1].index / WIDTH;
                    int ax = p0x - px;
                    int ay = p0y - py;
                    int bx = p1x - px;
                    int by = p1y - py;
                    int area = ax*by - ay*bx;
                    if (area >= 2304) {
                        int SqAreasSum = (ax*ax + ay*ay) + (bx*bx + by*by);
                        int measure = (SqAreasSum - (area << 1));
                        if (minMeasure > measure) {
                            bestp = ip;
                            best0 = i0;
                            best1 = i1;
                            minMeasure = measure;
                        }
                    }
                }
            }
        }
        return new int[] {pivots[bestp].index, points0[best0].index, points1[best1].index, minMeasure};
    }

    private static final int FILTERED_WIDTH = WIDTH - KERNEL_SHIFT;
    private static final int FILTERED_HEIGHT = HEIGHT - KERNEL_SHIFT;
    private static final int FILTERED_LENGTH = FILTERED_WIDTH * FILTERED_HEIGHT;
    private static final int QUADRANT_X_SHIFT = FILTERED_WIDTH >> 1;
    private static final int QUADRANT_Y_SHIFT = (FILTERED_HEIGHT >> 1) * WIDTH;
    private static final int FILTERED_CENTER = QUADRANT_Y_SHIFT + QUADRANT_X_SHIFT;
    private static final int NUM_OF_MAX = 4;
    private static final int ROW_SHIFT = WIDTH - QUADRANT_X_SHIFT;

    public static int[] findCorners(int[] filteredInput) {
        MaxFinder max0 = new MaxFinder(NUM_OF_MAX);
        MaxFinder max1 = new MaxFinder(NUM_OF_MAX);
        MaxFinder max2 = new MaxFinder(NUM_OF_MAX);
        MaxFinder max3 = new MaxFinder(NUM_OF_MAX);
        for (int index0 = 0, index1 = QUADRANT_X_SHIFT, index2 = QUADRANT_Y_SHIFT, index3 = index1 + index2; index0 < FILTERED_CENTER; index0 += ROW_SHIFT, index1 += ROW_SHIFT, index2 += ROW_SHIFT, index3 += ROW_SHIFT) {
            for (int x = 0; x < QUADRANT_X_SHIFT; x++, index0++, index1++, index2++, index3++) {
                max0.updateLinks(index0, filteredInput[index0]);
                max1.updateLinks(index1, filteredInput[index1]);
                max2.updateLinks(index2, filteredInput[index2]);
                max3.updateLinks(index3, filteredInput[index3]);
            }
        }
        Link[] m0 = max0.getIndices();
        Link[] m1 = max1.getIndices();
        Link[] m2 = max2.getIndices();
        Link[] m3 = max3.getIndices();
        // critical intensity is 7/16 of the maximum intensity of the filtered image
        int CRITICAL_INTENSITY = Math.max(m0[0].intensity, Math.max(m1[0].intensity, Math.max(m2[0].intensity, m3[0].intensity)));
        CRITICAL_INTENSITY = ((CRITICAL_INTENSITY << 3) - CRITICAL_INTENSITY) >> 4;
        int[] bestResult = _findCorners(m0, m1, m2, CRITICAL_INTENSITY);
        int[] result = _findCorners(m1, m3, m0, CRITICAL_INTENSITY);
        if (bestResult[3] > result[3]) {
            bestResult = result;
        }
        result =  _findCorners(m2, m0, m3, CRITICAL_INTENSITY);
        if (bestResult[3] > result[3]) {
            bestResult = result;
        }
        result =  _findCorners(m3, m2, m1, CRITICAL_INTENSITY);
        if (bestResult[3] > result[3]) {
            bestResult = result;
        }
        result = new int[]{bestResult[0], bestResult[1], bestResult[2], bestResult[2] + bestResult[1] - bestResult[0], bestResult[3]};
        for (int i = 0; i < 4; i++) {
            if (result[i] >= FILTERED_LENGTH) {
                result[i] = 0;
                result[4] = Integer.MAX_VALUE;
            }
        }
        return result;
    }

    public static boolean matchesShapeAndSize(int[] corners) {
        return corners[4] < 100;
    }
}
