    synctable - generates sync scroll tables

    Usage:

    java -jar synctable.jar 

    ... but that does nothing unless you set the proper values in the source and compile
    it yourself first, of course. 

    linelengths:    an array of which line lengths your sync scroll code can generate.
    VERTICAL160:    if you don't want a full table but the first one that can do a
                    vertical 160 byte screen scroll
    STATIC_VIDMEM:  force video memory address to never change during scroll. useful
                    for dual playfield stuff.

    The code will brute force (or random walk if the search space is too big) the lowest
    number of lines needed for your sync table, and output all relevant data including
    Devpac compatible data structures.

    A variant of this program was used to generate the sync tables used in both LoSTE
    and {Closure}

    Written by Troed of SYNC

    Licensed under Creative Commons Zero

