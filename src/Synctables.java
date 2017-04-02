import java.io.DataInputStream;
import java.io.IOException;

/**
 * Exhaustive and random walk search for combinations leading to 0,2,4 ... 254
 *
 * Specify number of line lengths to use and it will search and/or walk randomly until it finds solution
 * (random walk because 4^17 = 17 billion combinations, for example, the old TCB sync scroll routine)
 *
 * Findings:    4 lines 12 combos is trivially found
 *              17 lines 4 combos (TCB SoWatt, assuming Cuddly) takes ~20 seconds or so
 *              16 lines 4 combos aborted due to not finding 2 last combos for several minutes
 *              20 lines 3 combos (TCB Enchanted Lands) fails finding 1 last combo (pos 110) todo: hmm?
 *
 *              Trivia: TCB Enchanted Land sync scroll only uses 230, 204 and 160 byte lines. x number of 230 byte
 *                      lines followed by y number of 204 byte lines - up to a maximum of 20.
 *
 *                      TCB SoWatt sync scroll uses 186, 204, 230 and 160 line lengths - and 17 lines.
 *
 *
 * Vertical scroll only special cases, 160 and 230 byte wide screens:
 *
 * Mid video mem byte can be changed in 256 byte increments, so the combinations we need are:
 *
 * 160
 * ---
 * (0)
 * 160   256- 160 =  96
 * 320   512- 320 = 192
 * 480   512- 480 =  32
 * 640   768- 640 = 128
 * 800  1024- 800 = 224
 * 960  1024- 960 =  64
 * 1120 1280-1120 = 160
 * 1280 1280-1280 =   0 (256)
 *
 * 230
 * ---
 * (0)
 * 230
 * 460
 * 690
 * 920
 * 1150
 * ...
 * (hmm when does a multiple of 230 % 256 == 0?)
 * answer: 29440 (!)
 * can't imagine we gain much from doing this table then?
 *
 */
public class Synctables {

    static boolean RANDOM_WALK = false; // if set to true then it starts walking randomly disregarding max brute force below
    static double max = 128L*1024L*1024L; // maximum number of combinations to brute force before switching to random walk
    // (128M is a suitable number for a laptop i7)
    static int timeout = 240; // timeout in seconds for random walk before increasing lines

    static boolean VERTICAL160 = false; // if we should analyze incomplete tables for possible vertical scroll

    static boolean STATIC_VIDMEM = false; // set to true for manual tuning using a static video mem offset (multiple of 256)
    static int vidmem_offset = 512; // force a static video mem offset (multiple of 256)
    // known case: offset 512 and a full 12 linelength scroller exists in 5 lines (used in {Closure})
    // todo: this could be improved by adding support for dynamically finding one instead of supplying the value of course

    // todo, maybe, support subset on syncline (for those who don't synclock to HBL) and maybe also last line

//    static int[] linelengths = new int[]{160,204,230}; // Enchanted Land
//    static int[] linelengths = new int[]{160,186,204,230}; // SoWatt / Cuddly
//    static int[] linelengths = new int[]{160,230,184,204,54,80,158,186,206}; // pre-2006 state of the art (5 lines)
    static int[] linelengths = new int[]{160,162,230,184,204,0,54,56,80,158,186,206}; // Max 12 possible (4 lines)
// no mono switches - can use rasters/background color
//    static int[] linelengths = new int[]{160,162,204,0,158,206}; // all possible (8 lines needed)
//    static int[] linelengths = new int[]{160,204,158}; // no wakestate understanding needed (19 lines)

    static int nolengths = linelengths.length;
    static int[][] table;

