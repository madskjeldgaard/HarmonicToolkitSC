// Represents a harmony with a set of harmonic intervals
Harmony{
    classvar <all;
    var <intervalsOriginal;
    var <inversions, <doublings;

    *initClass{
        all = [
            // Minor 2nd
            \m2 -> [0, 1],

            // Major 2nd
            \M2 -> [0, 2],

            // Minor 3rd
            \m3 -> [0, 3],

            // Major 3rd
            \M3 -> [0, 4],

            // Perfect 4th
            \P4 -> [0, 5],

            // Augmented 4th
            \A4 -> [0, 6],

            // Diminished 5th
            \d5 -> [0, 6],

            // Perfect 5th
            \P5 -> [0, 7],

            // Minor 6th
            \m6 -> [0, 8],

            // Major 6th
            \M6 -> [0, 9],

            // Minor 7th
            \m7 -> [0, 10],

            // Major 7th
            \M7 -> [0, 11],

            // Perfect 8th
            \P8 -> [0, 12],

            // Major
            \major -> [0, 4, 7],

            // Minor
            \minor -> [0, 3, 7],

            // Diminished
            \diminished -> [0, 3, 6],

            // Augmented
            \augmented -> [0, 4, 8],

            // Major 7th
            \major7 -> [0, 4, 7, 11],

            // Minor 7th
            \minor7 -> [0, 3, 7, 10],

            // Dominant 7th
            \dominant7 -> [0, 4, 7, 10],

            // Diminished 7th
            \diminished7 -> [0, 3, 6, 9],

            // Half Diminished 7th
            \halfDiminished7 -> [0, 3, 6, 10],

            // Major 9th
            \major9 -> [0, 4, 7, 11, 14],

            // Minor 9th
            \minor9 -> [0, 3, 7, 10, 14],

            // Dominant 9th
            \dominant9 -> [0, 4, 7, 10, 14],

            // Major 13th
            \major13 -> [0, 4, 7, 11, 14, 21],

            // Dominant 13th
            \dominant13 -> [0, 4, 7, 10, 14, 21],
        ].asDict;
    }

    *intervals{
        ^this.all
    }

    *at{|name|
        var interval = this.all[name];

        if(interval.isNil,{
            "Interval not found".warn;
        });

        ^interval
    }


    // Create instance
    *new{|intervals|
        ^super.new.init(intervals)
    }

    init{|intervals|

        // Store original intervals
        intervalsOriginal = intervals.copy;


        this.clearInversions;
        this.clearDoubles;

        ^this
    }

    // withDouble{|patternType=\root, octaves=1|
    //     var doubledOriginal = ChordOps.double(intervalsOriginal, patternType, octaves);

    //     ^this
    // }

    // withInversion{|patternType=\first, style=\up|
    //     intervalsModified = ChordOps.invert(intervalsOriginal, patternType, style);

    //     ^this
    // }

    addInversion{|patternType=\first, style=\up|
        inversions = inversions.add([patternType, style]);

        inversions = inversions.removeDuplicates;

        ^this
    }

    addDouble{|patternType=\root, octaves=1|
        doublings = doublings.add([patternType, octaves]);

        doublings = doublings.removeDuplicates;

        ^this
    }

    clearInversions{
        inversions = [];

        ^this
    }

    clearDoubles{
        doublings = [];

        ^this
    }

    get{
        var full = intervalsOriginal;

        // Add doubles and inversions
        doublings.do{|double|
            full = ChordOps.double(full, double[0], double[1])
        };

        inversions.do{|inversion|
            full = ChordOps.invert(full, inversion[0], inversion[1])
        };

        ^full.removeDuplicates.sort
    }

}


