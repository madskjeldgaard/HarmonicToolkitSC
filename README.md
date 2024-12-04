# Harmonics

### Harmonic operations

Harmonic operations

### Installation

Open up SuperCollider and evaluate the following line of code:
`Quarks.install("https://github.com/madskjeldgaard/harmonics")`

## Examples


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

#### Harmonize incoming midi notes

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
