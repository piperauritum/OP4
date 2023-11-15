OP4 : OPWaveform {
	classvar op_label = #[\lev, \atk, \d1t, \d1lev, \d2t, \rel, \sca, \ovt, \fine, \irr, \ame, \wav];
	classvar pg_label = #[\algo, \fb, \mask, \lfowav, \lfofrq, \pmd, \amd, \pms, \ams];

	var <>defname, <>prog, <>param, <>outbus, <>fxbus, <>fxbal, dicParam, dicProg, op_mask;

	*new {|defname='op4test', prog, param, outbus=0, fxbus, fxbal=0|
		^super.newCopyArgs(defname, prog, param, outbus, fxbus, fxbal).init;
	}

	init {
		if ( prog.isNil ) { prog = [0, 0, 15, 0, 0, 0, 0, 0, 0] };
		if ( param.isNil ) { param = Array.fill(4, { [0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0] }) };

		dicParam = param.flop;
		dicParam = [op_label, dicParam].flop.flatten(1);
		dicParam = Dictionary.newFrom(dicParam);

		dicProg = [pg_label, prog].flop.flatten(1);
		dicProg = Dictionary.newFrom(dicProg);

		op_mask = prog[2].asInteger.asBinaryDigits(4).reverse;

		this.sdef;
	}

	ar {|freqs, envs, pch, levels, dur|
		var gate, buf, egs, algM, mdx, op0, op1, op2, op3, lfoamp, lfopch;

		gate = EnvGen.kr(Env.linen(0, dur, 0));

		buf = dicParam[\wav].collect{|e| super.waveBufnum[e] };

		freqs = freqs * super.pmo(dicProg[\lfowav].asInteger, dicProg[\pms].asInteger, dicProg[\lfofrq], dicProg[\pmd]);

		algM = this.algoMatrix(dicProg[\algo]);
		mdx = algM[0..2] * 10;

		lfoamp = super.amo(dicProg[\lfowav].asInteger, dicProg[\ams].asInteger, dicProg[\lfofrq], dicProg[\amd]);
		egs = envs.collect{|e,i|
			var scale = Index.kr(super.keyScaleBufnum[dicParam[\sca][i]], pch);
			var env = EnvGen.kr(e, gate, timeScale: scale) * levels[i];
			LinSelectX.kr(dicParam[\ame][i], [env, env * lfoamp]);
		};

		op0 = this.fbop(buf[0], freqs[0], dicProg[\fb], LocalIn.ar * dicProg[\fb], egs[0]);
		op1 = Osc.ar(buf[1], freqs[1], op0 * mdx[0], egs[1]);
		op2 = Osc.ar(buf[2], freqs[2], Mix([op0, op1] * mdx[1]), egs[2]);
		op3 = Osc.ar(buf[3], freqs[3], Mix([op0, op1, op2] * mdx[2]), egs[3]);
		LocalOut.ar(op0);

		^Mix([op0, op1, op2, op3] * algM[3]) / algM[3].sum;
	}

	algoMatrix {|algN = 0|
		var alg = [
			//	op1,	op2,	op3,		out
			//	[0,		[0, 1],	[0, 1, 2],	[0, 1, 2, 3]] (input)
			[1, 	[0, 1],	[0, 0, 1],	[0, 0, 0, 1]],
			[0, 	[1, 1],	[0, 0, 1],	[0, 0, 0, 1]],
			[0, 	[0, 1],	[1, 0, 1],	[0, 0, 0, 1]],
			[1, 	[0, 0],	[0, 1, 1],	[0, 0, 0, 1]],
			[1, 	[0, 0],	[0, 0, 1],	[0, 1, 0, 1]],
			[1, 	[1, 0],	[1, 0, 0],	[0, 1, 1, 1]],
			[1, 	[0, 0],	[0, 0, 0],	[0, 1, 1, 1]],
			[0, 	[0, 0],	[0, 0, 0],	[1, 1, 1, 1]],
		];

		^alg[algN];
	}

	fbop {|bufnum, freq, feedback, mod, eg|
		if ( bufnum == 0 )
		{ ^SinOscFB.ar(freq, feedback * eg, eg) }
		{ ^Osc.ar(bufnum, freq, mod, eg) };
	}

	sdef {
		SynthDef(defname, {|dur, pch, dyn=1, amp=0.1, pan=0|
			var env, frq, sig, gte, msk, dne;
			env = Array.fill(4, {|i|
				Env(
					levels: [0, 1, dicParam[\d1lev][i], 0, 0],
					times: [dicParam[\atk][i], dicParam[\d1t][i], dicParam[\d2t][i], dicParam[\rel][i]],
					curve: [\sin, \cub, \cub, \cub],
					releaseNode: 3
				);
			});

			frq = Array.fill(4, {|i|
				var frq = (dicParam[\fine][i] / 100 + pch).midicps;
				var ovt = dicParam[\ovt][i];
				var irr = [1, 2**0.5, 2.5**0.5, 3**0.5][dicParam[\irr][i]];
				frq * ovt * irr;
			});

			sig = this.ar(
				freqs: frq,
				envs: env,
				pch: pch,
				levels:  Array.fill(4, {|i|
					dicParam[\lev][i] * dyn * op_mask[i];
				}),
				dur: dur,
			);

			sig = Pan2.ar(sig * amp, pan);

			// Free itself when the sound is gone
			gte = env.collect{|e,i|
				var scale = Index.kr(super.keyScaleBufnum[dicParam[\sca][i]], pch);
				var gate = EnvGen.kr(Env.linen(0, dur, 0));
				EnvGen.kr(e, gate, timeScale: scale);
			};
			msk = this.algoMatrix(dicProg[\algo])[3];
			dne = msk.collect{|e,i| if (e == 1, { Done.kr(gte[i]) }, { 0 }) };
			FreeSelf.kr(InRange.kr(sum(dne), msk.sum, msk.sum));

			if ( (outbus.notNil) && (fxbal < 1) ) {
				OffsetOut.ar(outbus, sig * cos(0.5pi * fxbal))
			};

			if ( (fxbus.notNil) && (fxbal > 0) ) {
				OffsetOut.ar(fxbus, sig * sin(0.5pi * fxbal))
			};
		}).add;
	}
}