// Perform chord operations on an array of intervals
// All operations happen in semitone offsets
ChordOps{

    *prClean{|intervals|
        // Remove duplicates
        ^intervals.removeDuplicates
    }

    *transpose{|intervals, amount=0|
        ^intervals.collect{|interval| interval + amount}
    }

    *double{|intervals, patternType=\root, octaves=1|
        var doubledIntervals = intervals.copy;

        patternType.switch(
            \root, {
                octaves.abs.do{|i|
                    var octaveShift = 12 * (i + 1) * octaves.sign;
                    doubledIntervals = doubledIntervals.add(intervals[0] + octaveShift)
                }
            },
            \second, {
                octaves.abs.do{|i|
                    var octaveShift = 12 * (i + 1) * octaves.sign;
                    doubledIntervals = doubledIntervals.add(intervals[1] + octaveShift)
                }
            },
            \third, {
                octaves.abs.do{|i|
                    var octaveShift = 12 * (i + 1) * octaves.sign;
                    doubledIntervals = doubledIntervals.add(intervals[2] + octaveShift)
                }
            },
            \rootandsecond, {
                octaves.abs.do{|i|
                    var octaveShift = 12 * (i + 1) * octaves.sign;
                    doubledIntervals = doubledIntervals.add(intervals[0] + octaveShift);
                    doubledIntervals = doubledIntervals.add(intervals[1] + octaveShift);
                }
            },
            \rootandthird, {
                octaves.abs.do{|i|
                    var octaveShift = 12 * (i + 1) * octaves.sign;
                    doubledIntervals = doubledIntervals.add(intervals[0] + octaveShift);
                    doubledIntervals = doubledIntervals.add(intervals[2] + octaveShift);
                }
            },
            \secondandthird, {
                octaves.abs.do{|i|
                    var octaveShift = 12 * (i + 1) * octaves.sign;
                    doubledIntervals = doubledIntervals.add(intervals[1] + octaveShift);
                    doubledIntervals = doubledIntervals.add(intervals[2] + octaveShift);
                }
            },
            \all, {
                octaves.abs.do{|i|
                    var octaveShift = 12 * (i + 1) * octaves.sign;
                    doubledIntervals = doubledIntervals.add(intervals[0] + octaveShift);
                    doubledIntervals = doubledIntervals.add(intervals[1] + octaveShift);
                    doubledIntervals = doubledIntervals.add(intervals[2] + octaveShift);
                }
            }
        );

        // Sort
        ^doubledIntervals.removeDuplicates.sort
    }

    *invert{|intervals, patternType=\first, style=\up|
        var invertedIntervals = intervals.copy;

        var inversionPattern = patternType.switch(
            \first, { [1, 0, 0] },
            \second, { [1, 1, 0] },
            \third, { [1, 1, 1] },
            \middle, { [0, 1, 0] },
            \top, { [0, 0, 1] },
            \edges, { [1, 0, 1] },
            \none, { [0, 0, 0] }
        );

        inversionPattern.do{|dist, index|
            if (dist != 0) {
                style.switch(
                    \up, { invertedIntervals[index] = invertedIntervals[index] + (12 * dist) },
                    \down, { invertedIntervals[index] = invertedIntervals[index] - (12 * dist) },
                    \updown, {
                        if (index % 2 == 0) {
                            invertedIntervals[index] = invertedIntervals[index] + (12 * dist)
                        } {
                            invertedIntervals[index] = invertedIntervals[index] - (12 * dist)
                        }
                    },
                    \downup, {
                        if (index % 2 == 0) {
                            invertedIntervals[index] = invertedIntervals[index] - (12 * dist)
                        } {
                            invertedIntervals[index] = invertedIntervals[index] + (12 * dist)
                        }
                    }
                )
            }
        };

        ^invertedIntervals.removeDuplicates.sort
    }

    *revert{|intervals, patternType, style|
        var revertedStyle = style.switch(
            \up, { \down },
            \down, { \up },
            \updown, { \downup },
            \downup, { \updown }
        );

        ^ChordOps.invert(intervals, patternType, revertedStyle)
    }

}

+Array{
    harmonicDouble{|patternType=\root, octaves=1|
        ^ChordOps.double(this, patternType, octaves)
    }

    harmonicInvert{|patternType=\first, style=\up|
        ^ChordOps.invert(this, patternType, style)
    }

    harmonicRevert{|patternType, style|
        ^ChordOps.revert(this, patternType, style)
    }
}
