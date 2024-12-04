/*

Example:

(
    var h = Harmony.new(\major);
    h.addInversion(\first, \up);
    h.addDouble(\root, 2);
    h.get;
)


Pattern:

// Arpeggiate inversions of a chord
(
var h = Harmony.new(\major);

Pdef(\fast,
    Pbind(
        \octave, 4,
        \degree, 0,
        \mtranspose,
        Pseq([
            h.asPseq(4),
            h.withInversion(\first, \up).asPseq(4),
            h.withInversion(\second, \up).asPseq(4),
            h.withInversion(\first, \down).asPseq(4),
            h.withInversion(\second, \down).asPseq(4),
        ], inf),
        \dur, 0.125
    )
).play;

)

*/
// Represents a harmony with a set of harmonic intervals
Harmony{
    classvar <all;
    var <intervalsOriginal;
    var <inversions, <doublings;

    *initClass{
        all = [

            // No harmonics at all
            \none -> [0],

            // The same as above but with a more explicit name
            \unison -> [0, 0],

            // Simple harmonics

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

            // 3 tone chords

            // Major
            \major -> [0, 4, 7],

            // Minor
            \minor -> [0, 3, 7],

            // Diminished
            \diminished -> [0, 3, 6],

            // Augmented
            \augmented -> [0, 4, 8],

            // 4 tone chords

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

            // Large chords

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

        if(intervals.isSymbol, {
            intervals = this.class.at(intervals)
        });

        if(intervals.isNil, {
            "Invalid intervals: %".format(intervals).error;
            ^nil
        });

        // Store original intervals
        intervalsOriginal = intervals.copy;

        this.clearInversions;
        this.clearDoubles;

        ^this
    }

    addInversion{|patternType=\first, style=\up|
        inversions = inversions.add([patternType, style]);

        inversions = inversions.removeDuplicates;

        ^this
    }

    /*

    // Add a dooubling of the root notes either below or above the original notes
    // Negative octaves value will add the doubling below the original notes
    */
    addDouble{|patternType=\root, octaves=(-1)|
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

    get {
        var result = intervalsOriginal.copy;

        inversions.do { |inversion|
            result = ChordOps.invert(result, inversion[0], inversion[1]);
        };

        doublings.do { |doubling|
            var doubledOffsets = ChordOps.double(intervalsOriginal, doubling[0], doubling[1]);

            // Only add the doubled, unique offsets to the result
            doubledOffsets = doubledOffsets.reject { |offset|
                intervalsOriginal.includes(offset)
            };

            result = result.add(doubledOffsets);
        };

        ^ChordOps.prClean(result)
    }

    value {
        ^this.get
    }

    // Same as .addInversion but creates a copy of the instance and returns that
    withInversion{|patternType=\first, style=\up|
        ^this.copy.addInversion(patternType, style)
    }

    // Same as .addDouble but creates a copy of the instance and returns that
    withDouble{|patternType=\root, octaves=1|
        ^this.copy.addDouble(patternType, octaves)
    }

    asStream{
        ^this.get.asStream()
    }

}



// Perform chord operations on an array of intervals
// All operations happen in semitone offsets
ChordOps{

    *doublingTypes{
        ^[\root, \second, \third, \rootandsecond, \rootandthird, \secondandthird, \all]
    }

    *inversionTypes{
        ^[\first, \second, \third, \middle, \top, \edges, \none]
    }

    *prClean{|intervals|
        // Remove duplicates
        ^intervals.flat.removeDuplicates
    }

    *transpose{|intervals, amount=0|
        ^intervals.collect{|interval| interval + amount}
    }

    *double{|intervals, patternType=\root, octaves=1|
        var doubledIntervals = intervals.copy;

        var octaveShiftSemitones = {|x|
            // var oct = if (x % 2 == 0) { -12 } { 12 };
            var oct = 12;

            oct * (x + 1) * octaves.sign;
        };

        // Check if the pattern type is valid
        if(this.doublingTypes.includes(patternType).not, {
            "Invalid pattern type: %".format(patternType).error;
            ^nil
        }, {
            octaves.abs.do{|i|
                patternType.switch(
                    \root, {
                        doubledIntervals = doubledIntervals.add(doubledIntervals[0] + octaveShiftSemitones.value(i))
                    },
                    \second, {
                        doubledIntervals = doubledIntervals.add(doubledIntervals[1] + octaveShiftSemitones.value(i))
                    },
                    \third, {
                        doubledIntervals = doubledIntervals.add(doubledIntervals[2] + octaveShiftSemitones.value(i))
                    },
                    \rootandsecond, {
                        doubledIntervals = doubledIntervals.add(doubledIntervals[0] + octaveShiftSemitones.value(i));
                        doubledIntervals = doubledIntervals.add(doubledIntervals[1] + octaveShiftSemitones.value(i));
                    },
                    \rootandthird, {
                        doubledIntervals = doubledIntervals.add(doubledIntervals[0] + octaveShiftSemitones.value(i));
                        doubledIntervals = doubledIntervals.add(doubledIntervals[2] + octaveShiftSemitones.value(i));
                    },
                    \secondandthird, {
                        doubledIntervals = doubledIntervals.add(doubledIntervals[1] + octaveShiftSemitones.value(i));
                        doubledIntervals = doubledIntervals.add(doubledIntervals[2] + octaveShiftSemitones.value(i));
                    },
                    \all, {
                        doubledIntervals = doubledIntervals.add(doubledIntervals[0] + octaveShiftSemitones.value(i));
                        doubledIntervals = doubledIntervals.add(doubledIntervals[1] + octaveShiftSemitones.value(i));
                        doubledIntervals = doubledIntervals.add(doubledIntervals[2] + octaveShiftSemitones.value(i));
                    }
                );
            };

            // Sort and clean and return
            ^this.prClean(doubledIntervals)
        });
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

        // Check if the pattern type is valid
        if(this.inversionTypes.includes(patternType).not, {
            "Invalid pattern type: %".format(patternType).error;
            ^nil
        }, {


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

            ^this.prClean(invertedIntervals)
        });

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

// Arpeggiate a harmony
/*
// Example:
(
p = \major7.asHarmony.asPseq(repeats: inf);
z = p.asStream;
z.next()
)
*/
+ Harmony {
    asPseq{|repeats=inf|
        ^Pseq.new(this.get, repeats)
    }

    asPrand{|repeats=inf|
        ^Prand.new(this.get, repeats)
    }

    asPxrand{|repeats=inf|
        ^Pxrand.new(this.get, repeats)
    }

    asPshuffle{|repeats=inf|
        ^Pshuffle.new(this.get, repeats)
    }
}

// A useful way to convert a single symbol into a harmony
/*
// EXAMPLE
(
p = Pseq([\m3, \M3, \P4, \P5], inf).collect{|key| key.asHarmony.get() };
z = p.asStream;
z.next()
)

*/
+ Symbol {
    asHarmony{
        ^Harmony.new(this)
    }
}
