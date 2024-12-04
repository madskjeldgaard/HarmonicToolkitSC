# Harmonic Toolkit for SuperCollider

This SuperCollider package makes it simple and practical to work with harmony theory without having to know harmony theory.

It can be used to easily turn melodies into chords/harmonies in a pattern or to harmonize incoming midi notes and turn whatever you play on your midi keyboard into chords. 

You can also go the other way by turning a harmony/chord into an arpeggio.

A harmony in the context of this toolkit is simply an array of semi tone offsets (or midinotes). These can then be transformed using conventional harmonic operations like doubling (adding octaves above or below) or inversions of the root notes of the chords. 

## Installation

Open up SuperCollider and evaluate the following line of code:
`Quarks.install("https://github.com/madskjeldgaard/harmonics")`

## Examples

### Simple

A simple example of how to create a harmony and perform operations on it using inversions and doublings.

```supercollider
(
// Can be initialized with a chord name
var h = Harmony.new(\major7);

// Or a list of semitone offsets
// var h = Harmony.new([0, 5, 9]);

// See the semitones of the chord
"Before operations: %".format(h.value).postln;

// Perform inversion
h.addInversion(patternType: \first, style: \up);
"After inversion operations: %".format(h.value).postln;

// Clear the inversion if you fancy
// h.clearInversion();

// Perform doubling
h.addDouble(patternType: \root,  octaves: -2);
"After doubling operations: %".format(h.value).postln;
)
```

### Patterns

All of these pattern examples use the `\mtranspose` key in Pbind to transpose each midi note using one of the harmonies chosen. This is neat because then you can write a melody in either `\degree` or `\midinote` and keep that melody seperate from your harmony.

#### Pbind: Harmonize a random melody

```supercollider
(
var chordName = 'major7';

// Harmonize a random melody with major 7 chords
Pdef(\simpleHarmony,
    Pbind(
        \degree, Pwhite(0,5),
        \mtranspose, chordName.asHarmony,
        \dur, 0.125,
    )
).play;
)
```

#### Pbind: Harmonize melody with random chord types

```supercollider
(
// Random chord on each note
var chords = Prand([\major7, \major9, \dominant7, \augmented, \halfDiminished7], inf);

// Slowly change the melody 
var melody = Pstep([0,-5,1,3], 16, inf);

Pdef(\risingChords,
    Pbind(
        // Slowly ascend the scale
        \degree, melody.trace(prefix: "degree: "),

        // Harmonize it
        \mtranspose, chords.collect{|chordName| chordName.asHarmony.get() }.trace(prefix: "chord: "),

        \dur, 0.125,
    )
).play;

)
```

#### MIDI: Harmonize incoming midi notes

TODO

#### Advanced pattern example: Arpeggio with harmonized chords

```supercollider
/*

An advanced example.

This takes a melody and on each note plays an arpeggio of a major 7 chord.

*/
(
// Choose a chord
var h = \major7.asHarmony;

// Play some inversions of the chord
Pdef(\inversions,
    Pbind(
        \octave, 3,

        // The melody to be harmonized
        \degree, Pdup(64, Pseq([0,3,5], inf)),

        // The harmonization of the melody flattened as arpeggeios
        \mtranspose, Pseq([
            h.asPseq(4),
            h.withInversion(\first, \up).asPseq(4),
            h.withInversion(\second, \up).asPseq(4),
            h.withInversion(\edges, \up).asPseq(4),
            h.withInversion(\third, \up).asPseq(4),
            // h.withInversion(\second, \down).asPseq(4),
        ], inf),

        \dur, 0.125,

        \legato, 0.5,
    )
).play;
)


// Now harmonize the arpeggio above
// This takes the pattern above and plays harmonies on top of it
Pdef(\inversionsOvertones, Pbindf(Pdef(\inversions), \octave, Pkey(\octave)+1, \mtranspose, Pkey(\mtranspose) + \major7.asHarmony.get() )).play;
```
