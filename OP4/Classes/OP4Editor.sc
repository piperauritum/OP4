OP4Editor {
	var <>prog, <>param, op_mask,
	wd, algoList, timbList, knobs, lfoList, lfoKnobs, fbKnob, progNbx, maskBtn, envView,
	ampSp, timeSp, susSp, sca, ovt, fine, fbSp, wavSp, lfrqSp,
	spec, op_label, pg_label,
	dicProg;

	*new { | prog, param |
		^super.newCopyArgs(prog, param).init;
	}

	init {
		wd = Window(\OP4Editor, Rect(100, 100, 720, 360));
		wd.alwaysOnTop = true;
		wd.front;
		// wd.onClose = { CmdPeriod.run };

		ampSp = [0, 1, \lin, 0.01].asSpec;
		timeSp = [0.01, 10, \exp, 0.01].asSpec;
		susSp = [0.01, 20, \exp, 0.01].asSpec;
		sca = [0, 3, \lin, 1].asSpec;
		ovt = [1, 16, \lin, 1].asSpec;
		fine = [-2, 2, \lin, 0.5].asSpec;
		fbSp = [0, 12, \lin, 0.1].asSpec;
		wavSp = [0, 7, \lin, 1].asSpec;
		lfrqSp = [0, 60, \lin, 0.1].asSpec;

		spec = [ampSp, timeSp, timeSp, ampSp, susSp, timeSp, sca, ovt, fine, sca, ampSp, wavSp];
		op_label = OP4.op_label;
		pg_label = OP4.pg_label;

		if ( prog.isNil ) { prog = [0, 0, 15, 0, 0, 0, 0, 0, 0] };
		if ( param.isNil ) { param = Array.fill(4, { [0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0] }) };

		dicProg = [pg_label, prog].flop.flatten(1);
		dicProg = Dictionary.newFrom(dicProg);

		op_mask = prog[2].asInteger.asBinaryDigits(4).reverse;

		this.panelAlgo;
		this.panelTimb;
		this.panelProg;
		this.panelLFO;
		this.panelEnv;
		this.panelKnobs;
		this.dispParam;
	}

	dispParam {
		var dx;
		dx = param.flop;
		dx = [op_label, dx].flop.flatten(1);
		dx = Dictionary.newFrom(dx);

		param.do{|p,m|
			p.do{|e,n|
				var sp = spec[n];
				knobs[m][n][0].value = e;
				knobs[m][n][1].value = sp.unmap(e);
		}};

		algoList.value_(dicProg[\algo]);
		lfoList.value_(dicProg[\lfowav]);
		op_mask.do{|e,i| maskBtn[i].value = e };

		3.do{|n|
			var pg = dicProg[[\fb, \ams, \pms][n]];
			progNbx[n].value = pg;
		};

		3.do{|n|
			var pg = dicProg[[\lfofrq, \amd, \pmd][n]];
			lfoKnobs[n][0].value = pg;
			lfoKnobs[n][1].value = [lfrqSp, ampSp, ampSp][n].unmap(pg);
		};

		4.do{|i|
			var ev = Env(
				levels: [0, 1, dx[\d1lev][i], 0, 0],
				times: [dx[\atk][i], dx[\d1t][i], dx[\d2t][i], dx[\rel][i]],
				curve: [\sin, \cub, \cub, \cub],
				releaseNode: 3
			);
			envView[i].setEnv(ev);
		};

		this.sdef;
	}


	conv {|n|
		var timb, ary, pg;
		var atk2ms, dec2ms, rel2ms;

		timb = ~xTimbre[n];
		ary = Array2D.fromArray(5, 11, timb);
		pg = ary.rowAt(0);

		atk2ms = {|rate|
			12208 * exp(-0.338 * rate) / 1000;
		};

		dec2ms = {|rate|
			67891 * exp(-0.335 * rate) / 1000;
		};

		rel2ms = {|rate|
			58539 * exp(-0.705 * rate) / 1000;
		};

		param = (1..4).collect{|i|
			var e = ary.rowAt(i);
			[
				(-3/4).dbamp ** e[5],						// Total Level
				atk2ms.(e[0]),								// Attack Rate
				dec2ms.(e[1]),								// Decay1 Rate
				if(e[4] < 15, {(-3.dbamp) ** e[4]}, {0}),	// Decay1 Level
				dec2ms.(e[2]),								// Decay2 Rate
				rel2ms.(e[3]),								// Release Rate
				e[6],										// Keyboard Scaling
				[0.5, e[7]].maxItem,						// Overtone (Multiplier)
				[0, 1, 1.5, 2, 0, -1, -1.5, -2][e[8]],		// Fine (Detune 1)
				e[9],										// Irrational ratio (Detune 2)
				e[10],										// Amplitude Modulation Enable
				0,											// Wavefrom (sin)
			].collect{|e| e.round(0.001) };
		};

		prog = [
			pg[0] % 8,								// Algorithm
			pg[0] >> 3 / 7 * 12, 					// Feedback Level
			pg[1],									// Operator Mask
			pg[2],									// LFO Waveform
			63.5 * (-0.043 * (256 - pg[4])).exp,	// LFO Frequency
			pg[5] / 127,							// Pitch Modulation Depth
			pg[6] / 127,							// Amplitude Modulation Depth
			pg[7],									// Pitch Modulation Sensitivity
			pg[8],									// Amplitude Modulation Sensitivity
		];

		prog = prog.collect{|e| e.round(0.001) };
		dicProg = [pg_label, prog].flop.flatten(1);
		dicProg = Dictionary.newFrom(dicProg);

		this.dispParam;
	}


	sdef {
		prog = pg_label.collect{|e| dicProg[e]};
		OP4.sdef(\op4test, prog, param);
	}


	// ==== GUI ====

	panelAlgo {
		StaticText(wd, Rect(4, 0, 100, 20))
		.string_("Algorithm")
		.font_(Font.sansSerif(12).boldVariant);

		algoList = ListView(wd, Rect(4, 20, 100, 125))
		.items_([
			"0 -> 1 -> 2 -> 3",
			"[0, 1] -> 2 -> 3",
			"[0, [1 -> 2]] -> 3",
			"[[0 -> 1], 2] -> 3",
			"[0 -> 1], [2 -> 3]",
			"0 -> [1, 2, 3]",
			"[0 -> 1], 2, 3",
			"0, 1, 2, 3",
		])
		.action_({|i|
			dicProg[\algo] = i.value;
			this.sdef;
		});
	}


	panelTimb {
		StaticText(wd, Rect(120, 0, 100, 20))
		.string_("Timbre")
		.font_(Font.sansSerif(12).boldVariant);

		timbList = PopUpMenu(wd, Rect(120, 20, 100, 20))
		.items_(~xTimbre.flop[~xTimbre[0].size-2])
		.action_({|i|
			this.conv(i.value);
		});
	}


	panelProg {
		var cv = CompositeView(wd, Rect(120, 50, 100, 155));

		progNbx = Array.fill(3, {|n|
			var nb;

			nb = NumberBox(cv, Rect(30, 20*n+40, 35, 15))
			.action_({|v|
				dicProg[[\fb, \ams, \pms][n]] = v.value;
				this.sdef;
			});

			StaticText(cv, Rect(0, 20*n+40, 30, 15))
			.string_(["FB", "AMS", "PMS"][n])
			.font_(Font.sansSerif(12).boldVariant);

			nb;
		});

		maskBtn = Array.fill(4, {|n|
			var bt;

			bt = Button(cv, Rect(15*n, 20, 15, 15))
			.states_([
				["", Color.clear, Color.white],
				["", Color.clear, Color.blue]
			])
			.action_({|v|
				op_mask[n] = v.value;
				dicProg[\mask] = op_mask.collect{|e,i| 2 ** i * e}.inject(0, _+_).asInteger;
				this.sdef;
			});

			bt;
		});

		StaticText(cv, Rect(0, 0, 60, 20))
		.string_("OPmask")
		.font_(Font.sansSerif(12).boldVariant);
	}


	panelLFO {
		var cv = CompositeView(wd, Rect(4, 160, 170, 100));

		StaticText(cv, Rect(0, 0, 100, 20))
		.string_("LFO")
		.font_(Font.sansSerif(12).boldVariant);

		lfoList = ListView(cv, Rect(0, 20, 40, 70))
		.items_([
			"Saw",
			"Pulse",
			"Tri",
			"Noise",
		])
		.action_({|i|
			dicProg[\lfowav] = i.value;
			this.sdef;
		});

		lfoKnobs = Array.fill(3, {|n|
			var kn, nb;
			var x = 40*n+50;

			nb = NumberBox(cv, Rect(x, 40, 35, 15))
			.action_({|v|
				var va = v.value;
				dicProg[[\lfofrq, \amd, \pmd][n]] = va;
				kn.value = [lfrqSp, ampSp, ampSp][n].unmap(v.value);
				this.sdef;
			});

			kn = Knob(cv, Rect(x, 55, 35, 35))
			.action_({|v|
				var va = [lfrqSp, ampSp, ampSp][n].map(v.value);
				dicProg[[\lfofrq, \amd, \pmd][n]] = va;
				nb.value = va;
				this.sdef;
			});

			StaticText(cv, Rect(x, 20, 40, 20))
			.string_(["Freq", "AMD", "PMD"][n])
			.font_(Font.sansSerif(12).boldVariant);

			[nb, kn];
		});
	}


	panelEnv {
		var cv = CompositeView(wd, Rect(0, 255, 640, 360));

		envView = Array.fill(4, {|n|
			EnvelopeView(cv, Rect(160*n, 0, 160, 100)).editable_(false);
		});
	}


	panelKnobs {
		knobs = Array.fill(4, {|m|
			var cv = CompositeView(wd, Rect(220, 60*m+20, 500, 48));

			StaticText(cv, Rect(4, 22, 48, 20))
			.string_(m)
			.font_(Font.sansSerif(18).boldVariant);

			Array.fill(12, {|n|
				var sp = spec[n];
				var lb, kn, nb;

				nb = NumberBox(cv, Rect(40*n+20, 0, 35, 15))
				.action_({|v|
					var va = v.value;
					param[m][n] = va;
					kn.value = sp.unmap(v.value);
					this.sdef;
				});

				kn = Knob(cv, Rect(40*n+20, 15, 35, 35))
				.action_({|v|
					var va = sp.map(v.value);
					param[m][n] = va;
					nb.value = va;
					this.sdef;
				});

				if (n == 8) {
					kn.centered = true;
					kn.value = fine.unmap(0);
				};

				[nb, kn];
			});
		});

		op_label.do{|e, i|
			var a = StaticText(wd, Rect(40*i+240, 0, 40, 20))
			.string_(e.firstToUpper)
			.font_(Font.sansSerif(12).boldVariant);
			if (i < 6) {
				a.stringColor = Color.white;
				a.background = Color.black;
			};
		};
	}
}