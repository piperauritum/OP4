// Launch the OP4Editor by evaluating the following, then select a timbre.
// The editor defines the synth \op4test.
(
(thisProcess.nowExecutingPath.dirname +/+ "timbre.scd").load;		// Load Presets (~xTimbre)
~ope = OP4Editor.new;
)


// Test a single note.
Synth(\op4test, [\dur, 0.2, \pch, 12.rand+60, \dyn, 1, \amp, 0.2]);


// Test random notes.
(
x = Pbindef(\test,
	\instrument, \op4test,
	\dur, Pwhite(0.1, 0.8),
	\pch, Pwhite(36*2, 84*2)/2,
	\dyn, 1,
	\amp, 0.3,
	\pan, Pwhite(-0.5, 0.5)
).play;
)

x.stop;


// Get the program and parameters of the edited timbre.
~ope.prog;
~ope.param;


// Define a synth by using timbre data.
(
~prog = [ 4.0, 0.0, 15.0, 2.0, 8.061, 0.0, 0.0, 0.0, 0.0 ];
~param = [ [ 0.082, 0.151, 2.382, 0.0, 17.777, 3.489, 1.0, 3.0, -2.0, 0.0, 0.0, 0.0 ], [ 1.0, 0.039, 6.507, 0.0, 67.891, 3.489, 2.0, 1.0, 0.0, 0.0, 1.0, 0.0 ], [ 0.049, 0.108, 4.655, 0.0, 12.717, 7.062, 2.0, 1.0, -2.0, 0.0, 0.0, 0.0 ], [ 0.546, 0.077, 4.655, 0.0, 67.891, 3.489, 2.0, 4.0, 2.0, 0.0, 1.0, 0.0 ] ];
OP4.sdef(\op4test, ~prog, ~param);
)

// Launch the OP4Editor with the existing timbre data.
~ope = OP4Editor.new(~prog, ~param);


