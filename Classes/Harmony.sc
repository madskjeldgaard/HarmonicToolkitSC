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
// Represents a harmony with a set of harmonic intervals
Harmony {
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

    *harmonyNames{
        ^this.all.keys.asArray
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

    *random{
        var randomName = this.all.keys.choose;

        ^this.new(randomName)
    }

    *randomTriad{
        var randomName = [\major, \minor, \diminished, \augmented].choose;

        ^this.new(randomName)
    }

    *randomSeventh{
        var randomName = [\major7, \minor7, \dominant7, \diminished7, \halfDiminished7].choose;

        ^this.new(randomName)
    }

    init{|intervals|

        if(intervals.class == Symbol, {
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

    at{|index|
        var semitones = intervalsOriginal[index];

        if(semitones.isNil,{
            "Interval not found".warn;
        });

        ^semitones
    }

    // Add inversion with full numeric support
    addInversion{|patternType=\first, style=\up|
        inversions = inversions.add([patternType, style]);
        inversions = inversions.removeDuplicates;
        ^this
    }

    // Add doubling with full numeric support
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
    withDouble{|patternType=\root, octaves=(-1)|
        ^this.copy.addDouble(patternType, octaves)
    }

    asStream{
        ^this.get.asStream()
    }

    asHarmony {
        ^this
    }

    asMidiNoteNumbers{|midiNoteOffset=60|
        ^this.get.collect{|interval| interval + midiNoteOffset};
    }

    asPHarmony{|arpStyles=\chords, midiNoteOffset=0|
        ^PHarmony(harmoniesArray: this, arpStyles: arpStyles, midiNoteOffset:midiNoteOffset)
    }

    // ==================== NEW ENHANCEMENTS ====================

    // Get max inversion for this chord
    maxInversion {
        ^this.get.size
    }

    // Get all possible inversion levels (0 to max)
    inversionRange {
        ^(0..this.maxInversion)
    }

    // Get max doubling for this chord
    maxDouble {
        ^this.get.size
    }

    // Get all possible doubling levels (0 to max-1)
    doublingRange {
        ^(0..this.maxDouble - 1)
    }

    // Get inversion level as number
    getInversionLevel {
        var inv;
        if(inversions.isEmpty, { ^0 });
        inv = inversions[0][0];
        if(inv.isNumber, { ^inv });
        ^ChordOps.inversionLevelFromSymbol(inv, this.get.size)
    }

    // Get style as number
    getInversionStyle {
        var style;
        if(inversions.isEmpty, { ^0 });
        style = inversions[0][1];
        if(style.isNumber, { ^style });
        ^switch(style,
            \up, { 0 },
            \down, { 1 },
            \updown, { 2 },
            \downup, { 3 },
            { 0 }
        )
    }

    // Get all inversions as a list of Harmony objects
    getAllInversions { |styles([0])|  // 0=up, 1=down, etc.
        var maxInv = this.maxInversion;
        var inversions = [];
        styles = styles.asArray;
        styles.do{|style|
            (0..maxInv).do{|inv|
                inversions = inversions.add(
                    this.withInversion(inv, style)
                );
            }
        };
        ^inversions
    }

    // Get all doublings as a list of Harmony objects
    getAllDoublings { |octaves([-1, 1])|  // -1=below, 1=above
        var maxDouble = this.maxDouble;
        var doublings = [];
        octaves = octaves.asArray;
        octaves.do{|oct|
            (0..maxDouble-1).do{|doubleIdx|
                doublings = doublings.add(
                    this.withDouble(doubleIdx, oct)
                );
            }
        };
        ^doublings
    }

    // Get a random inversion
    randomInversion { |style=0|
        var maxInv = this.maxInversion;
        var randomInv = rrand(0, maxInv);
        ^this.withInversion(randomInv, style)
    }

    // Get a random doubling
    randomDouble { |octaves([-1, 1])|
        var maxDouble = this.maxDouble;
        var randomDouble = rrand(0, maxDouble - 1);
        var randomOctave = octaves.choose;
        ^this.withDouble(randomDouble, randomOctave)
    }

    // Get all inversions as MIDI notes
    asMidiWithAllInversions { |midiNoteOffset=60, styles([0])|
        var allInversions = this.getAllInversions(styles);
        ^allInversions.collect{|inv|
            inv.asMidiNoteNumbers(midiNoteOffset)
        }
    }

    // Convenience methods for common inversions
    invertFirst { ^this.withInversion(1, \up) }
    invertSecond { ^this.withInversion(2, \up) }
    invertThird { ^this.withInversion(3, \up) }
    invertFourth { ^this.withInversion(4, \up) }
    invertFifth { ^this.withInversion(5, \up) }
    invertSixth { ^this.withInversion(6, \up) }
    invertSeventh { ^this.withInversion(7, \up) }

    // Invert by specific number of notes with style
    invertN { |n=1, style=\up|
        ^this.withInversion(n, style)
    }
}

// Perform chord operations on an array of intervals
// All operations happen in semitone offsets
ChordOps{

    *doublingTypes{
        ^[\root, \second, \third, \fourth, \fifth, \sixth, \seventh, \rootandsecond, \rootandthird, \secondandthird, \all]
    }

    *inversionTypes{
        ^[\first, \second, \third, \fourth, \fifth, \sixth, \seventh, \middle, \top, \edges, \none]
    }

    *prClean{|intervals|
        // Remove duplicates and flatten
        ^intervals.flat.removeDuplicates
    }

    *transpose{|intervals, amount=0|
        ^intervals.collect{|interval| interval + amount}
    }

    // Get max inversion for a chord
    *maxInversion{|intervals|
        ^intervals.size  // Number of notes = max inversion level
    }

    // Get all inversion levels as numbers
    *inversionRange{|intervals|
        ^(0..intervals.size)
    }

    // Get max doubling for a chord
    *maxDouble{|intervals|
        ^intervals.size  // Can double up to number of notes
    }

    // Get all doubling levels as numbers
    *doublingRange{|intervals|
        ^(0..intervals.size - 1)
    }

    // Get inversion level from symbolic name
    *inversionLevelFromSymbol{|symbol, numNotes|
        ^switch(symbol,
            \first, { 1 },
            \second, { 2 },
            \third, { 3 },
            \fourth, { 4 },
            \fifth, { 5 },
            \sixth, { 6 },
            \seventh, { 7 },
            \middle, { (numNotes / 2).floor },
            \top, { numNotes - 1 },
            \edges, { 1 },
            \none, { 0 },
            { 0 }
        )
    }

    // Get style from number
    *styleFromNumber{|num|
        ^switch(num % 4,
            0, { \up },
            1, { \down },
            2, { \updown },
            3, { \downup }
        )
    }

    *double{|intervals, patternType=\root, octaves=1|
        var doubledIntervals = intervals.copy;
        var numNotes = intervals.size;
        var octaveShiftSemitones;

        // Handle numeric input for patternType
        if(patternType.isNumber, {
            // Map number to note index: 0=root, 1=second, 2=third, etc.
            var noteIndex = patternType.clip(0, numNotes - 1);
            var octaveShift = 12 * octaves.sign;

            octaves.abs.do{|i|
                doubledIntervals = doubledIntervals.add(
                    doubledIntervals[noteIndex] + (octaveShift * (i + 1))
                );
            };
            ^this.prClean(doubledIntervals);
        });

        // Also handle numeric octaves as number of octaves to shift
        octaveShiftSemitones = {|x|
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
                    \fourth, {
                        if(numNotes > 3) {
                            doubledIntervals = doubledIntervals.add(doubledIntervals[3] + octaveShiftSemitones.value(i))
                        }
                    },
                    \fifth, {
                        if(numNotes > 4) {
                            doubledIntervals = doubledIntervals.add(doubledIntervals[4] + octaveShiftSemitones.value(i))
                        }
                    },
                    \sixth, {
                        if(numNotes > 5) {
                            doubledIntervals = doubledIntervals.add(doubledIntervals[5] + octaveShiftSemitones.value(i))
                        }
                    },
                    \seventh, {
                        if(numNotes > 6) {
                            doubledIntervals = doubledIntervals.add(doubledIntervals[6] + octaveShiftSemitones.value(i))
                        }
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
                        numNotes.do{|idx|
                            doubledIntervals = doubledIntervals.add(doubledIntervals[idx] + octaveShiftSemitones.value(i));
                        }
                    }
                );
            };

            // Sort and clean and return
            ^this.prClean(doubledIntervals)
        });
    }

    *invert{|intervals, patternType=\first, style=\up|
        var invertedIntervals = intervals.copy;
        var numNotes = intervals.size;
        var inversionLevel;

        // Handle numeric input for style
        if(style.isNumber, {
            style = this.styleFromNumber(style);
        });

        // Handle numeric input for patternType directly
        if(patternType.isNumber, {
            // Clamp to valid range
            inversionLevel = patternType.clip(0, numNotes);
            if(inversionLevel == 0, {
                ^this.prClean(invertedIntervals);  // No inversion
            });

            // Move first N notes up one octave
            inversionLevel.do{|idx|
                style.switch(
                    \up, { invertedIntervals[idx] = invertedIntervals[idx] + 12 },
                    \down, { invertedIntervals[idx] = invertedIntervals[idx] - 12 },
                    \updown, {
                        if(idx % 2 == 0) {
                            invertedIntervals[idx] = invertedIntervals[idx] + 12
                        } {
                            invertedIntervals[idx] = invertedIntervals[idx] - 12
                        }
                    },
                    \downup, {
                        if(idx % 2 == 0) {
                            invertedIntervals[idx] = invertedIntervals[idx] - 12
                        } {
                            invertedIntervals[idx] = invertedIntervals[idx] + 12
                        }
                    }
                )
            };
            ^this.prClean(invertedIntervals);
        });

        // Handle symbolic names
        inversionLevel = this.inversionLevelFromSymbol(patternType, numNotes);

        // Check if pattern type is valid
        if(this.inversionTypes.includes(patternType).not, {
            "Invalid pattern type: %".format(patternType).error;
            ^nil
        });

        // For \middle, \top, \edges we need special handling
        if(#[\middle, \top, \edges].includes(patternType), {
            var indicesToMove = patternType.switch(
                \middle, {
                    if(numNotes % 2 == 0, {
                        [numNotes/2 - 1, numNotes/2]
                    }, {
                        [(numNotes/2).floor]
                    })
                },
                \top, { [numNotes - 1] },
                \edges, { [0, numNotes - 1] }
            );

            indicesToMove.do{|idx|
                style.switch(
                    \up, { invertedIntervals[idx] = invertedIntervals[idx] + 12 },
                    \down, { invertedIntervals[idx] = invertedIntervals[idx] - 12 },
                    \updown, {
                        if(idx % 2 == 0) {
                            invertedIntervals[idx] = invertedIntervals[idx] + 12
                        } {
                            invertedIntervals[idx] = invertedIntervals[idx] - 12
                        }
                    },
                    \downup, {
                        if(idx % 2 == 0) {
                            invertedIntervals[idx] = invertedIntervals[idx] - 12
                        } {
                            invertedIntervals[idx] = invertedIntervals[idx] + 12
                        }
                    }
                )
            };

            ^this.prClean(invertedIntervals);
        });

        // Standard inversion: move first N notes up one octave
        if(inversionLevel > 0 and: { inversionLevel <= numNotes }, {
            inversionLevel.do{|idx|
                style.switch(
                    \up, { invertedIntervals[idx] = invertedIntervals[idx] + 12 },
                    \down, { invertedIntervals[idx] = invertedIntervals[idx] - 12 },
                    \updown, {
                        if(idx % 2 == 0) {
                            invertedIntervals[idx] = invertedIntervals[idx] + 12
                        } {
                            invertedIntervals[idx] = invertedIntervals[idx] - 12
                        }
                    },
                    \downup, {
                        if(idx % 2 == 0) {
                            invertedIntervals[idx] = invertedIntervals[idx] - 12
                        } {
                            invertedIntervals[idx] = invertedIntervals[idx] + 12
                        }
                    }
                )
            };
        }, {
            if(inversionLevel > numNotes, {
                "Inversion level % exceeds number of notes (%). Clamping to %".format(patternType, numNotes, numNotes).warn;
                // Clamp to maximum
                numNotes.do{|idx|
                    style.switch(
                        \up, { invertedIntervals[idx] = invertedIntervals[idx] + 12 },
                        \down, { invertedIntervals[idx] = invertedIntervals[idx] - 12 },
                        \updown, {
                            if(idx % 2 == 0) {
                                invertedIntervals[idx] = invertedIntervals[idx] + 12
                            } {
                                invertedIntervals[idx] = invertedIntervals[idx] - 12
                            }
                        },
                        \downup, {
                            if(idx % 2 == 0) {
                                invertedIntervals[idx] = invertedIntervals[idx] - 12
                            } {
                                invertedIntervals[idx] = invertedIntervals[idx] + 12
                            }
                        }
                    )
                }
            });
        });

        ^this.prClean(invertedIntervals);
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

// Turn Harmonys into intervals in a pattern
/*

Example:

p = PIntervals(\major7, \dominant7, Harmony.randomTriad, Harmony([0,5, 10])).asStream
p.next

*/
PIntervals : Pattern {
    var <>harmonies;

    *new { |...harmonies|
        harmonies = harmonies.collect{|harm| harm.asHarmony };
        ^super.new.harmonies_(harmonies)
    }

    embedInStream { |inval|
        harmonies.do{|harm|
            var harmonyArray = harm.get();

            if (harmonyArray.isNil) {
                Error("Harmony key '%' not found in Harmony.all".format(harm)).throw;
            };

            inval = harmonyArray.embedInStream(inval);

        }

        ^inval;
    }
}

// Turn Harmonys into intervals in a pattern by turning
PHarmony : Pattern {
    var <>harmonies;
    var <>arpStyles;
    var <>midiNoteOffset;

    *new { |harmoniesArray, arpStyles=\chords, midiNoteOffset=0|
        ^super.new.harmonies_(harmoniesArray)
            .arpStyles_(arpStyles)
            .midiNoteOffset_(midiNoteOffset)
    }

    *arpStyles{
        ^[
            \chords, // Play intervals as chords.
            \up, // bottom to top
            \down, // top to bottom
            \updown, // bottom to top and back
            \downup, // top to bottom and back
            \random, // random order
            \upin, // lowest to highest, repeat inward
            \inup, // highest to lowest, repeat inward
            \downin, // highest to lowest, repeat inward
            \blossomup, // from center, spiral up
            \blossomdown, // from center, spiral down
        ]
    }

    embedInStream {|inval|
        var combineWith, combined;
        var harmoniesLargest = (harmonies.asArray.size > arpStyles.asArray.size);

        if(this.harmonies.class != Array, {
            this.harmonies =  this.harmonies.asArray;
        });

        if(this.arpStyles.class != Array, {
            this.arpStyles =  this.arpStyles.asArray;
        });

        if(harmoniesLargest, {
                combined = this.harmonies.asArray.collect{|harm, index| [harm, this.arpStyles.wrapAt(index)] };
        }, {
                combined = this.arpStyles.asArray.collect{|arpStyle, index| [this.harmonies.wrapAt(index), arpStyle] };
        });


        combined.do{|harmPair|
            var harm = harmPair.first.asHarmony;
            var arpStyle = harmPair[1];
            var harmonyArray = harm.get();
            var arp;

            if (harmonyArray.isNil) {
                Error("Harmony key '%' not found in Harmony.all".format(harm)).throw;
            };

            arp = this.class.prArpeggiateHarmony(arpStyle, harm) + midiNoteOffset;
            inval = arp.embedInStream(inval);
        }

        ^inval;

    }

    *prArpeggiateHarmony{|arpStyle, harmony|

        // Check if the pattern type is valid
        if(this.arpStyles.includes(arpStyle).not, {
            "Invalid pattern type: %".format(arpStyle).error;
            ^nil
        });

        ^switch(
            arpStyle,
                \chords, {
                    harmony.get()
                },
                \up, {
                    harmony.asPseq(repeats: 1)
                },
                \down, {
                    var listSeq = harmony.get().reverse;
                    Pseq(listSeq, repeats: 1)
                },
                \updown, {
                    var up = harmony.get();
                    var down = up.reverse;
                    Pseq((up++down), repeats: 1)
                },
                \downup, {
                    var up = harmony.get();
                    var down = up.reverse;
                    Pseq((down++up), repeats: 1)
                },

                \random, {
                    harmony.asPxrand(repeats: harmony.get().size);
                },
                \upin, {
                // Repeatedly get the lowest, then the highest, and repeat the process inward until out of values. Inspired by Bitwig's "Up+In"
                    var listSeq = harmony.get().upIn;

                    Pseq(listSeq, repeats: 1)

                },
                \inup,{
                var listSeq = harmony.get().inUp;
                    Pseq(listSeq, repeats: 1)
                },
                \blossomup, {
                var listSeq = harmony.get().blossomUp();
                    Pseq(listSeq, repeats: 1)
                },
                \blossomdown, {
                var listSeq = harmony.get().blossomDown();
                    Pseq(listSeq, repeats: 1)
                }
          )
    }
}

// Turn a string of roman numerals into a sequential chord progression
/*
(
p = PChordProgression("i IV ii", 48);
z = p.asStream;

z.next;
z.next
z.next
)

// Pbind example
(
s.waitForBoot{
    Pbind(
        \midinote, PChordProgression(
            chords: "i IV ii",
            midiNoteOffset: 48,
            arpStyle: \chords,
            repeats: inf
        ),
        \dur, 0.25,
    ).play
}
)

*/
PChordProgression : Pattern {
    var <>romanChords;
    var <>midiNoteOffset;
    var <>repeats;
    var <>arpStyle;

    *new { |chords, midiNoteOffset=48, arpStyle=\chords, repeats=inf|
        ^super.new.romanChords_(chords.romanChords)
            .midiNoteOffset_(midiNoteOffset)
            .repeats_(repeats)
            .arpStyle_(arpStyle)
    }

    embedInStream {|inval|
        var romanChords = this.romanChords;

        repeats.do{
            romanChords.do{|chord|
                var harmony = chord[0];
                var pitchOffset = chord[1];
                var harmonyPattern = harmony.asPHarmony(arpStyle, midiNoteOffset: pitchOffset + midiNoteOffset);

                inval = harmonyPattern.embedInStream(inval);
            }
        }

        ^inval;
    }
}

// Representation of roman numerals. Basically just a global dictionary where the key is a roman numeral, and the value is an Event with the Harmony and the pitch offset according to it's number
RomanChords{
    classvar <all;

    *initClass{

        // Make sure it inializes after the Harmony class
        Class.initClassTree(Harmony);


        all = [
            // Triads
            \I -> [Harmony.new(\major), 0],
            \i -> [Harmony.new(\minor), 0],

            \II -> [Harmony.new(\major), 2],
            \ii -> [Harmony.new(\minor), 2],

            \III -> [Harmony.new(\major), 4],
            \iii -> [Harmony.new(\minor), 4],

            \IV -> [Harmony.new(\major), 5],
            \iv -> [Harmony.new(\minor), 5],

            \V -> [Harmony.new(\major), 7],
            \v -> [Harmony.new(\minor), 7],

            \VI -> [Harmony.new(\major), 9],
            \vi -> [Harmony.new(\minor), 9],

            \VII -> [Harmony.new(\major), 11],
            \vii -> [Harmony.new(\diminished), 11],

            // Sevenths
            \I7 -> [Harmony.new(\major7), 0],
            \i7 -> [Harmony.new(\minor7), 0],
            \II7 -> [Harmony.new(\major7), 2],
            \ii7 -> [Harmony.new(\minor7), 2],
            \III7 -> [Harmony.new(\major7), 4],
            \iii7 -> [Harmony.new(\minor7), 4],
            \IV7 -> [Harmony.new(\major7), 5],
            \iv7 -> [Harmony.new(\minor7), 5],
            \V7 -> [Harmony.new(\dominant7), 7],
            \v7 -> [Harmony.new(\minor7), 7],

        ].asDict;
    }

    *at{|name|
        ^this.all[name]
    }
}

// Parse a string as a roman numeral
+Symbol{
    parseRomanChord{
        var harmony, pitchOffset;
        var romanChord = RomanChords.at(this);

        if(romanChord.isNil, {
            "Invalid roman numeral: %".format(this).error;
            ^nil
        });

        harmony = romanChord[0];
        pitchOffset = romanChord[1];

        ^[harmony, pitchOffset];
    }
}

+String{
    parseRomanChord{
        ^this.asSymbol.parseRomanChord;
    }

    romanChords{
        var tokens = this.split(Char.space);
        var romanChords = [];

        tokens.do{|token|
            var romanChord = token.parseRomanChord;

            if(romanChord.isNil, {
                "Invalid roman numeral: %".format(token).error;
                ^nil
            });

            romanChords = romanChords.add(romanChord);
        };

        ^romanChords;
    }

    romanChordsMidiNotes{|withMidiNoteOffset=0|
        var romanChords = this.romanChords;

        ^romanChords.collect{|chord|
            var harmony = chord[0];
            var pitchOffset = chord[1];

            var midiNotes = harmony.asMidiNoteNumbers(pitchOffset);

            midiNotes.collect{|note| note + pitchOffset} + withMidiNoteOffset;
        };
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

    asHarmony{
        ^Harmony.new(this)
    }

    // Lowest to highest
    upIn{
        var arr = this;
        var firstSize = arr.size;
        var outArray = Array.new();

        while({arr.size > 0}) {
                var low = arr.minItem;
                var high;

                if(low.notNil, {
                    outArray = outArray.add(low);
                    arr = arr.reject({|item, i| (item === low) });
                });

                high = arr.maxItem;
                if(high.notNil, {
                    outArray = outArray.add(high);
                    arr = arr.reject({|item, i| (item === high) });
                });

        };


        ^outArray
    }

    // Higest to lowest
    inUp{
        var arr = this;
        var firstSize = arr.size;
        var outArray = Array.new();

        while({arr.size > 0}) {
            var low = arr.maxItem;
            var high;

            if(low.notNil, {
                    outArray = outArray.add(low);
                    arr = arr.reject({|item, i| (item === low) });
                    });

            high = arr.minItem;
            if(high.notNil, {
                    outArray = outArray.add(high);
                    arr = arr.reject({|item, i| (item === high) });
                    });

        };

        ^outArray
    }

    // Spiral up from the center
    blossomUp{
        var array = this;
        var sorted = array.sort;
        var middleIndex = (sorted.size / 2).floor;
        var result = [sorted[middleIndex]];
        var left = middleIndex - 1;
        var right = middleIndex + 1;

        while { left >= 0 or: { right < sorted.size } } {
            if (right < sorted.size) { result = result.add(sorted[right]) };
            if (left >= 0) { result = result.add(sorted[left]) };
            left = left - 1;
            right = right + 1;
        };

        ^result;
    }

    // Same as above but other direction
    blossomDown{
        var array = this;
        var sorted = array.sort;
        var middleIndex = (sorted.size / 2).floor;
        var result = [sorted[middleIndex]];
        var left = middleIndex - 1;
        var right = middleIndex + 1;

        while { left >= 0 or: { right < sorted.size } } {
            if (left >= 0) { result = result.add(sorted[left]) };
            if (right < sorted.size) { result = result.add(sorted[right]) };
            left = left - 1;
            right = right + 1;
        };

        ^result;
    }


    // FIXME:This should be used to create arpeggiator patterns
    // Arpeggiate array and return arpeggiated array
    arpeggiate{|arpStyle=\chords|

        ^switch(
            arpStyle,
            \chords, { ^this },
            \up, { this},
            \down, { this.reverse},
            \updown, {
                // Creates a pyramid
                this ++ this.reverse
            },
            \downup, {
                this.reverse ++ this.reverse
            },
            \updownPyramid, {
                // Creates a pyramid
                this ++ this[..this.size-2].reverse
            },
            \downupPyramid, {
                this.reverse ++ this[..this.size-2].reverse
            },
            \random, { this},
            \upin, { this.upIn},
            \inup, { this.inUp},
            \blossomup, { this.blossomUp},
            \blossomdown, { this.blossomDown}
        );
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

    asPtuple{|repeats=inf|
        ^Ptuple.new(this.get, repeats)
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
// Arpeggiate a harmony
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

    asPtuple{|repeats=inf|
        ^Ptuple.new(this.get, repeats)
    }
}