    public static void main(String[] args) {

        int countmissing = 1; // to get us started
        int lines = 0;  // number of lines needed for the sync scroll
        boolean foundvertical = false;

        while(countmissing > 0 && !foundvertical) {
            lines++;

            System.out.println("Calculating " + lines + " lines of " + nolengths + " combinations");
            System.out.println("Press Enter to increase number of lines, ESC+Enter to stop searching");
            double total = Math.pow(nolengths, lines);
            if(total > max) {
                System.out.println("Search space bigger than " + (long)max + ", switching to random walk");
                RANDOM_WALK = true;
            } else {
                System.out.println("Total searchspace: " + (long)total + " calculations");
            }

            table = new int[128][lines];

            for(int x = 0; x < table.length; x++) {
                table[x][0] = -1;
            }

            // table of combos
            int[][] templines = new int[lines][nolengths];
            for (int j = 0; j < lines; j++) {
                for (int i = 0; i < nolengths; i++) {
                    templines[j][i] = linelengths[i];
                }
            }

            countmissing = 128; // positions to go

            System.out.println("Evaluating combos");

            // 4^17 combos is 17 billion evaluations ... I need to rethink this.

    /*
     ... 160,160,160,160
     ... 160,160,160,186
     ... 160,160,160,204
     ... 160,160,160,230

     ... 160,160,186,160
     ... 160,160,186,186
     ... 160,160,186,204
     ... 160,160,186,230

    Some sort of annihilation possible?

        160
        186  <-- same as 186 160
    160 204  <-- same as 204 160
        230  <-- same as 230 160 below, if we assume order is irrelevant (true for simple cases)

        160
        186
    186 204  <-- same as 204 186
        230  <-- same as 230 186

        160
        186
    204 204
        230  <-- same as 230 204

        160
        186
    230 204
        230

    So instead of 4^2=16 combinations above we have 4+3+2+1 = 10. With order irrelevant these paths are now closed onwards, an immediate 37.5%
    reduction of the search space. This should then continue onwards. 186 160 204 would cancel out 204 160 186 etc. Not sure how to implement
    a quick version of this though?

    */

            DataInputStream dis = new DataInputStream(System.in);
            long start = System.currentTimeMillis();
            long end = start + timeout*1000;

            try {
                if (!RANDOM_WALK) {
                    // Proper exhaustive and deterministic solution. Trivial.
                    // have nolines array with indexes into nolengths. Increase index rightmost, at overflow increase left.
                    // All the way back to 0. Easy.
                    long totalCount = 0;
                    int counter[] = new int[lines];
                    int i = counter.length - 1;
                    while (i >= 0 && countmissing > 0 && dis.available() == 0) {
                        int bytes = 0;
                        for (int k = 0; k < counter.length; k++) {
                            int temp = templines[k][counter[k]];
                            //                        System.out.print(temp + " ");
                            bytes += temp;
                        }
                        totalCount++;

                        int offset = bytes;                       // total byte calc

                        if(STATIC_VIDMEM && (offset < vidmem_offset || offset >= vidmem_offset+256)) {
                            // noop
                        } else {

                            offset %= 256;
                            offset /= 2;
                            // specific offset empty?
                            if (table[offset][0] == -1) {
                                for (int j = 0; j < lines; j++) {
                                    table[offset][j] = templines[j][counter[j]];
                                }
                                countmissing--;
                                // output progress
                                int percentage = (int) (((double) totalCount / total) * 100);
                                //                        int major = (int)(((double)(nolines - i)/(double)nolines)*100);
                                System.out.println("Position " + offset * 2 + " found, " + countmissing + " combos missing, " + percentage + "% searched");
                            }
                        }
                        int k = i;
                        counter[k]++;
                        while (counter[k] == nolengths) {
                            counter[k] = 0;
                            if (k > 0) {
                                k--;
                                counter[k]++;
                            } else {
                                i--;
                            }
                        }
                    }
                } else {
                    // when the search space is too big - do random walks and see if and offset can be found
                    // 4 line lengths 18 lines takes 10 seconds or so on a Core 2 Duo so this is a very valid approach
                    while (countmissing > 0 && dis.available() == 0 && System.currentTimeMillis() < end) {
                        int bytes = 0;
                        int walk[] = new int[lines];
                        for (int j = 0; j < lines; j++) {
                            int ran = (int) Math.round(Math.random() * (nolengths - 1));
                            int path = templines[j][ran];
                            walk[j] = path;
                            bytes += path;
                        }

                        int offset = bytes;                       // total byte calc

                        if(STATIC_VIDMEM && (offset < vidmem_offset || offset >= vidmem_offset+256)) {
                            // noop
                        } else {
                            offset %= 256;
                            offset /= 2;
                            // specific offset empty?
                            if (table[offset][0] == -1) {
                                for (int j = 0; j < lines; j++) {
                                    table[offset][j] = walk[j];
                                }
                                countmissing--;
                                System.out.println("Position " + offset * 2 + " found, " + countmissing + " combos missing");
                            }
                        }
                    }
                }
                if(VERTICAL160) {
                    boolean found = false;
                    int i;
                    for(i = 0; i < 16 && !found; i++) {
                        found = true;
                        for (int k = i; k < 128; k += 16) {  // "16" is in reality 32
                            if (table[k][0] == -1) {
                                found = false;
                            }
                        }
                    }
                    if(found) {
                        System.out.println("Vertical scroll possible, offset: " + (i-1)*2);
                        foundvertical = true;
                    }
                }
                int read = dis.available();
                boolean breaker = false;
                while(read > 0) {
                    int r = dis.read();
                    if(r == 27) { // ESC
                        breaker = true;
                    }
                    read = dis.available();
                }
                if(breaker) {
                    break;
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        // Output the complete table (or in vertical case, maybe incomplete)

        for(int x = 0; x < table.length; x++) {
            if(table[x][0] == -1) {
                System.out.println(x*2 + ": no line");
            } else {
                System.out.print(x*2 + ":");
                int tot = 0;
                for(int j=0; j<lines; j++) {
                    int val = table[x][j];
                    tot += val;
                    System.out.print(" " + val);
                }
                System.out.println(" Total: " + tot);
            }
        }

        System.out.println("*********************************************");
        System.out.println("*  Sync scroll table creator by Troed/SYNC  *");
        System.out.print("*  " + linelengths.length + " line lengths used");
        if(linelengths.length < 10) {
            System.out.println("                      *");
        } else {
            System.out.println("                     *");
        }
        System.out.println("*********************************************");

        // Output as include file for Devpac
        System.out.println("* Columns as indexes into line-rout table. Last value is 256 byte offset");
        System.out.print("_linsrc\tdc.l ");
        int x;
        for(x = 0; x < linelengths.length-1; x++) {
            System.out.print("_s" + linelengths[x] + ",");
        }
        System.out.println("_s" + linelengths[x]);
        System.out.println("_synctab");
        for(x = 0; x < table.length; x++) {
            System.out.print("\tdc.b ");
            int tot = 0;
            if(table[x][0] == -1) {
                for(int i=0;i<lines-1;i++) {
                    System.out.print("0,");
                }
                System.out.println("0,0\t\t* " + x*2 + " not found");
            } else {
                // lookup index in linelengths
                for(int j=0;j<lines;j++) {
                    for(int i=0;i<linelengths.length;i++) {
                        if(linelengths[i] == table[x][j]) {
                            System.out.print(i + ",");
                            tot+=table[x][j];
                            break;
                        }
                    }
                }
                System.out.println((int)(tot/256) + "\t\t* " + x*2 + " (" + tot + ")");
            }
        }

        if(countmissing == 0 || foundvertical) {
            System.out.println(lines + " lines needed for the sync scroll");
        }
        else {
            System.out.println(countmissing + " combos missing");
        }
    }
}